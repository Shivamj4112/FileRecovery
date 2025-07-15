package com.example.filerecovery.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.filerecovery.R
import com.example.filerecovery.data.viewmodel.local.ContactsViewModel
import com.example.filerecovery.data.viewmodel.local.FileRecoveryViewModel
import com.example.filerecovery.databinding.ActivityMainBinding
import com.example.filerecovery.ui.activity.CapacityActivity
import com.example.filerecovery.ui.activity.FileDetailsActivity
import com.example.filerecovery.ui.components.MultiSegmentProgressBar
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.JunkFileManager
import com.example.filerecovery.utils.SegmentUtils
import com.example.filerecovery.utils.SharedPref
import com.example.filerecovery.utils.StorageInfo
import com.example.filerecovery.utils.ViewModelSingleton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.getValue
import kotlin.math.roundToInt

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    val fileRecoverModel: FileRecoveryViewModel by viewModels()
    val contactsViewModel: ContactsViewModel by viewModels()
    private val junkFileManager = JunkFileManager(this)

    private var pendingFileType: String? = null

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {

                setupStorage(this@MainActivity, binding)

                pendingFileType?.let { fileType ->
                    navigationWithFileType(
                        this@MainActivity,
                        ScanningActivity::class.java,
                        fileType
                    )
                }
                updateRecoveredFilesCount()
                junkFileManager.updateJunkFilesSize(binding.tvJunkFilesSize)
            }
        }
        pendingFileType = null
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        if (readGranted && writeGranted) {
            pendingFileType?.let { fileType ->
                navigationWithFileType(
                    this@MainActivity,
                    ScanningActivity::class.java,
                    fileType
                )
            }
            updateRecoveredFilesCount()
        }
        pendingFileType = null
        junkFileManager.updateJunkFilesSize(binding.tvJunkFilesSize)
    }

    private val contactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingFileType?.let {
                    navigationWithFileType(
                        this,
                        ContactRecoveryActivity::class.java,
                        it
                    )
                }
                SharedPref.contactPermissionDialog = 1
            } else {
                val count = SharedPref.contactPermissionDialog + 1
                SharedPref.contactPermissionDialog = count
                if (count >= 2 && !shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {

                    startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    })
                        .toString()
                }
            }
            pendingFileType = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        binding.apply {
            checkStoragePermission(action = {
                setupStorage(this@MainActivity, this@apply)
            })

            ViewModelSingleton.init(fileRecoverModel,contactsViewModel)

            val usedStorage = StorageInfo.getUsedStorage(this@MainActivity)
            val totalStorage = StorageInfo.getTotalStorage(this@MainActivity)

            tvUsedStorage.text = "$usedStorage ${getString(R.string.used)}"
            tvTotalStorage.text = "${getString(R.string.of)} $totalStorage"

            ivSettings.setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingActivity::class.java))
            }

            llStorageInfo.setOnClickListener {
                pendingFileType = null
                checkStoragePermissionAndNavigate(CapacityActivity::class.java)
            }

            llPhotosRecovery.setOnClickListener {
                checkStoragePermissionsAndNavigate(FileType.Photos.toString())
            }

            llVideosRecovery.setOnClickListener {
                checkStoragePermissionsAndNavigate(FileType.Videos.toString())
            }

            llAudiosRecovery.setOnClickListener {
                checkStoragePermissionsAndNavigate(FileType.Audios.toString())
            }

            llDocumentsRecovery.setOnClickListener {
                checkStoragePermissionsAndNavigate(FileType.Documents.toString())
            }

            llContactsRecovery.setOnClickListener {
                checkContactsPermissionAndNavigate(FileType.Contacts.toString())
            }
            llRecoveredFiles.setOnClickListener {
                startActivity(Intent(this@MainActivity, RecoveredFilesActivity::class.java))
            }

            llJunkRemover.setOnClickListener {
                checkStoragePermission {
                    junkFileManager.handleJunkRemoval(binding.tvJunkFilesSize)
                }
            }

        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {

        }
    }

    override fun onResume() {
        super.onResume()
        updateRecoveredFilesCount()
        junkFileManager.updateJunkFilesSize(binding.tvJunkFilesSize)
    }

    private fun updateRecoveredFilesCount() {
        CoroutineScope(Dispatchers.Main).launch {
            val count = withContext(Dispatchers.IO) {
                val recoveryDir = File(
                    Environment.getExternalStorageDirectory(),
                    "${getString(R.string.app_name)}/RecoveredFiles"
                )
                if (recoveryDir.exists() && recoveryDir.isDirectory) {
                    recoveryDir.listFiles()?.size ?: 0
                } else {
                    0
                }
            }
            binding.tvRecoveredFilesCount.text = getString(R.string._20_files, count.toString())
        }
    }

    fun checkStoragePermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${packageName}")
                }
                DialogUtil.showPermissionDialog(this) {
                    manageStoragePermissionLauncher.launch(intent)
                }
            } else {
                action()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                DialogUtil.showPermissionDialog(this) {
                    storagePermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            } else {
                action()
            }

        }
    }

    fun checkStoragePermissionsAndNavigate(fileType: String) {
        pendingFileType = fileType
        checkStoragePermission(action = {
            navigationWithFileType(this@MainActivity, ScanningActivity::class.java, fileType)
        })
    }

    fun checkStoragePermissionAndNavigate(endActivity: Class<*>) {
        checkStoragePermission(action = {
            startActivity(Intent(this@MainActivity, endActivity))
        })
    }


    private fun setupStorage(
        activity: MainActivity,
        binding: ActivityMainBinding
    ) {
        val usage = StorageInfo.getStorageUsageByCategory(this@MainActivity)
        val (total, used, _) = StorageInfo.getStorageStatsInGB(this@MainActivity)
        val categoryData = mapOf(
            FileType.Photos to usage[FileType.Photos]?.first!!,
            FileType.Videos to usage[FileType.Videos]?.first!!,
            FileType.Other to usage[FileType.Other]?.first!!
        )

        val totalUsedByCategories = categoryData.values.sum()
        val percentUsed = ((used / total) * 100)
        binding.tvStoragePercentage.text = "${StorageInfo.formatSize(percentUsed)}%"

        val segments = SegmentUtils.createSegmentsFromUsage(
            context = activity,
            categoryUsageMap = categoryData,
            totalStorageGB = total
        )

        binding.multiColorProgressBar.setSegments(segments)
    }

    fun checkContactsPermissionAndNavigate(fileType: String) {
        pendingFileType = fileType
        val permission = Manifest.permission.READ_CONTACTS

        when {
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                navigationWithFileType(this, ContactRecoveryActivity::class.java, fileType)
            }

            shouldShowRequestPermissionRationale(permission) -> {
                contactsPermissionLauncher.launch(permission)
            }

            SharedPref.contactPermissionDialog >= 2 -> {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                })
            }

            else -> contactsPermissionLauncher.launch(permission)
        }
    }


}
