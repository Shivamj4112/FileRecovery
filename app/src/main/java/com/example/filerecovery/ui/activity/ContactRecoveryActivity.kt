package com.example.filerecovery.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filerecovery.R
import com.example.filerecovery.data.viewmodel.local.ContactsViewModel
import com.example.filerecovery.databinding.ActivityContactRecoveryBinding
import com.example.filerecovery.ui.adapter.ContactsAdapter
import com.example.filerecovery.utils.DialogUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class ContactRecoveryActivity : BaseActivity() {

    private lateinit var binding: ActivityContactRecoveryBinding
    private val contactsViewModel: ContactsViewModel by viewModels()
    private var allContactsCount = 0
    private var allDeletedCount = 0
    private var allBackupCount = 0


    private val contactsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchInitialContactsData()
        } else {
            Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show()

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityContactRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {
            tvTitleName.text = getString(R.string.category_recovery, getString(R.string.contacts))
            setupUI()
        }

        checkContactsPermission()
    }

    private fun setupUI() {
        binding.ivArrowBack.setOnClickListener { finish() }

        binding.llAllContacts.setOnClickListener {
            startActivity(Intent(this, ContactsListActivity::class.java).apply {
                putExtra("listType", "all")
                putExtra("fileCount" , allContactsCount)
            })
        }

        binding.llDeletedContacts.setOnClickListener {
            startActivity(Intent(this, ContactsListActivity::class.java).apply {
                putExtra("listType", "deleted")
                putExtra("fileCount" , allDeletedCount)
            })
        }

        binding.llBackupContacts.setOnClickListener {
            startActivity(Intent(this, ContactsListActivity::class.java).apply {
                putExtra("listType", "backup")
                putExtra("fileCount" , allBackupCount)
            })
        }

//        fetchBackupFiles()
    }

    override fun onResume() {
        super.onResume()

        fetchInitialContactsData()

        contactsViewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        contactsViewModel.allContactsCount.observe(this) { count ->
            binding.tvAllContactCount.text = count.toString()
            allContactsCount = count
        }

        contactsViewModel.deletedContactsCount.observe(this) { count ->
            binding.tvDeletedContactCount.text = count.toString()
            allDeletedCount = count
        }

        contactsViewModel.backupFileCount.observe(this) { count ->
            allBackupCount = count
        }

    }

    private fun checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        } else {
            fetchInitialContactsData()
        }
    }
    private fun fetchInitialContactsData() {
        CoroutineScope(Dispatchers.Main).launch {
            contactsViewModel.fetchAllContacts(contentResolver)
            contactsViewModel.fetchDeletedContacts()
        }
    }
    private fun fetchBackupFiles() {
        CoroutineScope(Dispatchers.Main).launch {
            contactsViewModel.fetchBackupFiles(getExternalFilesDir(null)?.absolutePath)
        }
    }

}