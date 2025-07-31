package com.example.filerecovery.utils

import androidx.recyclerview.widget.DiffUtil
import com.example.filerecovery.data.model.ContactListItem

class ContactDiffCallback(
    private val oldList: List<ContactListItem>,
    private val newList: List<ContactListItem>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]

        return when {
            old is ContactListItem.Header && new is ContactListItem.Header -> old.title == new.title
            old is ContactListItem.ContactItem && new is ContactListItem.ContactItem ->
                old.contact.name == new.contact.name && old.contact.phoneNumber == new.contact.phoneNumber
            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
