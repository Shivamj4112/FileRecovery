package com.example.filerecovery.ui.activity

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.filerecovery.R
import com.example.filerecovery.data.datasource.DateGroupItem
import com.example.filerecovery.data.model.Contact
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.databinding.ActivityProgressBinding
import com.example.filerecovery.utils.ViewModelSingleton
import com.example.filerecovery.utils.ViewModelSingleton.contactsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProgressActivity : BaseActivity() {

    private lateinit var binding: ActivityProgressBinding
    private val fileRecoveryModel = ViewModelSingleton.fileRecoveryModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val operationType = intent.getStringExtra("operationType") ?: "recovery"
        val fileType = intent.getStringExtra("fileType")
        val selectedFiles = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("selectedFiles", FileItem::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("selectedFiles") ?: emptyList()
        }
        val selectedContacts = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("selectedContacts", Contact::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra("selectedContacts") ?: emptyList()
        }
        val fileName = intent.getStringExtra("fileName")
        val storagePath = intent.getStringExtra("storagePath")

        binding.apply {
            ivArrowBack.setOnClickListener {
                finish()
            }

            ivHome.setOnClickListener {
                val intent = Intent(this@ProgressActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                val options = ActivityOptions.makeCustomAnimation(
                    this@ProgressActivity,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )

                startActivity(intent, options.toBundle())
                finish()
            }


            btContinue.setOnClickListener {
                val intent = Intent(this@ProgressActivity, FileDetailsActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("fromActivity", true)
                }
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this@ProgressActivity,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                startActivity(intent, options.toBundle())

                finish()
            }

            if (operationType == "backup" || operationType == "recovery") {
                tvfileSuccess.text = getString(R.string.backup_successful)
                tvFilesFound.text = getString(R.string.has_been_save_to_the_device, "Contacts")
            }

            Glide.with(this@ProgressActivity).load(R.drawable.ic_progress_bar).into(llProgressLayout.ivProgress)



            btViewFiles.setOnClickListener {
                if (operationType == "backup") {
                    startActivity(Intent(this@ProgressActivity, ContactsListActivity::class.java).apply {
                        putExtra("listType", "backup")
                    })
                }else {
                    navigationWithFileType(
                        this@ProgressActivity,
                        RecoveredFilesActivity::class.java,
                        fileType!!
                    )
                }
                finish()
            }

            llProgressLayout.root.visibility = View.VISIBLE
            tvFilesFound.visibility = View.GONE
            btContinue.visibility = View.GONE
            btViewFiles.visibility = View.GONE

            if (operationType == "backup") {
                backupContacts(selectedContacts, fileName, storagePath)
            } else {
                recoverFiles(selectedFiles, fileType ?: "")
            }
        }

    }
    private fun backupContacts(contacts: List<Contact>, fileName: String?, storagePath: String?) {
        if (contacts.isEmpty() || fileName.isNullOrBlank()) {
            showSuccessLayout("contacts")
            return
        }

        binding.llProgressLayout.tvProgressText.text = "0/${contacts.size}"
        binding.llProgressLayout.tvProgressType.text = "Contacts"

        CoroutineScope(Dispatchers.Main).launch {
            contacts.forEachIndexed { index, contact ->
                binding.llProgressLayout.tvProgressText.text = "${index + 1}/${contacts.size}"
                delay(500)
            }

            val result = contactsViewModel.backupSelectedContacts(fileName, storagePath)
            showSuccessLayout("contacts")
            Toast.makeText(this@ProgressActivity, result, Toast.LENGTH_SHORT).show()
        }
    }

    private fun recoverFiles(files: List<FileItem>,fileType: String) {
        if (files.isEmpty()) {
            showSuccessLayout(fileType)
            return
        }

        binding.llProgressLayout.tvProgressText.text = "0/${files.size}"
        binding.llProgressLayout.tvProgressType.text = getFileTypeName(fileType)

        CoroutineScope(Dispatchers.Main).launch {
            files.forEachIndexed { index, file ->
                binding.llProgressLayout.tvProgressText.text = "${index + 1}/${files.size}"

                fileRecoveryModel.recoverFile(file)
                delay(500)
            }
            showSuccessLayout(fileType)
        }
    }

    private fun showSuccessLayout(fileType:String) {
        binding.apply {
            llProgressLayout.root.visibility = View.GONE
            tvFilesFound.visibility = View.VISIBLE
            btContinue.visibility = if (fileType == "contacts") View.GONE else View.VISIBLE
            btViewFiles.visibility = View.VISIBLE
            if (fileType == "contacts") {
                tvfileSuccess.text = getString(R.string.backup_successful)
                tvFilesFound.text = getString(R.string.has_been_save_to_the_device, "Contacts")
            }
            else{
                tvFilesFound.text = getString(R.string.has_been_save_to_the_device, getFileTypeName(fileType))
            }
            showToast(getString(R.string.file_recovered_successfully))
        }
    }
}