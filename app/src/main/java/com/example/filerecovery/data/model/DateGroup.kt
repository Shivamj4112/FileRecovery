package com.example.filerecovery.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class DateGroup(
    val date: String,
    val files: List<FileItem>,
) : Parcelable