package com.example.filerecovery.data.model

data class AlbumPhoto(
    val folderPath: String,
    val photos: List<PhotoModel>,
    val lastModified: Long
)
