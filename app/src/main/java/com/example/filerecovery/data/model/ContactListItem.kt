package com.example.filerecovery.data.model

sealed class ContactListItem {
    data class Header(val title: String, val fileCount: Int, val listType: String) : ContactListItem()
    data class ContactItem(val contact: Contact) : ContactListItem()
}
