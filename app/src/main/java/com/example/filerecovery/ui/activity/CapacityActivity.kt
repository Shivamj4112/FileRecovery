package com.example.filerecovery.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.filerecovery.R
import com.example.filerecovery.databinding.ActivityCapacityBinding
import com.example.filerecovery.ui.components.MultiSegmentProgressBar
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.SegmentUtils
import com.example.filerecovery.utils.StorageInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@AndroidEntryPoint
class CapacityActivity : BaseActivity() {

    private lateinit var binding: ActivityCapacityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCapacityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        val usedStorage = StorageInfo.getUsedStorageInGB(this@CapacityActivity)
        val totalStorage = StorageInfo.getTotalStorageInGB(this@CapacityActivity)



        binding.apply {

            ivArrowBack.setOnClickListener { finish() }

            tvUsedStorage.text = getString(R.string.gb_without_space, usedStorage)
            tvTotalStorage.text =
                getString(R.string.used_of, getString(R.string.gb_without_space, totalStorage))

            setupStorage(this@CapacityActivity, this@apply)


            lifecycleScope.launch(Dispatchers.IO) {
                val usage = StorageInfo.getStorageUsageByCategory(this@CapacityActivity)
                withContext(Dispatchers.Main) {
                    tvPhotosStorage.text =
                        getString(R.string.gb_with_space, usage[FileType.Photos]?.first.toString())
                    tvVideosStorage.text =
                        getString(R.string.gb_with_space, usage[FileType.Videos]?.first.toString())
                    tvAudioStorage.text =
                        getString(R.string.gb_with_space, usage[FileType.Audios]?.first.toString())
                    tvDocumentsStorage.text = getString(
                        R.string.gb_with_space,
                        usage[FileType.Documents]?.first.toString()
                    )
                    tvOtherStorage.text =
                        getString(R.string.gb_with_space, usage[FileType.Other]?.first.toString())

                    tvPhotosCount.text =
                        getString(R.string._1024_files, usage[FileType.Photos]?.second.toString())
                    tvVideosCount.text =
                        getString(R.string._1024_files, usage[FileType.Videos]?.second.toString())
                    tvAudiosCount.text =
                        getString(R.string._1024_files, usage[FileType.Audios]?.second.toString())
                    tvDocumentCount.text = getString(
                        R.string._1024_files,
                        usage[FileType.Documents]?.second.toString()
                    )
                    tvOtherCount.text =
                        getString(R.string._1024_files, usage[FileType.Other]?.second.toString())
                }
            }

        }
    }

    private fun setupStorage(
        activity: CapacityActivity,
        binding: ActivityCapacityBinding
    ) {
        val usage = StorageInfo.getStorageUsageByCategory(this)
        val total = StorageInfo.getTotalStorageInGB(this).toDouble()

        val categoryData = mapOf(
            FileType.Photos to usage[FileType.Photos]?.first!!,
            FileType.Videos to usage[FileType.Videos]?.first!!,
            FileType.Audios to usage[FileType.Audios]?.first!!,
            FileType.Documents to usage[FileType.Documents]?.first!!,
            FileType.Other to usage[FileType.Other]?.first!!
        )

        val segments = SegmentUtils.createSegmentsFromUsage(
            context = activity,
            categoryUsageMap = categoryData,
            totalStorageGB = total
        )

        binding.pbStorageInfo.setSegments(segments)


        fun getPercent(gb: Double): Float {
            val percent = ((gb / total) * 100).toFloat()
            return if (percent > 0f && percent < 2f) 2f else percent
        }


        fun setCategoryProgress(
            progressBar: MultiSegmentProgressBar,
            fileType: FileType
        ) {
            val percent = getPercent(categoryData[fileType] ?: 0.0).toFloat()
            val color = getBackgroundColor(fileType.toString())
            progressBar.setSegments(listOf(MultiSegmentProgressBar.Segment(percent, color)))
        }

        // Apply to all categories
        setCategoryProgress(binding.progressPhotos, FileType.Photos)
        setCategoryProgress(binding.progressVideos, FileType.Videos)
        setCategoryProgress(binding.progressAudios, FileType.Audios)
        setCategoryProgress(binding.progressDocuments, FileType.Documents)
        setCategoryProgress(binding.progressOther, FileType.Other)

    }
}