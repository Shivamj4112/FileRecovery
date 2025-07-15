package com.example.filerecovery.utils

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.Settings.Global.getString
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.filerecovery.R
import com.example.filerecovery.data.model.FileItem
import java.io.File

object FileUtils {


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

    fun getRecoveredFiles(appName: String): List<FileItem> {
        val recoveryDir = File(Environment.getExternalStorageDirectory(), "$appName/RecoveredFiles")
        if (!recoveryDir.exists()) return emptyList()

        return recoveryDir.listFiles()?.mapNotNull { file ->
            if (file.isFile) {

                val duration = if (getFileType(file) == FileType.Videos || getFileType(file) == FileType.Audios) {
                    getMediaDuration(file)
                } else {
                    null
                }

                FileItem(
                    path = file.absolutePath,
                    name = file.name,
                    type = getFileType(file),
                    size = file.length(),
                    lastModified = file.lastModified(),
                    resolution = getImageResolution(file),
                    duration = duration,
                    formattedSize = FileItem.formatSize(file.length())
                )
            } else {
                null
            }
        } ?: emptyList()
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

    fun getMimeType(fileItem: FileItem): String {
        return when (fileItem.type) {
            FileType.Photos -> "image/*"
            FileType.Videos -> "video/*"
            FileType.Audios -> "audio/*"
            FileType.Documents -> when (fileItem.name.substringAfterLast(".").lowercase()) {
                "pdf" -> "application/pdf"
                "doc", "docx" -> "application/msword"
                "txt" -> "text/plain"
                else -> "*/*"
            }

            else -> "*/*"
        }
    }

    fun Activity.shareFile(fileItem: FileItem) {
        val file = File(fileItem.path)
        if (!file.exists()) {
            Toast.makeText(this, getString(R.string.file_does_not_exist), Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = getMimeType(fileItem)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share File"))
        } catch (e: Exception) {

        }
    }

    fun getImageResolution(file: File): String? {
        if (getFileType(file) != FileType.Photos) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return if (options.outWidth != -1 && options.outHeight != -1) {
            "${options.outWidth}x${options.outHeight}"
        } else {
            null
        }
    }


    fun getFolderSize(folder: File): Long {
        var size = 0L
        if (folder.exists()) {
            folder.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getFolderSize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    fun scanForJunkFiles(directory: File, extensions: List<String>): Long {
        var size = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    size += scanForJunkFiles(file, extensions)
                } else if (extensions.any { file.extension.equals(it.removePrefix("."), true) }) {
                    size += file.length()
                }
            }
        }
        return size
    }

    fun deleteFolderContents(folder: File): Long {
        var deletedSize = 0L
        if (folder.exists()) {
            folder.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deletedSize += deleteFolderContents(file)
                }
                if (file.delete()) {
                    deletedSize += file.length()
                }
            }
        }
        return deletedSize
    }

    fun deleteJunkFiles(directory: File, extensions: List<String>): Long {
        var deletedSize = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deletedSize += deleteJunkFiles(file, extensions)
                } else if (extensions.any { file.extension.equals(it.removePrefix("."), true) }) {
                    if (file.delete()) {
                        deletedSize += file.length()
                    }
                }
            }
        }
        return deletedSize
    }
}