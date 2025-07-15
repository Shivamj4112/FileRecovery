package com.example.filerecovery.data.repository.local

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.filerecovery.R
import com.example.filerecovery.data.model.AlbumPhoto
import com.example.filerecovery.data.model.ContactItem
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.data.model.RecoveryCategory
import com.example.filerecovery.data.model.PhotoModel
import com.example.filerecovery.data.model.ScanProgress
import com.example.filerecovery.utils.FileType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

class FileRecoveryRepository(
    private val context: Context
) {

    private val recoveredFilesPath: String by lazy {
        val appName = context.getString(R.string.app_name)
        File(Environment.getExternalStorageDirectory(), "$appName/RecoveredFiles").absolutePath
    }

    suspend fun getAllAlbums(): List<AlbumPhoto> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<AlbumPhoto>()
        val photoMap = mutableMapOf<String, MutableList<PhotoModel>>()

        val root = Environment.getExternalStorageDirectory()
        scanDirectory(root, photoMap)

        photoMap.forEach { (folderPath, photos) ->
            photos.sortByDescending { it.lastModified }
            albums.add(
                AlbumPhoto(
                    folderPath = folderPath,
                    photos = photos,
                    lastModified = File(folderPath).lastModified()
                )
            )
        }

        albums.sortByDescending { it.lastModified }
        albums
    }

    suspend fun getHiddenAlbums(): List<AlbumPhoto> = withContext(Dispatchers.IO) {
        getAllAlbums().filter { File(it.folderPath).isHidden }
    }

    private fun scanDirectory(dir: File, photoMap: MutableMap<String, MutableList<PhotoModel>>) {

        if (dir.absolutePath == recoveredFilesPath) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(file, photoMap)
            } else if (isImageFile(file)) {
                val size = file.length()
                if (size > 40000) {
                    val folderPath = file.parent ?: continue
                    if (folderPath == recoveredFilesPath) continue
                    val photo = PhotoModel(
                        path = file.absolutePath,
                        lastModified = file.lastModified(),
                        size = size,
                        isHidden = file.isHidden
                    )
                    photoMap.getOrPut(folderPath) { mutableListOf() }.add(photo)
                }
            }
        }
    }

    private fun isImageFile(file: File): Boolean {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return options.outWidth != -1 && options.outHeight != -1
    }

    // Function to get the duration of a video file

    private fun getMediaDuration(file: File): Long? {
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }


// Function to get deleted files
    suspend fun scanDeletedFiles(
        fileType: String,
        onProgress: (ScanProgress) -> Unit
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val deletedFiles = mutableListOf<FileItem>()
        val recoveryCategoryMap = mutableMapOf<String, MutableList<FileItem>>()
        val allFiles = mutableListOf<FileItem>()
         var processedFiles = 0

        val contentUri = when (fileType) {
            FileType.Photos.toString() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            FileType.Videos.toString() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            FileType.Audios.toString() -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            FileType.Documents.toString() -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                throw UnsupportedOperationException("Documents not supported below Android Q")
            }
            else -> null
        }

        try {
            if (contentUri != null) {
                val trashedFiles = scanTrashedFiles(contentUri, fileType)
                deletedFiles.addAll(trashedFiles.filter { it.path != recoveredFilesPath })
            }

            val directories = getDirectoriesForFileType(fileType)
            val totalFiles = directories.sumOf { dir ->
                try {
                    dir.walk().filter { it.isFile && it.parent != recoveredFilesPath }.count()
                } catch (e: Exception) {
                    0
                }
            } + deletedFiles.size
            processedFiles = deletedFiles.size

            coroutineScope {
                directories.forEach { dir ->
                    try {
                        dir.walk().forEach { file ->
                            processedFiles++
                            if (file.isFile && isRecoverable(file) && file.parent != recoveredFilesPath) {
                                val detectedType = getFileType(file)
                                if (detectedType.toString() == fileType || matchesFileType(
                                        file,
                                        fileType
                                    )
                                ) {
                                    val resolution = if (detectedType == FileType.Photos) {
                                        getImageResolution(file)
                                    } else {
                                        null
                                    }
                                    val duration =
                                        if (detectedType == FileType.Videos || detectedType == FileType.Audios) {
                                            getMediaDuration(file)
                                        } else {
                                            null
                                        }

                                    val thumbnailUri = when (detectedType) {
                                        FileType.Photos -> getThumbnailUriForFile(file, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                        FileType.Videos -> getThumbnailUriForFile(file, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                                        else -> null
                                    }

                                    val fileItem = FileItem(
                                        path = file.absolutePath,
                                        name = file.name,
                                        type = detectedType,
                                        size = file.length(),
                                        lastModified = file.lastModified(),
                                        resolution = resolution,
                                        duration = duration,
                                        formattedSize = FileItem.formatSize(file.length()),
                                                thumbnailUri = thumbnailUri
                                    )

                                    deletedFiles.add(fileItem)
                                    allFiles.add(fileItem)
                                    val parentDir = file.parentFile?.name ?: "Uncategorized"
                                    val categoryFiles =
                                        recoveryCategoryMap.getOrPut(parentDir) { mutableListOf() }
                                    categoryFiles.add(fileItem)

                                    val progress =
                                        if (totalFiles > 0) (processedFiles * 100) / totalFiles else 100
                                    val categories = recoveryCategoryMap.map { (name, files) ->
                                        RecoveryCategory(name, files)
                                    }.filter { it.files.isNotEmpty() }
                                    onProgress(
                                        ScanProgress(
                                            file.absolutePath,
                                            progress,
                                            deletedFiles,
                                            categories
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            deletedFiles.distinctBy { it.path }
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(ScanProgress("", 100, deletedFiles, emptyList()))
            deletedFiles.distinctBy { it.path }
        }
    }

    private fun getImageResolution(file: File): String? {
        if (!isImageFile(file)) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return if (options.outWidth != -1 && options.outHeight != -1) {
            "${options.outWidth}x${options.outHeight}"
        } else {
            null
        }
    }

    private suspend fun scanTrashedFiles(contentUri: Uri, fileType: String): List<FileItem> =
        withContext(Dispatchers.IO) {
            val trashedFiles = mutableListOf<FileItem>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val projection = arrayOf(
                    MediaStore.MediaColumns._ID,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.DATA,
                    MediaStore.MediaColumns.SIZE,
                    MediaStore.MediaColumns.DATE_MODIFIED,
                    MediaStore.MediaColumns.WIDTH,
                    MediaStore.MediaColumns.HEIGHT,
                    MediaStore.MediaColumns.DURATION,
                    MediaStore.MediaColumns.RESOLUTION
                )
                val selection = "${MediaStore.MediaColumns.IS_TRASHED} = 1"
                val cursor = context.contentResolver.query(
                    contentUri,
                    projection,
                    null,
                    null
                )
                cursor?.use {
                    while (it.moveToNext()) {

                        val fileType = when (fileType) {
                            FileType.Photos.toString() -> FileType.Photos
                            FileType.Videos.toString() -> FileType.Videos
                            FileType.Audios.toString() -> FileType.Audios
                            FileType.Documents.toString() -> FileType.Documents
                            FileType.Contacts.toString() -> FileType.Contacts
                            else -> FileType.Other
                        }
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))

                        val path = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA))
                        val name =
                            it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                        val size =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE))
                        val lastModified =
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)) * 1000

                        val duration = if (fileType == FileType.Videos || fileType == FileType.Audios) {
                            it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DURATION))
                        } else {
                            null
                        }

                        val resolution = if (fileType == FileType.Photos) {
                            it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.RESOLUTION))
                        } else {
                            null
                        }

                        val thumbnailUri = when (fileType) {
                            FileType.Photos -> getThumbnailUri(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, MediaStore.Images.Thumbnails.IMAGE_ID, id)
                            FileType.Videos -> getThumbnailUri(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI, MediaStore.Video.Thumbnails.VIDEO_ID, id)
                            else -> null
                        }


                        trashedFiles.add(
                            FileItem(
                                path = path,
                                name = name,
                                type = fileType,
                                size = size,
                                lastModified = lastModified,
                                resolution = resolution,
                                duration = duration,
                                formattedSize = FileItem.formatSize(size),
                                thumbnailUri = thumbnailUri
                            )
                        )
                    }
                }
                trashedFiles
            } else {
                val fallbackDirs = when (fileType) {
                    FileType.Photos.toString() -> listOf(
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                            ".thumbnails"
                        )
                    )

                    FileType.Videos.toString() -> listOf(
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                            "temp"
                        )
                    )

                    FileType.Audios.toString() -> listOf(
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                            "temp"
                        )
                    )

                    FileType.Documents.toString() -> listOf(
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                            "temp"
                        )
                    )

                    else -> {
                        emptyList()
                    }
                }

                fallbackDirs.forEach { dir ->
                    if (dir.exists()) {
                        dir.walk().forEach { file ->
                            if (file.isFile && matchesFileType(file, fileType)) {
                                val resolution = if (fileType == FileType.Photos.toString()) {
                                    getImageResolution(file)
                                } else {
                                    null
                                }
                                trashedFiles.add(
                                    FileItem(
                                        path = file.absolutePath,
                                        name = file.name,
                                        type = getFileType(file),
                                        size = file.length(),
                                        lastModified = file.lastModified(),
                                        resolution = resolution,
                                        formattedSize = FileItem.formatSize(file.length())
                                    )
                                )
                            }
                        }
                    }
                }

                trashedFiles
            }
        }

    private fun getThumbnailUri(contentUri: Uri, idColumn: String, id: Long): Uri? {
        val cursor = context.contentResolver.query(
            contentUri,
            arrayOf(MediaStore.Images.Thumbnails.DATA),
            "$idColumn = ?",
            arrayOf(id.toString()),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val path = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Thumbnails.DATA))
                return Uri.fromFile(File(path))
            }
        }
        return null
    }

    private fun getThumbnailUriForFile(file: File, contentUri: Uri): Uri? {
        val cursor = context.contentResolver.query(
            contentUri,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DATA} = ?",
            arrayOf(file.absolutePath),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return getThumbnailUri(
                    if (contentUri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI else MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    if (contentUri == MediaStore.Images.Media.EXTERNAL_CONTENT_URI) MediaStore.Images.Thumbnails.IMAGE_ID else MediaStore.Video.Thumbnails.VIDEO_ID,
                    id
                )
            }
        }
        return null
    }

    private fun getDirectoriesForFileType(fileType: String): List<File> {
        val baseDir = Environment.getExternalStorageDirectory()

        val typeDirectoryMap = mapOf(
            FileType.Photos.toString() to "",
            FileType.Videos.toString() to "",
            FileType.Audios.toString() to "",
            FileType.Documents.toString() to ""
        )

        return typeDirectoryMap[fileType]
            ?.let { listOf(File(baseDir, it)) }
            ?.filter { it.exists() && it.absolutePath != recoveredFilesPath}
            ?: emptyList()
    }

    private fun isRecoverable(file: File): Boolean {
        return file.exists()
    }

    private fun matchesFileType(file: File, fileType: String): Boolean {
        return when (fileType) {
            FileType.Photos.toString() -> file.extension.lowercase() in listOf(
                "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg", "tiff", "ico", "raw"
            )


            FileType.Videos.toString() -> file.extension.lowercase() in listOf(
                "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "mpeg", "mpg", "m4v", "ts", "vob"
            )

            FileType.Audios.toString() -> file.extension.lowercase() in listOf(
                "mp3", "wav", "aac", "ogg", "m4a", "flac", "wma", "amr", "opus", "aiff"
            )

            FileType.Documents.toString() -> file.extension.lowercase() in listOf(
                "pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv", "epub", "html", "xml", "md", "json","apk","aab"
            )

            else -> false
        }
    }

    fun getFileType(file: File): FileType {
        return when (file.extension.lowercase()) {
            in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg", "tiff", "ico", "raw") -> FileType.Photos
            in listOf("mp3", "wav", "aac", "ogg", "m4a", "flac", "wma", "amr", "opus", "aiff") -> FileType.Audios
            in listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "3gp", "mpeg", "mpg", "m4v", "ts", "vob") -> FileType.Videos
            in listOf("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx", "csv", "epub", "html", "xml", "md", "json","apk","aab") -> FileType.Documents
            else -> FileType.Other
        }
    }


    suspend fun recoverFile(fileItem: FileItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val appName = context.getString(R.string.app_name)
            val sourceFile = File(fileItem.path)
            val recoveryDir = File(
                Environment.getExternalStorageDirectory(),
                "$appName/RecoveredFiles"
            )
            recoveryDir.mkdirs()
            val destFile = File(recoveryDir, fileItem.name)
            sourceFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("Range")
    suspend fun scanDeletedContacts(): List<ContactItem> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ContactItem>()
        val contentResolver: ContentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val name =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val phoneNumber = getPhoneNumber(contentResolver, id)
                contacts.add(ContactItem(id, name, phoneNumber))
            }
        }
        contacts
    }

    @SuppressLint("Range")
    private fun getPhoneNumber(
        contentResolver: ContentResolver,
        contactId: String
    ): String? {
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            } else null
        }
    }
}