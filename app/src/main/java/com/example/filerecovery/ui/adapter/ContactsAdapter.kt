package com.example.filerecovery.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.filerecovery.R
import com.example.filerecovery.data.model.Contact
import com.example.filerecovery.data.model.ContactListItem
import com.example.filerecovery.databinding.ItemContactBinding
import com.example.filerecovery.databinding.LayoutConatactItemCountBinding
import com.example.filerecovery.utils.ContactDiffCallback

class ContactsAdapter(
    private val onSelectionChanged: (Contact, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<ContactListItem> = emptyList()

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CONTACT = 1
    }

    inner class HeaderViewHolder(val binding: LayoutConatactItemCountBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ContactListItem.Header) {
            val context = binding.root.context
            binding.tvFoundedFilesCount.text =
                context.getString(R.string.found_contacts, header.fileCount.toString())
            binding.root.visibility =
                if (header.listType == "backup") View.GONE else View.VISIBLE
        }
    }

    inner class ContactViewHolder(val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(contact: Contact) {
            binding.tvContactName.text = contact.name
            binding.tvContactNumber.text = contact.phoneNumber ?: "No number"

            binding.ivSelect.setImageResource(
                if (contact.isSelected) R.drawable.ic_tick_selected
                else R.drawable.ic_tick_unselected_gray
            )

            binding.ivSelect.setOnClickListener {
                contact.isSelected = !contact.isSelected
                onSelectionChanged(contact, contact.isSelected)
                notifyItemChanged(bindingAdapterPosition)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ContactListItem.Header -> VIEW_TYPE_HEADER
            is ContactListItem.ContactItem -> VIEW_TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val binding = LayoutConatactItemCountBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }

            VIEW_TYPE_CONTACT -> {
                val binding = ItemContactBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ContactViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ContactListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ContactListItem.ContactItem -> (holder as ContactViewHolder).bind(item.contact)
        }
    }

    override fun getItemCount(): Int = items.size

    fun setContacts(newContacts: List<Contact>, listType: String) {
        val sortedContacts = newContacts.sortedBy { it.name.lowercase() }

        val newItems = mutableListOf<ContactListItem>()

        if (listType != "backup") {
            newItems.add(ContactListItem.Header("Contacts", sortedContacts.size, listType))
        }
        sortedContacts.forEach { contact ->
            newItems.add(ContactListItem.ContactItem(contact))
        }

        val diffCallback = ContactDiffCallback(items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    fun areAllContactsSelected(): Boolean {
        return items.filterIsInstance<ContactListItem.ContactItem>().all { it.contact.isSelected }
    }
}

