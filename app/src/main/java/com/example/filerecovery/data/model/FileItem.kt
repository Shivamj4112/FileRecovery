package com.example.filerecovery.data.model

import android.net.Uri
import android.os.Parcelable
import com.example.filerecovery.utils.FileType
import kotlinx.parcelize.Parcelize

@Parcelize
data class FileItem(
    val path: String,
    val name: String,
    val type: FileType,
    val size: Long,
    val lastModified: Long,
    val duration : Long? = null,
    val resolution: String? = null,
    val formattedSize: String,
    val thumbnailUri: Uri? = null
) : Parcelable {

    companion object {
        fun formatSize(bytes: Long): String {
            val kb = 1024.0
            val mb = kb * 1024
            val gb = mb * 1024

            return when {
                bytes >= gb -> String.format("%.1f GB", bytes / gb)
                bytes >= mb -> String.format("%.1f MB", bytes / mb)
                bytes >= kb -> String.format("%.1f KB", bytes / kb)
                else -> "$bytes Bytes"
            }
        }
    }
}