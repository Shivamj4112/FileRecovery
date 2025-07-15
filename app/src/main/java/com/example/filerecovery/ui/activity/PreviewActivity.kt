package com.example.filerecovery.ui.activity

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.filerecovery.R
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.data.model.FileItem.Companion.formatSize
import com.example.filerecovery.databinding.ActivityPreviewBinding
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.FileUtils.getMimeType
import com.example.filerecovery.utils.FileUtils.shareFile
import com.example.filerecovery.utils.Utils.formatDuration
import com.example.filerecovery.utils.ViewModelSingleton
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class PreviewActivity : BaseActivity() {

    private lateinit var binding: ActivityPreviewBinding
    var fileRecoveryModel = ViewModelSingleton.fileRecoveryModel
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isMediaPlaying = false
    private var isUserSeeking = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fileItem: FileItem? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("fileItem", FileItem::class.java)
        } else {
            intent.getParcelableExtra("fileItem")
        }

        val hasRecovered = intent.getBooleanExtra("hasRecovered", false)

        fileItem?.let { item ->
            binding.apply {
                ivArrowBack.setOnClickListener {
                    finish()
                }

                tvFileName.isSelected = true

                if (hasRecovered) {
                    llPreview1.root.visibility = View.GONE
                    llPreview2.root.visibility = View.VISIBLE
                    tvTitleName.text = getString(R.string.recovered_files)
                    with(llPreview2) {
                        ivShare.setOnClickListener { shareFile(item) }
                        ivDelete.setOnClickListener { showDeleteConfirmationDialog(item) }
                    }
                } else {
                    llPreview1.root.visibility = View.VISIBLE
                    llPreview2.root.visibility = View.GONE
                    tvTitleName.text = getString(R.string.preview)
                    with(llPreview1) {
                        ivShare.setOnClickListener { shareFile(item) }
                        ivDelete.setOnClickListener { showDeleteConfirmationDialog(item) }
                        btnRecover.setOnClickListener {
                            val intent =
                                Intent(this@PreviewActivity, ProgressActivity::class.java).apply {
                                    putParcelableArrayListExtra(
                                        "selectedFiles",
                                        arrayListOf(fileItem)
                                    )
                                    putExtra("fileType", item.type.toString())
                                }
                            startActivity(intent)

                        }
                    }
                }

                when (item.type) {
                    FileType.Photos -> {
                        rlMediaLayout.visibility = View.GONE
                        vvPreviewMedia.visibility = View.GONE
                        ivAudioIcons.visibility = View.GONE
                        ivPreviewMedia.visibility = View.VISIBLE
                        Glide.with(this@PreviewActivity).load(item.path).into(ivPreviewMedia)
                        llResolutionLayout.visibility = View.VISIBLE
                        tvResolution.text = item.resolution?.replace("x","*",false)
                        ivDocumentIcons.visibility = View.GONE
                    }

                    FileType.Videos -> {
                        rlMediaLayout.visibility = View.VISIBLE
                        vvPreviewMedia.visibility = View.VISIBLE
                        ivPreviewMedia.visibility = View.GONE
                        ivAudioIcons.visibility = View.GONE
                        btnPlayPause.visibility = View.VISIBLE
//                        setupMediaPlayer(item.path, true)
                        btnPlayPause.setOnClickListener {
                            startActivity(
                                Intent(this@PreviewActivity, VideoPlayActivity::class.java).apply {
                                    putExtra("fileItem", item)
                                    putExtra("hasRecovered", true)
                                }
                            )
                        }
                        llResolutionLayout.visibility = View.GONE
                        ivDocumentIcons.visibility = View.GONE
                    }

                    FileType.Audios -> {
                        rlMediaLayout.visibility = View.VISIBLE
                        vvPreviewMedia.visibility = View.GONE
                        ivPreviewMedia.visibility = View.GONE
//                        setupMediaPlayer(item.path, true)
                        llResolutionLayout.visibility = View.GONE
                        ivDocumentIcons.visibility = View.GONE
                        ivAudioIcons.visibility = View.VISIBLE
                        ivAudioView.visibility = View.VISIBLE
                        ivAudioView.setImageResource(R.drawable.audio_background)
                        Glide.with(this@PreviewActivity)
                            .load(R.drawable.ic_preview_audio)
                            .into(ivAudioIcons)
                        setupMediaPlayer(item.path, false)
                    }

                    FileType.Documents -> {
                        binding.rlMediaLayout.visibility = View.GONE
                        ivAudioIcons.visibility = View.GONE
                        vvPreviewMedia.visibility = View.GONE
                        ivPreviewMedia.visibility = View.GONE
                        ivDocumentMedia.visibility = View.VISIBLE
                        ivDocumentMedia.setImageResource(R.drawable.document_background)
                        ivDocumentIcons.visibility = View.VISIBLE
                        ivOpenDocument.visibility = View.VISIBLE

                        val extension = fileItem.path.substringAfterLast(".", "").lowercase()
                        Glide.with(this@PreviewActivity)
                            .load(
                                when (extension) {
                                    "pdf" -> R.drawable.ic_pdf
                                    "xls", "xlsx" -> R.drawable.ic_xls
                                    "rtf" -> R.drawable.ic_rtf
                                    "txt" -> R.drawable.ic_txt
                                    "ppt", "pptx" -> R.drawable.ic_ppt
                                    "apk" -> R.drawable.ic_apk
                                    else -> R.drawable.ic_document
                                }
                            ).into(ivDocumentIcons)
                        llResolutionLayout.visibility = View.GONE

                        ivOpenDocument.setOnClickListener { openDocument(item) }
                    }

                    else -> {}
                }


                tvFileName.text = fileItem.name
                tvFilePath.text = fileItem.path
                tvSize.text = formatSize(fileItem.size)
                tvDate.text = formatModifiedDate(fileItem.lastModified)


            }
        } ?: run {
            showToast(getString(R.string.file_not_found))
            finish()
        }

    }

    private fun openDocument(fileItem: FileItem) {
        val file = File(fileItem.path)
        if (!file.exists()) {
            showToast(getString(R.string.file_does_not_exist))
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(fileItem))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            // Verify if there's an app to handle the intent
            if (openIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(openIntent, "Open File"))
            } else {
                showToast("No application found to open this file type")
            }
        } catch (e: Exception) {
            showToast("Error opening file: ${e.message}")
        }
    }

    private fun setupMediaPlayer(filePath: String, isVideo: Boolean) {

        if (isVideo) {
            binding.vvPreviewMedia.setVideoPath(filePath)
            binding.vvPreviewMedia.setOnPreparedListener { mp ->
                mediaPlayer = mp
                binding.seekBarMedia.max = mp.duration
                binding.tvTotalDuration.text =
                    getString(R.string.total_duration, formatDuration(mp.duration))
                binding.tvCurrentDuration.text = "00:00"
            }
        } else {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                binding.tvCurrentDuration.setTextColor(getColor(R.color.black))
                binding.tvTotalDuration.setTextColor(getColor(R.color.black))
                binding.seekBarMedia.max = duration
                binding.tvTotalDuration.text =
                    getString(R.string.total_duration, formatDuration(duration))
                binding.tvCurrentDuration.text = "00:00"
            }
        }

        binding.btnPlayPause.setOnClickListener {
            if (isMediaPlaying) {
                if (isVideo) {
                    binding.vvPreviewMedia.pause()
                } else {
                    mediaPlayer?.pause()
                }
                isMediaPlaying = false
                binding.btnPlayPause.visibility = View.VISIBLE
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                handler.removeCallbacksAndMessages(null)
            } else {
                if (isVideo) {
                    binding.vvPreviewMedia.start()
                } else {
                    mediaPlayer?.start()
                }
                isMediaPlaying = true
                binding.btnPlayPause.visibility = View.INVISIBLE
                updateSeekBar()
            }
        }


        binding.seekBarMedia.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentDuration.text = formatDuration(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = true
                handler.removeCallbacksAndMessages(null)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserSeeking = false
                seekBar?.let {
                    val progress = it.progress
                    if (isVideo) {
                        binding.vvPreviewMedia.seekTo(progress)
                    } else {
                        mediaPlayer?.seekTo(progress)
                    }
                    handler.postDelayed({
                        val currentPos = if (isVideo) {
                            binding.vvPreviewMedia.currentPosition
                        } else {
                            mediaPlayer?.currentPosition ?: 0
                        }
                        binding.tvCurrentDuration.text = formatDuration(currentPos)
                        binding.seekBarMedia.progress = currentPos
                        if (isMediaPlaying) {
                            updateSeekBar()
                        }
                    }, 300)
                }
            }
        })
    }

    private fun updateSeekBar() {
        if (isMediaPlaying && !isUserSeeking) {
            val currentPos = mediaPlayer?.currentPosition ?: binding.vvPreviewMedia.currentPosition
            binding.seekBarMedia.progress = currentPos
            binding.tvCurrentDuration.text = formatDuration(currentPos)
        }
        if (isMediaPlaying) {
            handler.postDelayed({ updateSeekBar() }, 1000)
        }
    }

    private fun showDeleteConfirmationDialog(fileItem: FileItem) {
        DialogUtil.showDeleteDialog(this) { deleteFile(fileItem) }
    }

    private fun deleteFile(fileItem: FileItem) {
        val file = File(fileItem.path)
        if (file.exists() && file.delete()) {
            Toast.makeText(this, getString(R.string.file_deleted_successfully), Toast.LENGTH_SHORT).show()
            val resultIntent = Intent().apply {
                putExtra("deletedFile", fileItem)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            showToast(getString(R.string.failed_to_delete_file))
        }
    }

    override fun onPause() {
        super.onPause()
        if (isMediaPlaying) {
            if (binding.vvPreviewMedia.isPlaying) {
                binding.vvPreviewMedia.pause()
            }
            mediaPlayer?.pause()
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            isMediaPlaying = false
            handler.removeCallbacksAndMessages(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}

