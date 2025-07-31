package com.example.filerecovery.ui.activity

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.filerecovery.R
import com.example.filerecovery.data.datasource.TopHeaderListener
import com.example.filerecovery.data.model.DateGroup
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.data.model.FileListItem
import com.example.filerecovery.data.model.RecoveryCategory
import com.example.filerecovery.databinding.ActivityFileDetailsBinding
import com.example.filerecovery.ui.adapter.FileDetailsAdapter
import com.example.filerecovery.ui.fragment.FilterBottomSheetDialog
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.ViewModelSingleton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class FileDetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityFileDetailsBinding
    var fileRecoveryModel = ViewModelSingleton.fileRecoveryModel
    private lateinit var adapter: FileDetailsAdapter
    private var originalFiles: List<FileItem> = emptyList()
    private var currentFilter: FilterBottomSheetDialog.FilterOption =
        FilterBottomSheetDialog.FilterOption.ALL_DAYS
    private var currentSort: FilterBottomSheetDialog.SortOption =
        FilterBottomSheetDialog.SortOption.NEWEST
    private var currentDuration: FilterBottomSheetDialog.DurationOption =
        FilterBottomSheetDialog.DurationOption.ALL_DURATIONS
    private var currentSize: FilterBottomSheetDialog.SizeOption =
        FilterBottomSheetDialog.SizeOption.ALL_SIZES

    private val previewResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val deletedFile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.getParcelableExtra("deletedFile", FileItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.getParcelableExtra("deletedFile")
            }
            deletedFile?.let { fileItem ->
                originalFiles = originalFiles.filter { it != fileItem }
                updateFileList(intent.getStringExtra("fileType")!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFileDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fileCount = intent.getStringExtra("fileCount")
        val fileType = intent.getStringExtra("fileType")!!


        savedInstanceState?.let {
            currentFilter = FilterBottomSheetDialog.FilterOption.valueOf(
                it.getString("currentFilter", FilterBottomSheetDialog.FilterOption.ALL_DAYS.name)
            )
            currentSort = FilterBottomSheetDialog.SortOption.valueOf(
                it.getString("currentSort", FilterBottomSheetDialog.SortOption.NEWEST.name)
            )
            currentDuration = FilterBottomSheetDialog.DurationOption.valueOf(
                it.getString(
                    "currentDuration",
                    FilterBottomSheetDialog.DurationOption.ALL_DURATIONS.name
                )
            )
            currentSize = FilterBottomSheetDialog.SizeOption.valueOf(
                it.getString(
                    "currentSize",
                    FilterBottomSheetDialog.SizeOption.ALL_SIZES.name
                )
            )
        }

        val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("category", RecoveryCategory::class.java)
        } else {
            intent.getParcelableExtra("category")
        }

        binding.apply {
            val categoryName = getFileTypeName(fileType)

            ivArrowBack.setOnClickListener { finish() }

            tvTitleName.text = getString(R.string.category_recovery, categoryName)
            btRecover.setOnClickListener { recoverSelectedFiles(fileType) }
            updateRecoverButtonVisibility(emptySet())
        }

        originalFiles = category?.files!!
        setupRecyclerView(fileType.toString())
        updateFileList(fileType)
        observeViewModel(category, fileType)

    }

    override fun onPause() {
        super.onPause()
        Glide.with(this).pauseRequests()
    }

    override fun onResume() {
        super.onResume()
        Glide.with(this).resumeRequests()
    }

    private fun observeViewModel(category: RecoveryCategory, fileType: String) {
        fileRecoveryModel.scanProgress.observe(this@FileDetailsActivity) { progress ->
            lifecycleScope.launch(Dispatchers.Main) {
            }
        }

        fileRecoveryModel.filesCategories.observe(this@FileDetailsActivity) { categories ->
            lifecycleScope.launch(Dispatchers.Main) {
                val updatedCategory = categories.find { it.categoryName == category.categoryName }

                originalFiles = updatedCategory?.files ?: emptyList()
                updateFileList(fileType)

                adapter.setScanningState(false)
            }
        }
    }

    private fun rescanFiles(fileType: String) {
        fileRecoveryModel.scanDeletedFiles(fileType)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentFilter", currentFilter.name)
        outState.putString("currentSort", currentSort.name)
        outState.putString("currentDuration", currentDuration.name)
        outState.putString("currentSize", currentSize.name)
    }

    private fun setupRecyclerView(fileType: String) {
        adapter = FileDetailsAdapter(
            fileType = fileType,
            isDateGrouped = isDateGrouped(),
            onItemClick = {
                val intent = Intent(this, PreviewActivity::class.java).putExtra("fileItem", it)
                previewResultLauncher.launch(intent)
            },
            onSelectionChanged = { selectedFiles ->
                binding.updateRecoverButtonVisibility(selectedFiles)
            },
            topHeaderListener = object : TopHeaderListener {
                override fun onSelectAllClicked() {
                    toggleSelectAll()
                }

                override fun onRescanClicked() {
                    rescanFiles(fileType)
                }

                override fun onFilterClicked() {
                    showFilterBottomSheet(fileType)
                }
            }
        )

        binding.rvPhotos.layoutManager = when (fileType) {
            FileType.Photos.toString(), FileType.Videos.toString() -> {
                val gridLayoutManager = GridLayoutManager(this, 3)
                gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return when (adapter.getItemViewType(position)) {
                            FileDetailsAdapter.VIEW_TYPE_HEADER, FileDetailsAdapter.VIEW_TYPE_TOP_HEADER  -> 3
                            else -> 1
                        }
                    }
                }
                gridLayoutManager
            }

            else -> LinearLayoutManager(this)
        }


        binding.rvPhotos.adapter = adapter

        binding.rvPhotos.setRecycledViewPool(RecyclerView.RecycledViewPool().apply {
            setMaxRecycledViews(FileDetailsAdapter.VIEW_TYPE_HEADER, 10)
            setMaxRecycledViews(FileDetailsAdapter.VIEW_TYPE_PHOTO_VIDEO, 30)
            setMaxRecycledViews(FileDetailsAdapter.VIEW_TYPE_AUDIO_DOCUMENT, 30)
        })
    }

    private fun ActivityFileDetailsBinding.updateRecoverButtonVisibility(selectedFiles: Set<FileItem>) {
        btRecover.visibility = if (selectedFiles.isNotEmpty()) View.VISIBLE else View.GONE
        btRecover.text = getString(R.string.recover_with_count, selectedFiles.size.toString())
    }

    private fun toggleSelectAll() {
        val allSelected = adapter.getSelectedFiles().size == originalFiles.size
        adapter.toggleSelectAll(!allSelected)
        binding.updateRecoverButtonVisibility(adapter.getSelectedFiles())
    }

    private fun recoverSelectedFiles(fileType: String) {
        val selectedFiles = adapter.getSelectedFiles()
        val intent = Intent(this, ProgressActivity::class.java).apply {
            putParcelableArrayListExtra("selectedFiles", ArrayList(selectedFiles))
            putExtra("fileType", fileType)
        }

        startActivity(intent)
        adapter.clearSelections()
        binding.updateRecoverButtonVisibility(emptySet())
        showToast(getString(R.string.file_recovered_successfully))
    }

    private fun updateFileList(fileType: String) {
//        binding.progressBarContent.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val filteredFiles = filterFiles(originalFiles, fileType)
            val sortedFiles = sortFiles(filteredFiles)
            val dateGroups = if (isDateGrouped()) {
                groupFilesByDate(sortedFiles)
            } else {
                listOf(DateGroup("", sortedFiles))
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(dateGroups, isDateGrouped())
                binding.llEmptyLayout.visibility =
                    if (sortedFiles.isEmpty()) View.VISIBLE else View.GONE
//                binding.progressBarContent.visibility = View.GONE

                val image = when (fileType) {
                    FileType.Photos.toString() -> getDrawable(R.drawable.ic_empty_photos)
                    FileType.Audios.toString() -> getDrawable(R.drawable.ic_empty_audio)
                    FileType.Videos.toString() -> getDrawable(R.drawable.ic_empty_videos)
                    FileType.Documents.toString() -> getDrawable(R.drawable.ic_empty_document)
                    else -> getDrawable(R.drawable.ic_empty_photos)
                }

                val text = when (fileType) {
                    FileType.Photos.toString() -> getString(R.string.no_photos_found)
                    FileType.Audios.toString() -> getString(R.string.no_audio_found)
                    FileType.Videos.toString() -> getString(R.string.no_videos_found)
                    FileType.Documents.toString() -> getString(R.string.no_documents_found)
                    else -> getString(R.string.no_photos_found)
                }

                binding.tvEmpty.text = text
                Glide.with(this@FileDetailsActivity)
                    .load(image)
                    .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                    .into(binding.ivEmpty)
            }
        }
    }

    private fun isDateGrouped(): Boolean {
        return currentSort == FilterBottomSheetDialog.SortOption.NEWEST ||
                currentSort == FilterBottomSheetDialog.SortOption.OLDEST
    }

    private fun filterFiles(files: List<FileItem>, fileType: String): List<FileItem> {
        val calendar = Calendar.getInstance()
        val filteredByDate = when (currentFilter) {
            FilterBottomSheetDialog.FilterOption.ALL_DAYS -> files
            FilterBottomSheetDialog.FilterOption.WITHIN_7_DAYS -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                files.filter { it.lastModified >= calendar.timeInMillis }
            }

            FilterBottomSheetDialog.FilterOption.WITHIN_1_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
                files.filter { it.lastModified >= calendar.timeInMillis }
            }

            FilterBottomSheetDialog.FilterOption.WITHIN_6_MONTHS -> {
                calendar.add(Calendar.MONTH, -6)
                files.filter { it.lastModified >= calendar.timeInMillis }
            }
        }

        val filteredByDuration = if (fileType == "Audios") {
            filteredByDate.filter { file ->
                val duration = file.duration
                when (currentDuration) {
                    FilterBottomSheetDialog.DurationOption.ALL_DURATIONS -> true
                    FilterBottomSheetDialog.DurationOption.DURATION_0_5 -> duration != null && duration <= 5 * 60 * 1000
                    FilterBottomSheetDialog.DurationOption.DURATION_5_20 -> duration != null && duration in (5 * 60 * 1000 + 1)..(20 * 60 * 1000)
                    FilterBottomSheetDialog.DurationOption.DURATION_20_60 -> duration != null && duration in (20 * 60 * 1000 + 1)..(60 * 60 * 1000)
                    FilterBottomSheetDialog.DurationOption.DURATION_OVER_60 -> duration != null && duration > 60 * 60 * 1000
                }
            }
        } else {
            filteredByDate
        }
        return if (fileType == "Documents") {
            filteredByDuration.filter { file ->
                val size = file.size
                when (currentSize) {
                    FilterBottomSheetDialog.SizeOption.ALL_SIZES -> true
                    FilterBottomSheetDialog.SizeOption.SIZE_0_500KB -> size <= 500 * 1024
                    FilterBottomSheetDialog.SizeOption.SIZE_500KB_1MB -> size in (500 * 1024 + 1)..(1024 * 1024)
                    FilterBottomSheetDialog.SizeOption.SIZE_1MB_10MB -> size in (1024 * 1024 + 1)..(10 * 1024 * 1024)
                    FilterBottomSheetDialog.SizeOption.SIZE_OVER_10MB -> size > 10 * 1024 * 1024
                }
            }
        } else {
            filteredByDuration
        }
    }

    private fun sortFiles(files: List<FileItem>): List<FileItem> {
        return when (currentSort) {
            FilterBottomSheetDialog.SortOption.LARGEST -> files.sortedByDescending { it.size }
            FilterBottomSheetDialog.SortOption.SMALLEST -> files.sortedBy { it.size }
            FilterBottomSheetDialog.SortOption.NEWEST -> files.sortedByDescending { it.lastModified }
            FilterBottomSheetDialog.SortOption.OLDEST -> files.sortedBy { it.lastModified }
        }
    }

    private fun groupFilesByDate(files: List<FileItem>): List<DateGroup> {
        if (!isDateGrouped()) return listOf(DateGroup("", files))
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        val grouped = files.groupBy { file ->
            val fileDate = Date(file.lastModified)
            val fileCalendar = Calendar.getInstance().apply { time = fileDate }
            when {
                isSameDay(fileCalendar, today) -> "Today"
                isSameDay(fileCalendar, yesterday) -> "Yesterday"
                else -> dateFormat.format(fileDate)
            }
        }

        val sortedGroups = grouped.map { (date, fileList) ->
            DateGroup(date, fileList)
        }.sortedByDescending { group ->
            when (group.date) {
                "Today" -> Long.MAX_VALUE
                "Yesterday" -> Long.MAX_VALUE - 1
                else -> dateFormat.parse(group.date)?.time ?: 0L
            }
        }

        return when (currentSort) {
            FilterBottomSheetDialog.SortOption.OLDEST -> sortedGroups.reversed()
            else -> sortedGroups
        }
    }


    fun showFilterBottomSheet(fileType: String) {
        val bottomSheet = FilterBottomSheetDialog.newInstance(
            currentFilter,
            currentSort,
            currentDuration,
            currentSize,
            fileType
        )
        bottomSheet.setOnFilterSelected { filter, sort, duration, size ->
            currentFilter = filter
            currentSort = sort
            currentDuration = duration
            currentSize = size
            updateFileList(fileType)
        }
        bottomSheet.show(supportFragmentManager, "FilterBottomSheet")
    }
}