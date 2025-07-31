package com.example.filerecovery.data.model

sealed class FileListItem {
    object TopHeader : FileListItem()
    data class Header(val date: String, val isSelected: Boolean) : FileListItem()
    data class File(val fileItem: FileItem, val date: String) : FileListItem()
}
