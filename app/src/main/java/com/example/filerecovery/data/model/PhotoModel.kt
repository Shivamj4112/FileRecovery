package com.example.filerecovery.data.model

data class PhotoModel(
    val path: String,
    val lastModified: Long,
    val size: Long,
    val isChecked: Boolean = false,
    val isHidden: Boolean = false
)
