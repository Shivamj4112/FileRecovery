package com.example.filerecovery.ui.adapter

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.filerecovery.R
import com.example.filerecovery.data.datasource.TopHeaderListener
import com.example.filerecovery.data.model.DateGroup
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.data.model.FileItem.Companion.formatSize
import com.example.filerecovery.data.model.FileListItem
import com.example.filerecovery.databinding.ItemDocumentBinding
import com.example.filerecovery.databinding.ItemFileBinding
import com.example.filerecovery.databinding.ItemTopHeaderBinding
import com.example.filerecovery.databinding.LayoutFilesBinding
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.Utils.formatDuration

class FileListDiffCallback(
    private val oldList: List<FileListItem>,
    private val newList: List<FileListItem>
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return when {
            oldItem is FileListItem.Header && newItem is FileListItem.Header ->
                oldItem.date == newItem.date

            oldItem is FileListItem.File && newItem is FileListItem.File ->
                oldItem.fileItem.path == newItem.fileItem.path

            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return oldItem == newItem
    }
}

class FileDetailsAdapter(
    private val fileType: String,
    private var isDateGrouped: Boolean,
    private val onItemClick: (FileItem) -> Unit,
    private val onSelectionChanged: (Set<FileItem>) -> Unit,
    private val topHeaderListener: TopHeaderListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var context: Context
    private val selectedFiles = mutableSetOf<FileItem>()
    private val groupSelectionStates = mutableMapOf<String, Boolean>()
    private var flattenedList = listOf<FileListItem>()
    private var isScanning = false

    override fun getItemViewType(position: Int): Int {
        return when (flattenedList[position]) {
            is FileListItem.TopHeader -> VIEW_TYPE_TOP_HEADER
            is FileListItem.Header -> VIEW_TYPE_HEADER
            is FileListItem.File -> when (fileType) {
                FileType.Photos.toString(), FileType.Videos.toString() -> VIEW_TYPE_PHOTO_VIDEO
                else -> VIEW_TYPE_AUDIO_DOCUMENT
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        return when (viewType) {
            VIEW_TYPE_TOP_HEADER -> {
                val binding =
                    ItemTopHeaderBinding.inflate(LayoutInflater.from(context), parent, false)
                TopHeaderViewHolder(binding)
            }

            VIEW_TYPE_HEADER -> {
                val binding =
                    LayoutFilesBinding.inflate(LayoutInflater.from(context), parent, false)
                DateHeaderViewHolder(binding)
            }

            VIEW_TYPE_PHOTO_VIDEO -> {
                val binding = ItemFileBinding.inflate(LayoutInflater.from(context), parent, false)
                FileViewHolder(binding)
            }

            VIEW_TYPE_AUDIO_DOCUMENT -> {
                val binding =
                    ItemDocumentBinding.inflate(LayoutInflater.from(context), parent, false)
                DocumentViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = flattenedList[position]) {
            is FileListItem.TopHeader ->  (holder as TopHeaderViewHolder).bind()

            is FileListItem.Header -> {
                (holder as DateHeaderViewHolder).bind(item)
            }
            is FileListItem.File -> when (holder) {
                is FileViewHolder -> holder.bind(item.fileItem)

                is DocumentViewHolder -> holder.bind(item.fileItem)
            }
        }
    }

    override fun getItemCount(): Int = flattenedList.size

    fun updateData(newDateGroups: List<DateGroup>, dateGrouped: Boolean) {
        isDateGrouped = dateGrouped
        val newFiles = newDateGroups.flatMap { it.files }.toSet()
        selectedFiles.retainAll { it in newFiles }

        val newFlattenedList = mutableListOf<FileListItem>()

        newFlattenedList.add(FileListItem.TopHeader)

        if (isDateGrouped) {
            newDateGroups.forEach { group ->
                newFlattenedList.add(
                    FileListItem.Header(
                        group.date,
                        groupSelectionStates[group.date] ?: false
                    )
                )
                newFlattenedList.addAll(group.files.map { FileListItem.File(it, group.date) })
            }
        } else {
            newFlattenedList.addAll(newDateGroups.first().files.map { FileListItem.File(it, "") })
        }

        val diffCallback = FileListDiffCallback(this.flattenedList, newFlattenedList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.flattenedList = newFlattenedList
        diffResult.dispatchUpdatesTo(this)
        updateGroupSelectionStates()
        onSelectionChanged(selectedFiles)
    }

    fun toggleSelectAll(select: Boolean) {
        selectedFiles.clear()
        if (select) {
            selectedFiles.addAll(
                flattenedList.filterIsInstance<FileListItem.File>().map { it.fileItem })
        }
        updateGroupSelectionStates()
        notifyDataSetChanged()
        onSelectionChanged(selectedFiles)
    }

    fun clearSelections() {
        selectedFiles.clear()
        updateGroupSelectionStates()
        notifyDataSetChanged()
        onSelectionChanged(selectedFiles)
    }

    private fun updateGroupSelectionStates() {
        groupSelectionStates.clear()
        if (isDateGrouped) {
            val groups = flattenedList.filterIsInstance<FileListItem.Header>().map { it.date }
            groups.forEach { date ->
                val groupFiles =
                    flattenedList.filterIsInstance<FileListItem.File>().filter { it.date == date }
                        .map { it.fileItem }
                groupSelectionStates[date] = groupFiles.all { it in selectedFiles }
            }
        }
    }

    fun getSelectedFiles(): Set<FileItem> = selectedFiles

    fun getItemAtPosition(position: Int): FileListItem = flattenedList[position]

    inner class TopHeaderViewHolder(val binding: ItemTopHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind() {
            val fileCount = flattenedList.count { it is FileListItem.File }
            val categoryName = fileType
            val titleText = context.getString(R.string.found_file_count, categoryName, fileCount.toString())
            binding.apply {
                llRescan.visibility = if (isScanning) View.GONE else View.VISIBLE
                progressBar.visibility = if (isScanning) View.VISIBLE else View.GONE

                tvFileCount.text = highlightNumber(titleText, fileCount, R.color.black)
                tvSubTitle.text =
                    context.getString(R.string.if_the_is_missing_rescan, categoryName.lowercase())

                val allSelected = selectedFiles.size == flattenedList.count { it is FileListItem.File }
                ivSelectAll.setImageResource(
                    if (allSelected) R.drawable.ic_tick_selected else R.drawable.ic_tick_unselected_gray
                )

                ivSelectAll.setOnClickListener {
                    selectedFiles.size != flattenedList.count { it is FileListItem.File }
                    topHeaderListener.onSelectAllClicked()
                }

                ivFilter.setOnClickListener {
                    topHeaderListener.onFilterClicked()
                }

                llRescan.setOnClickListener {
                    topHeaderListener.onRescanClicked()
                    setScanningState(true)
                }
            }
        }

        private fun highlightNumber(
            fullText: String,
            number: Int,
            textColor: Int
        ): SpannableString {
            val numberStr = number.toString()
            val spannable = SpannableString(fullText)
            val start = fullText.indexOf(numberStr)
            val end = start + numberStr.length
            if (start >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(context, textColor)),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            return spannable
        }
    }

    inner class DateHeaderViewHolder(private val binding: LayoutFilesBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: FileListItem.Header) {
            binding.tvDate.text = header.date
            binding.root.visibility = if (isDateGrouped) View.VISIBLE else View.GONE
            val groupFiles = flattenedList
                .filterIsInstance<FileListItem.File>()
                .filter { it.date == header.date }
                .map { it.fileItem }

            val isGroupSelected = groupFiles.all { it in selectedFiles }

            binding.ivSelectAll.setImageResource(
                if (isGroupSelected) R.drawable.ic_tick_selected else R.drawable.ic_tick_unselected_gray
            )

            binding.ivSelectAll.setOnClickListener {
                val newState = !isGroupSelected
                groupSelectionStates[header.date] = newState

                if (newState) {
                    selectedFiles.addAll(groupFiles)
                } else {
                    selectedFiles.removeAll(groupFiles)
                }
                notifyDataSetChanged()
                onSelectionChanged(selectedFiles)
            }
        }
    }

    inner class FileViewHolder(private val binding: ItemFileBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: FileItem) {
            binding.apply {
                Glide.with(context)
                    .load(file.path)
                    .into(ivFilesItem)

                ivSelect.setImageResource(
                    if (file in selectedFiles) R.drawable.ic_tick_selected else R.drawable.ic_tick_unselected_gray
                )

                ivPlay.visibility = if (fileType == FileType.Videos.toString()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                ivSelect.setOnClickListener {
                    if (file in selectedFiles) {
                        selectedFiles.remove(file)
                    } else {
                        selectedFiles.add(file)
                    }
                    updateGroupSelectionStates()
                    notifyDataSetChanged()
                    onSelectionChanged(selectedFiles)
                }
                root.setOnClickListener {
                    onItemClick(file)
                }
            }
        }
    }

    fun setScanningState(scanning: Boolean) {
        isScanning = scanning
        notifyItemChanged(0)
    }

    inner class DocumentViewHolder(private val binding: ItemDocumentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(file: FileItem) {
            binding.apply {
                tvFileName.isSelected = true
                tvFileName.text = file.name

                if (file.type == FileType.Audios) {
                    Glide.with(context)
                        .load(R.drawable.ic_play)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(ivDocumentIcons)
                    ivAudio.visibility = View.VISIBLE
                    tvSubTitle.text =
                        "${formatDuration(file.duration?.toInt() ?: 0)} ${formatSize(file.size)}"
                } else {
                    val extension = file.path.substringAfterLast(".", "").lowercase()
                    Glide.with(context)
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
                        )
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(ivDocumentIcons)

                    ivAudio.visibility = View.GONE
                    tvSubTitle.text = "${formatSize(file.size)}"
                }

                ivSelect.setImageResource(
                    if (file in selectedFiles) R.drawable.ic_tick_selected else R.drawable.ic_tick_unselected_gray
                )
                ivSelect.setOnClickListener {
                    if (file in selectedFiles) {
                        selectedFiles.remove(file)
                    } else {
                        selectedFiles.add(file)
                    }
                    updateGroupSelectionStates()
                    notifyDataSetChanged()
                    onSelectionChanged(selectedFiles)
                }
                root.setOnClickListener { onItemClick(file) }
            }
        }
    }

    companion object {
        const val VIEW_TYPE_TOP_HEADER = -1
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_PHOTO_VIDEO = 1
        const val VIEW_TYPE_AUDIO_DOCUMENT = 2
    }
}