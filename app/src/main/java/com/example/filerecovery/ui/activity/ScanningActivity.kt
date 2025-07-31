package com.example.filerecovery.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.filerecovery.R
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.data.viewmodel.local.FileRecoveryViewModel
import com.example.filerecovery.databinding.ActivityScanningBinding
import com.example.filerecovery.ui.activity.FileRecoveryActivity
import com.example.filerecovery.ui.activity.MainActivity
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.ViewModelSingleton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScanningActivity : BaseActivity() {

    private lateinit var binding: ActivityScanningBinding
    var fileRecoveryModel = ViewModelSingleton.fileRecoveryModel
    private var scanStartTime: Long = 0
    private val MIN_SCAN_TIME_MS = 5000L
    private var isScanning = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            binding.root.isClickable = true
        } else {
            Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isScanning) {
                showScanningToast()
            } else {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        val fileType = intent.getStringExtra("fileType")!!

        premiumBgSetup(fileType)

        binding.apply {

            setupBackground(fileType)
            setupContent(fileType)

            ivArrowBack.setOnClickListener {
                if (isScanning) {
                    showScanningToast()
                } else {
                    finish()
                }
            }

            root.setOnClickListener {
                llScanFiles.root.visibility = View.GONE
                root.isClickable = false
                llScanning.root.visibility = View.VISIBLE
                scanStartTime = System.currentTimeMillis()

                llScanning.progressBar.setProgress(0)

                fileRecoveryModel.scanDeletedFiles(fileType)

                isScanning = true
            }
        }

        fileRecoveryModel.scanProgress.observe(this) { progress ->

            binding.llScanning.tvCurrentPath.text = progress.currentPath
            binding.llScanning.progressBar.setProgress(progress.progress)
        }

        val mediator = MediatorLiveData<Pair<Boolean?, List<FileItem>?>>().apply {
            addSource(fileRecoveryModel.isLoading) { isLoading ->
                value = Pair(isLoading, fileRecoveryModel.files.value)
            }
            addSource(fileRecoveryModel.files) { files ->
                value = Pair(fileRecoveryModel.isLoading.value, files)
            }
        }

        mediator.observe(this) { (isLoading, files) ->
            if (!isLoading!! && isScanning && files != null) {
                val elapsedTime = System.currentTimeMillis() - scanStartTime
                val remainingTime = MIN_SCAN_TIME_MS - elapsedTime
                if (remainingTime > 0) {
                    lifecycleScope.launch {
                        delay(remainingTime)
                        binding.showScanComplete(files.size, fileType)
                        isScanning = false
                    }
                } else {
                    binding.showScanComplete(files.size, fileType)
                    isScanning = false
                }
            }
        }

        fileRecoveryModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                // Reset UI to initial scanning state
                setupInitialUI(fileType)
                isScanning = false
                scanStartTime = 0L
            }
        }
    }
//    override fun onResume() {
//        super.onResume()
//        // Reset UI when resuming (e.g., returning from FileRecoveryActivity)
//        val fileType = intent.getStringExtra("fileType")!!
//        setupInitialUI(fileType)
//        isScanning = false
//        scanStartTime = 0L
//    }

    private fun setupInitialUI(fileType: String) {
        binding.apply {
            setupBackground(fileType)
            setupContent(fileType)
            llScanFiles.root.visibility = View.VISIBLE
            llScanning.root.visibility = View.GONE
            llScanComplete.root.visibility = View.GONE
            root.isClickable = true
        }
    }

    private fun premiumBgSetup(fileType: String) {
        val bg = when (fileType) {
            FileType.Photos.toString() -> R.drawable.photo_card_premium_bg
            FileType.Videos.toString() -> R.drawable.videos_card_premium_bg
            FileType.Audios.toString() -> R.drawable.audios_card_premium_bg
            else -> R.drawable.documents_card_premium_bg
        }
        binding.llScanning.llPremiumCard.setBackgroundResource(bg)
    }

    private fun ActivityScanningBinding.setupBackground(fileType: String) {
        root.setBackgroundColor(getBackgroundColor(fileType))
        actionBar.setBackgroundColor(getBackgroundColor(fileType))
    }

    private fun ActivityScanningBinding.setupContent(fileType: String) {
        llScanFiles.apply {
            when (fileType) {
                FileType.Photos.toString() -> {
                    tvScanDisclaimer.text = getString(R.string.tap_to_scan_photos)
                    ivScanDisclaimer.setImageResource(R.drawable.ic_scan_photo)
                }

                FileType.Videos.toString() -> {
                    tvScanDisclaimer.text = getString(R.string.tap_to_scan_videos)
                    ivScanDisclaimer.setImageResource(R.drawable.ic_scan_videos)
                }

                FileType.Audios.toString() -> {
                    tvScanDisclaimer.text = getString(R.string.tap_to_scan_audios)
                    ivScanDisclaimer.setImageResource(R.drawable.ic_scan_audio)
                }

                FileType.Documents.toString() -> {
                    tvScanDisclaimer.text = getString(R.string.tap_to_scan_documents)
                    ivScanDisclaimer.setImageResource(R.drawable.ic_scan_documents)
                }
            }
            loadGif(fileType)
        }

    }

    private fun ActivityScanningBinding.showScanComplete(count: Int, fileType: String) {
        llScanFiles.root.visibility = View.GONE
        llScanFiles.root.isClickable = false
        llScanning.root.visibility = View.GONE
        llScanComplete.root.visibility = View.VISIBLE
        isScanning = false

        val lowerCaseText = getFileTypeName(fileType).lowercase()
        val text = getString(R.string.found_recovery, count.toString(), lowerCaseText)
        llScanComplete.tvFilesFound.text = highlightNumber(text, count)

        scanStartTime = 0L
        binding.llScanning.progressBar.setProgress(0)

        llScanComplete.btViewFiles.setOnClickListener {
            startActivity(Intent(this@ScanningActivity, FileRecoveryActivity::class.java).putExtra("fileType", fileType))
            finish()
        }
    }


    fun loadGif(type: String) {

        val animation = when (type) {
            FileType.Photos.toString() -> R.drawable.ic_scanning_photo_lottie
            FileType.Videos.toString() -> R.drawable.ic_scanning_videos_lottie
            FileType.Audios.toString() -> R.drawable.ic_scanning_audio_lottie
            FileType.Documents.toString() -> R.drawable.ic_scanning_document_lottie
            else -> R.drawable.ic_scanning_photo_lottie
        }

        Glide.with(this@ScanningActivity)
            .asGif()
            .load(animation)
            .into(binding.llScanning.ivScanning)
    }
}