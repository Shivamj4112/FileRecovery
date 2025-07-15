package com.example.filerecovery.data.model

data class ScanProgress(
    val currentPath: String,
    val progress: Int,
    val scannedFiles: List<FileItem>,
    val photoCategories: List<RecoveryCategory> = emptyList()
)
