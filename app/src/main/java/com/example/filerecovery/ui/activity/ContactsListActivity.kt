package com.example.filerecovery.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filerecovery.R
import com.example.filerecovery.data.viewmodel.local.ContactsViewModel
import com.example.filerecovery.databinding.ActivityContactsListBinding
import com.example.filerecovery.databinding.DialogBackupBinding
import com.example.filerecovery.databinding.LoadingDialogBinding
import com.example.filerecovery.ui.adapter.BackupFilesAdapter
import com.example.filerecovery.ui.adapter.ContactsAdapter
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.ViewModelSingleton
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class ContactsListActivity : BaseActivity() {

    private lateinit var binding: ActivityContactsListBinding
    var contactsViewModel = ViewModelSingleton.contactsViewModel
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var backupFilesAdapter: BackupFilesAdapter
    private var listType: String? = null
    private var fileCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityContactsListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        listType = intent.getStringExtra("listType")
        fileCount = intent.getIntExtra("fileCount", 0)

        setupUI()


        fetchContactsData()
        checkContactsPermission()
    }

    private fun setupUI() {

        binding.ivArrowBack.setOnClickListener { finish() }

        binding.ivSelectAll.setOnClickListener {
            when (listType) {
                "all" -> {
                    val isAllSelected = contactsViewModel.contactsList.value?.all { it.isSelected } ?: false
                    contactsViewModel.selectAllContacts(!isAllSelected)
                    updateSelectAllImage()
                    contactsAdapter.notifyDataSetChanged()
                }
                "deleted" -> {
                    val allSelected = contactsAdapter.areAllContactsSelected()
                    contactsViewModel.deletedContactsList.value?.forEach { contact ->
                        contactsViewModel.toggleContactSelection(contact, !allSelected)
                    }
                }
            }
        }

        binding.ivRefreshShare.setOnClickListener {
            when (listType) {
                "all" -> {
                    val selectedContacts = contactsViewModel.getSelectedContacts()
                    if (selectedContacts.isEmpty()) {
                        Toast.makeText(this, getString(R.string.please_select_at_least_one_contact_to_share), Toast.LENGTH_SHORT).show()
                    } else {
                        val shareText = selectedContacts.joinToString("\n") { contact ->
                            "${contact.name}: ${contact.phoneNumber ?: "No number"}"
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            putExtra(Intent.EXTRA_SUBJECT, "Shared Contacts")
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Contacts"))
                    }
                }
                "deleted" -> {
                    fetchContactsData()
                    checkContactsPermission()
                }
            }

        }

        binding.tvTitleName.text = when (listType) {
            "all" -> getString(R.string.all_contacts)
            "deleted" -> getString(R.string.deleted_contacts)
            "backup" -> getString(R.string.backup_contacts)
            else -> getString(R.string.contacts)
        }

//        binding.tvFoundedFilesCount.text = getString(R.string.found_contacts, fileCount.toString())
//
//        binding.llFileCount.visibility = if (listType == "backup") View.GONE else View.VISIBLE

        if (listType == "backup") {
            backupFilesAdapter = BackupFilesAdapter(
                fileSelected = { file ->
                    startActivity(
                        Intent(
                            this@ContactsListActivity,
                            VcfContactsActivity::class.java
                        ).apply {
                            putExtra("vcfFilePath", file.path)
                        })
                },
            )
            binding.rvContacts.apply {
                layoutManager = LinearLayoutManager(this@ContactsListActivity)
                adapter = backupFilesAdapter
            }
        } else {
            contactsAdapter = ContactsAdapter { contact, isChecked ->
                contactsViewModel.toggleContactSelection(contact, isChecked)
                updateSelectAllImage()
            }
            binding.rvContacts.apply {
                layoutManager = LinearLayoutManager(this@ContactsListActivity)
                adapter = contactsAdapter
            }
        }


        binding.btnRecover.setOnClickListener {
            when (listType) {
                "all" -> {
                    if ((contactsViewModel.selectedContactsSize.value ?: 0) > 0) {
                        showBackupDialog()
                    } else {
                        Toast.makeText(this, getString(R.string.please_select_at_least_one_contact_to_backup), Toast.LENGTH_SHORT).show()
                    }
                }

                "deleted" -> {
                    if ((contactsViewModel.selectedContactsSize.value ?: 0) > 0) {
                        CoroutineScope(Dispatchers.Main).launch {
                            val result = contactsViewModel.recoverSelectedContacts(contentResolver)
                            Toast.makeText(this@ContactsListActivity, result, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, getString(R.string.please_select_at_least_one_contact_to_recover), Toast.LENGTH_SHORT).show()
                    }
                }

                "backup" -> {
                    binding.llBottomBar.isEnabled = false
                }
            }
        }

        binding.rvContacts.visibility = View.VISIBLE
        binding.llEmptyLayout.visibility = View.GONE

        binding.btGoBack.setOnClickListener {
            finish()
        }

        when (listType) {
            "backup" -> {

                binding.tvSelectAll.visibility = View.GONE
                binding.ivSelectAll.visibility = View.GONE

                contactsViewModel.backupFilesList.observe(this) { files ->
                    if (files.isEmpty()) {
                        binding.llEmptyLayout.visibility = View.VISIBLE
                        binding.rvContacts.visibility = View.GONE
                        binding.tvEmpty.text = getString(R.string.no_backup_contacts)
                        binding.llBottomBar.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE
                        binding.btnRecover.isEnabled = false
                    } else {
                        backupFilesAdapter.setFiles(files, lifecycleScope)
                        binding.llEmptyLayout.visibility = View.GONE
                        binding.rvContacts.visibility = View.VISIBLE
                        binding.llBottomBar.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                        binding.btnRecover.isEnabled = true
                    }
                    binding.llBottomBar.visibility = View.GONE
                }
            }

            "all" -> {
                binding.ivRefreshShare.setImageResource(R.drawable.ic_share)
                binding.ivRefreshShare.setColorFilter(ContextCompat.getColor(this@ContactsListActivity, R.color.text_gray))
                contactsViewModel.contactsList.observe(this) { contacts ->
                    if (contacts.isEmpty()) {
                        binding.llEmptyLayout.visibility = View.VISIBLE
                        binding.rvContacts.visibility = View.GONE
                        binding.tvEmpty.text = getString(R.string.no_all_contacts)
                        binding.progressBar.visibility = View.GONE
                        binding.btnRecover.isEnabled = false
                    } else {
                        contactsAdapter.setContacts(contacts,listType!!)
                        binding.llEmptyLayout.visibility = View.GONE
                        binding.progressBar.visibility = View.GONE
                        binding.rvContacts.visibility = View.VISIBLE
                        binding.btnRecover.isEnabled = true
                    }
                    updateSelectAllImage()
                }
            }

            "deleted" -> {
                contactsViewModel.deletedContactsList.observe(this) { contacts ->
                    if (contacts.isEmpty()) {
                        binding.llEmptyLayout.visibility = View.VISIBLE
                        binding.rvContacts.visibility = View.GONE
                        binding.tvEmpty.text = getString(R.string.no_deleted_contacts)
                        binding.progressBar.visibility = View.GONE
                        binding.btnRecover.isEnabled = false
                        contactsAdapter.setContacts(emptyList(),listType!!)
                    } else {
                        contactsAdapter.setContacts(contacts,listType!!)
                        binding.llEmptyLayout.visibility = View.GONE
                        binding.rvContacts.visibility = View.VISIBLE
                        binding.progressBar.visibility = View.GONE
                        binding.btnRecover.isEnabled = true
                    }
                    updateSelectAllImage()
                }
            }
        }



        contactsViewModel.selectedContactsSize.observe(this) { size ->
            binding.btnRecover.text = when (listType) {
                "all" -> if (size > 0) getString(R.string.backup_with_count, size.toString()) else getString(R.string.backup)
                "deleted" -> if (size > 0) getString(R.string.recover_with_count, size.toString()) else getString(R.string.recover)
                else ->if (size > 0) getString(R.string.backup_with_count, size.toString()) else getString(R.string.backup)
            }
            updateSelectAllImage()
        }

        contactsViewModel.isLoading.observe(this) { isLoading ->
            binding.rvContacts.visibility =  View.VISIBLE
            binding.llEmptyLayout.visibility =
                if (isLoading == true) View.GONE else binding.llEmptyLayout.visibility
            binding.btnRecover.visibility = if (isLoading == true) View.GONE else View.VISIBLE
        }
    }

    private fun checkContactsPermission() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (listType == "deleted" || listType == "backup") {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CONTACTS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_CONTACTS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissions(permissionsToRequest.toTypedArray(), 100)
        } else {
            fetchContactsData()
        }
    }


    private fun fetchContactsData() {
        CoroutineScope(Dispatchers.Main).launch {
            when (listType) {
                "all" -> {
                    contactsViewModel.fetchAllContacts(contentResolver)
                    contactsViewModel.loadAllContacts()
                }

                "deleted" -> {
                    contactsViewModel.fetchAllContacts(contentResolver)
                    contactsViewModel.fetchDeletedContacts()
                    contactsViewModel.loadDeletedContacts()
                }

                "backup" -> {
                    fetchBackupFiles()
                }
            }
        }
    }

    private fun fetchBackupFiles() {
        CoroutineScope(Dispatchers.Main).launch {
            contactsViewModel.fetchBackupFiles(getExternalFilesDir(null)?.absolutePath)
        }
    }

    private fun showBackupDialog() {
        val dialog = BottomSheetDialog(this)
        val binding = DialogBackupBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.background = ContextCompat.getDrawable(this, R.drawable.bottomsheet_card)

        binding.apply {

            btBackup.isEnabled = false
            btBackup.setTextColor(
                ContextCompat.getColor(
                    this@ContactsListActivity,
                    R.color.light_photo_color
                )
            )
            tvCount.text = "0/50"

            etName.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val currentLength = s?.length ?: 0
                    tvCount.text = "$currentLength/50"

                    when {
                        currentLength == 0 -> {
                            btBackup.isEnabled = false
                            btBackup.setTextColor(
                                ContextCompat.getColor(
                                    this@ContactsListActivity,
                                    R.color.light_photo_color
                                )
                            )
//                            etName.error = null
                        }

                        currentLength > 50 -> {
                            btBackup.isEnabled = false
//                            etName.error = "Character limit exceeded"
                            btBackup.setTextColor(
                                ContextCompat.getColor(
                                    this@ContactsListActivity,
                                    R.color.light_photo_color
                                )
                            )
                        }

                        else -> {
                            btBackup.isEnabled = true
//                            etName.error = null
                            btBackup.setTextColor(
                                ContextCompat.getColor(
                                    this@ContactsListActivity,
                                    R.color.white
                                )
                            ) // or active color
                        }
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            btBackup.setOnClickListener {
                val fileName = etName.text.toString()

                val intent = Intent(this@ContactsListActivity, ProgressActivity::class.java).apply {
                    putExtra("operationType", "backup")
                    putExtra("fileName", fileName)
                    putParcelableArrayListExtra("selectedContacts", ArrayList(contactsViewModel.getSelectedContacts().toList()))
                    putExtra("storagePath", getExternalFilesDir(null)?.absolutePath)
                }
                startActivity(intent)
                dialog.dismiss()


//                CoroutineScope(Dispatchers.Main).launch {
//                    val result = contactsViewModel.backupSelectedContacts(fileName, getExternalFilesDir(null)?.absolutePath)
//                    showToast(result)
//                }
//                dialog.dismiss()
            }

            btCancel.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchContactsData()
        } else {
            Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSelectAllImage() {
        val contacts = contactsViewModel.contactsList.value ?: emptyList()
        val allSelected = contacts.isNotEmpty() && contacts.all { it.isSelected }
        binding.ivSelectAll.setImageResource(
            if (allSelected) R.drawable.ic_tick_selected else R.drawable.ic_tick_unselected_black
        )
    }
}