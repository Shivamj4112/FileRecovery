package com.example.filerecovery.utils

import androidx.recyclerview.widget.DiffUtil
import com.example.filerecovery.data.model.Contact

class ContactDiffCallback(
    private val oldList: List<Contact>,
    private val newList: List<Contact>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldContact = oldList[oldItemPosition]
        val newContact = newList[newItemPosition]
        return oldContact.name == newContact.name &&
                oldContact.phoneNumber == newContact.phoneNumber &&
                oldContact.isSelected == newContact.isSelected
    }
}