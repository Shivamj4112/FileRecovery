package com.example.filerecovery.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filerecovery.R
import com.example.filerecovery.data.viewmodel.local.ContactsViewModel
import com.example.filerecovery.databinding.ActivityVcfContactsBinding
import com.example.filerecovery.ui.adapter.ContactsAdapter
import com.example.filerecovery.utils.ViewModelSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class VcfContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVcfContactsBinding
    var contactsViewModel = ViewModelSingleton.contactsViewModel
    private lateinit var contactsAdapter: ContactsAdapter
    private var vcfFilePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVcfContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        vcfFilePath = intent.getStringExtra("vcfFilePath")

        setupUI()
        fetchVcfContacts()
    }

    private fun setupUI() {
        binding.ivArrowBack.setOnClickListener { finish() }

        contactsAdapter = ContactsAdapter { contact, isChecked ->
            contactsViewModel.toggleContactSelection(contact, isChecked)
            updateSelectAllImage()
        }
        binding.rvContacts.apply {
            layoutManager = LinearLayoutManager(this@VcfContactsActivity)
            adapter = contactsAdapter
        }

        binding.ivSelectAll.setOnClickListener {
            val isAllSelected = contactsViewModel.vcfContactsList.value?.all { it.isSelected } ?: false
            contactsViewModel.selectAllVcfContacts(!isAllSelected)
            updateSelectAllImage()
            contactsAdapter.notifyDataSetChanged()
        }

        binding.btnRecover.setOnClickListener {
            if ((contactsViewModel.selectedContactsSize.value ?: 0) > 0) {
                CoroutineScope(Dispatchers.Main).launch {
                    val result = contactsViewModel.recoverSelectedContacts(contentResolver)
                    Toast.makeText(this@VcfContactsActivity, result, Toast.LENGTH_SHORT).show()
                    if (result.contains("successfully", ignoreCase = true)) {
                        finish()
                    }
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.please_select_at_least_one_contact_to_recover),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        contactsViewModel.vcfContactsList.observe(this) { contacts ->
            if (contacts.isEmpty()) {
                binding.rvContacts.visibility = View.GONE
                binding.progressBar.visibility = View.GONE
                binding.btnRecover.isEnabled = false
                binding.ivSelectAll.isEnabled = false
            } else {
                contactsAdapter.setContacts(contacts,"backup")
                binding.rvContacts.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
                binding.btnRecover.isEnabled = true
                binding.ivSelectAll.isEnabled = true
            }
            updateSelectAllImage()
        }

        contactsViewModel.selectedContactsSize.observe(this) { size ->
            binding.btnRecover.text = if (size > 0) {
                getString(R.string.recover_with_count, size.toString())
            } else {
                getString(R.string.recover  )
            }
        }

        contactsViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
            binding.rvContacts.visibility = if (isLoading == true) View.GONE else View.VISIBLE
            binding.btnRecover.visibility = if (isLoading == true) View.GONE else View.VISIBLE
        }
    }

    private fun updateSelectAllImage() {
        val contacts = contactsViewModel.vcfContactsList.value ?: emptyList()
        val allSelected = contacts.isNotEmpty() && contacts.all { it.isSelected }
        binding.ivSelectAll.setImageResource(
            if (allSelected) R.drawable.ic_tick_selected else R.drawable.ic_tick_unselected_black
        )
    }


    private fun fetchVcfContacts() {
        CoroutineScope(Dispatchers.Main).launch {
            vcfFilePath?.let { path ->
                contactsViewModel.fetchContactsFromVcf(File(path))
            }
        }
    }
}