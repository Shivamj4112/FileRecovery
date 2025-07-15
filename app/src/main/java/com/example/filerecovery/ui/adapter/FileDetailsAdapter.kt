package com.example.filerecovery.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.filerecovery.R
import com.example.filerecovery.data.model.DateGroup
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.data.model.FileItem.Companion.formatSize
import com.example.filerecovery.data.model.FileListItem
import com.example.filerecovery.databinding.ItemDocumentBinding
import com.example.filerecovery.databinding.ItemFileBinding
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
    private val onSelectionChanged: (Set<FileItem>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var context: Context
    private val selectedFiles = mutableSetOf<FileItem>()
    private val groupSelectionStates = mutableMapOf<String, Boolean>()
    private var flattenedList = listOf<FileListItem>()


    override fun getItemViewType(position: Int): Int {
        return when (flattenedList[position]) {
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
            VIEW_TYPE_HEADER -> {
                val binding = LayoutFilesBinding.inflate(LayoutInflater.from(context), parent, false)
                DateHeaderViewHolder(binding)
            }
            VIEW_TYPE_PHOTO_VIDEO -> {
                val binding = ItemFileBinding.inflate(LayoutInflater.from(context), parent, false)
                FileViewHolder(binding)
            }
            VIEW_TYPE_AUDIO_DOCUMENT -> {
                val binding = ItemDocumentBinding.inflate(LayoutInflater.from(context), parent, false)
                DocumentViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = flattenedList[position]) {
            is FileListItem.Header -> (holder as DateHeaderViewHolder).bind(item)
            is FileListItem.File -> when (holder) {
                is FileViewHolder -> holder.bind(item.fileItem, item.date)
                is DocumentViewHolder -> holder.bind(item.fileItem, item.date)
            }
        }
    }

    override fun getItemCount(): Int = flattenedList.size

    fun updateData(newDateGroups: List<DateGroup>, dateGrouped: Boolean) {
        isDateGrouped = dateGrouped
        val newFiles = newDateGroups.flatMap { it.files }.toSet()
        selectedFiles.retainAll { it in newFiles }

        val newFlattenedList = mutableListOf<FileListItem>()
        if (isDateGrouped) {
            newDateGroups.forEach { group ->
                newFlattenedList.add(FileListItem.Header(group.date, groupSelectionStates[group.date] ?: false))
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
            selectedFiles.addAll(flattenedList.filterIsInstance<FileListItem.File>().map { it.fileItem })
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
                val groupFiles = flattenedList.filterIsInstance<FileListItem.File>().filter { it.date == date }.map { it.fileItem }
                groupSelectionStates[date] = groupFiles.all { it in selectedFiles }
            }
        }
    }


    fun getSelectedFiles(): Set<FileItem> = selectedFiles

    fun getItemAtPosition(position: Int): FileListItem = flattenedList[position]


    inner class DateHeaderViewHolder(private val binding: LayoutFilesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: FileListItem.Header) {
            binding.tvDate.text = header.date
            binding.root.visibility = if (isDateGrouped) View.VISIBLE else View.GONE
            binding.ivSelectAll.setImageResource(
                if (header.isSelected) R.drawable.ic_tick_selected else R.drawable.ic_tick_unselected_gray
            )
            binding.ivSelectAll.setOnClickListener {
                val newState = !header.isSelected
                groupSelectionStates[header.date] = newState
                val groupFiles = flattenedList.filterIsInstance<FileListItem.File>().filter { it.date == header.date }.map { it.fileItem }
                if (newState) {
                    selectedFiles.addAll(groupFiles)
                } else {
                    selectedFiles.removeAll(groupFiles.toSet())
                }
                notifyDataSetChanged()
                onSelectionChanged(selectedFiles)
            }
        }
    }

    inner class FileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: FileItem, date: String) {
            binding.apply {
                Glide.with(context)
                    .load(file.path)
                    .thumbnail(0.25f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .override(200, 200)
                    .placeholder(R.drawable.ic_launcher_background)
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


    inner class DocumentViewHolder(private val binding: ItemDocumentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(file: FileItem, date: String) {
            binding.apply {
                tvFileName.isSelected = true
                tvFileName.text = file.name

                if (file.type == FileType.Audios) {
                    Glide.with(context)
                        .load(R.drawable.ic_play)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(ivDocumentIcons)
                    ivAudio.visibility = View.VISIBLE
                    tvSubTitle.text = "${formatDuration(file.duration?.toInt() ?: 0)} ${formatSize(file.size)}"
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
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_PHOTO_VIDEO = 1
        const val VIEW_TYPE_AUDIO_DOCUMENT = 2
    }
}