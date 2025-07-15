package com.example.filerecovery.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.filerecovery.R
import com.example.filerecovery.data.model.Contact
import com.example.filerecovery.databinding.ItemContactBinding

class ContactsAdapter(
    private val onSelectionChanged: (Contact, Boolean) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private var contacts: List<Contact> = emptyList()

    class ContactViewHolder(val binding: ItemContactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.binding.apply {
            tvContactName.text = contact.name
            tvContactNumber.text = contact.phoneNumber ?: "No number"
            ivSelect.setImageResource(
                if (contact.isSelected) R.drawable.ic_tick_selected
                else R.drawable.ic_tick_unselected_gray
            )
            ivSelect.setOnClickListener {
                contact.isSelected = !contact.isSelected
                onSelectionChanged(contact, contact.isSelected)
                ivSelect.setImageResource(
                    if (contact.isSelected) R.drawable.ic_tick_selected
                    else R.drawable.ic_tick_unselected_gray
                )
            }
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun setContacts(newContacts: List<Contact>) {
        contacts = newContacts
        notifyDataSetChanged()
    }
    fun areAllContactsSelected(): Boolean {
        return contacts.isNotEmpty() && contacts.all { it.isSelected }
    }

}