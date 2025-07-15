package com.example.filerecovery.data.model

import android.os.Parcelable
import com.example.filerecovery.utils.FileType
import kotlinx.parcelize.Parcelize

@Parcelize
data class RecoveryCategory(
    val categoryName: String,
    val files: List<FileItem>,
    val type: FileType = FileType.Photos
) : Parcelable
