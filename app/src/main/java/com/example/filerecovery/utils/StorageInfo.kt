package com.example.filerecovery.utils

import android.app.usage.StorageStatsManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import com.example.filerecovery.R
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

object StorageInfo {

    fun getStorageStatsInGB(context: Context): Triple<Double, Double, Double> {

        var totalBytes: Long
        var freeBytes: Long

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val uuid = storageManager.getUuidForPath(Environment.getDataDirectory())
                totalBytes = storageStatsManager.getTotalBytes(uuid)
                freeBytes = storageStatsManager.getFreeBytes(uuid)
            } catch (e: Exception) {
                // Fallback to StatFs for error cases
                val stat = StatFs(Environment.getDataDirectory().path)
                val blockSize = stat.blockSizeLong
                totalBytes = stat.blockCountLong * blockSize
                freeBytes = stat.availableBlocksLong * blockSize
            }
        } else {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            totalBytes = stat.blockCountLong * blockSize
            freeBytes = stat.availableBlocksLong * blockSize
        }

        val usedBytes = totalBytes - freeBytes
        val totalGB = totalBytes / 1_000_000_000.0
        val usedGB = usedBytes / 1_000_000_000.0
        val freeGB = freeBytes / 1_000_000_000.0

        return Triple(
            (totalGB * 100).roundToInt() / 100.0,
            (usedGB * 100).roundToInt() / 100.0,
            (freeGB * 100).roundToInt() / 100.0
        )
    }

    fun getUsedStorage(context: Context): String {
        val (_, usedGB, _) = getStorageStatsInGB(context)
        return "${formatSize(usedGB)} ${context.getString(R.string.gb)}"
    }

    fun getUsedStorageInGB(context: Context): String {
        val (_, usedGB, _) = getStorageStatsInGB(context)
        return "${formatSize(usedGB)}"
    }

    fun getAvailableStorage(context: Context): String {
        val (_, _, availableGB) = getStorageStatsInGB(context)
        return "$availableGB ${context.getString(R.string.gb)}"
    }

    fun getTotalStorage(context: Context): String {
        val (totalGb, _, _) = getStorageStatsInGB(context)
        return "${formatSize(totalGb)} ${context.getString(R.string.gb)}"
    }

    fun getTotalStorageInGB(context: Context): String {
        val (totalGb, _, _) = getStorageStatsInGB(context)
        return "${formatSize(totalGb)}"
    }

    private fun getFileStats(
        context: Context,
        uri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): Pair<Long, Int> {
        val contentResolver = context.contentResolver
        val projection = arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE)
        var totalSize = 0L
        var count = 0

        contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            count = cursor.count
            if (count > 0) {
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while (cursor.moveToNext()) {
                    totalSize += cursor.getLong(sizeColumn)
                }
            }
        }

        return Pair(totalSize, count)
    }

    fun getStorageUsageByCategory(context: Context): Map<FileType, Pair<Double, Int>> {

        val buildSelection = { mimeTypes: Array<String> ->
            mimeTypes.joinToString(",") { "?" }.let {
                "${MediaStore.Files.FileColumns.MIME_TYPE} IN ($it)"
            }
        }

        val photos = getFileStats(
            context,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            buildSelection(photoMimeTypes),
            photoMimeTypes
        )

        val videos = getFileStats(
            context,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            buildSelection(videoMimeTypes),
            videoMimeTypes
        )

        val audios = getFileStats(
            context,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            buildSelection(audioMimeTypes),
            audioMimeTypes
        )

        val documents = getFileStats(
            context,
            MediaStore.Files.getContentUri("external"),
            buildSelection(documentMimeTypes),
            documentMimeTypes
        )

        val allFiles = getFileStats(
            context,
            MediaStore.Files.getContentUri("external")
        )

        val totalKnownSize = photos.first + videos.first + audios.first + documents.first
        val totalKnownCount = photos.second + videos.second + audios.second + documents.second

        val otherSize = (allFiles.first - totalKnownSize).coerceAtLeast(0L)
        val otherCount = (allFiles.second - totalKnownCount).coerceAtLeast(0)

        val toGB = { size: Long -> (size / 1_000_000_000.0 * 100).roundToInt() / 100.0 }

        return mapOf(
            FileType.Photos to Pair(toGB(photos.first), photos.second),
            FileType.Videos to Pair(toGB(videos.first), videos.second),
            FileType.Audios to Pair(toGB(audios.first), audios.second),
            FileType.Documents to Pair(toGB(documents.first), documents.second),
            FileType.Other to Pair(toGB(otherSize), otherCount)
        )
    }

    private val photoMimeTypes = arrayOf(
        "image/jpeg", "image/png", "image/webp", "image/gif", "image/bmp", "image/heif", "image/heic"
    )

    private val videoMimeTypes = arrayOf(
        "video/mp4", "video/3gpp", "video/avi", "video/x-matroska", "video/mpeg", "video/webm", "video/quicktime"
    )

    private val audioMimeTypes = arrayOf(
        "audio/mpeg", "audio/mp4", "audio/x-wav", "audio/3gpp", "audio/ogg", "audio/flac", "audio/amr", "audio/x-ms-wma"
    )

    private val documentMimeTypes = arrayOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain",
        "text/html",
        "application/rtf",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.spreadsheet",
        "application/vnd.oasis.opendocument.presentation"
    )

    fun formatSize(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", value)
        }
    }

}