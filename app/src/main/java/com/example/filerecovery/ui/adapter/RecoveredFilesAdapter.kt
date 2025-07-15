package com.example.filerecovery.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.filerecovery.R
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.databinding.ItemDocumentBinding
import com.example.filerecovery.databinding.ItemFileBinding
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.Utils.formatDuration


class RecoveredFilesAdapter(
    private val context: Context,
    private val fileType: String,
    private val onItemClick : (FileItem) -> Unit,
    private val onSelectionChanged: (Set<FileItem>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var files: List<FileItem> = emptyList()
    private val selectedFiles = mutableSetOf<FileItem>()

    companion object {
        private const val VIEW_TYPE_PHOTO_VIDEO = 0
        private const val VIEW_TYPE_AUDIO_DOCUMENT = 1
    }

    fun submitList(newFiles: List<FileItem>) {
        files = newFiles
        selectedFiles.clear()
        onSelectionChanged(selectedFiles)
        notifyDataSetChanged()
    }

    fun clearSelections() {
        selectedFiles.clear()
        onSelectionChanged(selectedFiles)
        notifyDataSetChanged()
    }

    fun getSelectedFiles(): Set<FileItem> = selectedFiles

    override fun getItemViewType(position: Int): Int {
        return when (fileType) {
            FileType.Photos.toString(), FileType.Videos.toString() -> VIEW_TYPE_PHOTO_VIDEO
            else -> VIEW_TYPE_AUDIO_DOCUMENT
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PHOTO_VIDEO -> {
                val binding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                FileViewHolder(binding)
            }
            VIEW_TYPE_AUDIO_DOCUMENT -> {
                val binding = ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DocumentViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val fileItem = files[position]
        when (holder) {
            is FileViewHolder -> holder.bind(fileItem)
            is DocumentViewHolder -> holder.bind(fileItem)
        }
    }

    override fun getItemCount(): Int = files.size

    inner class FileViewHolder(private val binding: ItemFileBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fileItem: FileItem) {
            binding.apply {
                Glide.with(context)
                    .load(fileItem.path)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(ivFilesItem)

                ivSelect.setImageResource(
                    if (selectedFiles.contains(fileItem)) R.drawable.ic_tick_selected
                    else R.drawable.ic_tick_unselected_gray
                )

                ivPlay.isVisible = fileType == FileType.Videos.toString()

                ivSelect.setOnClickListener {
                    if (selectedFiles.contains(fileItem)) {
                        selectedFiles.remove(fileItem)
                        ivSelect.setImageResource(R.drawable.ic_tick_unselected_gray)
                    } else {
                        selectedFiles.add(fileItem)
                        ivSelect.setImageResource(R.drawable.ic_tick_selected)
                    }
                    onSelectionChanged(selectedFiles)
                }

                ivFilesItem.setOnClickListener {
                    onItemClick(fileItem)
                }
            }
        }
    }

    inner class DocumentViewHolder(private val binding: ItemDocumentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fileItem: FileItem) {
            binding.apply {
                tvFileName.isSelected = true
                tvFileName.text = fileItem.name
                tvSubTitle.text = FileItem.formatSize(fileItem.size)

                ivAudio.visibility = View.GONE

                when (fileItem.type) {
                    FileType.Audios -> {
                        tvSubTitle.text = "${formatDuration(fileItem.duration?.toInt()!!)} ${FileItem.formatSize(fileItem.size)}"
                        ivAudio.visibility = View.VISIBLE
                        ivDocumentIcons.setImageResource(R.drawable.ic_play)
                    }
                    FileType.Documents -> {
                        ivAudio.visibility = View.GONE
                        val extension = fileItem.path.substringAfterLast(".", "").lowercase()
                        ivDocumentIcons.setImageResource(
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
                    }
                    else -> {}
                }

                ivSelect.setImageResource(
                    if (selectedFiles.contains(fileItem)) R.drawable.ic_tick_selected
                    else R.drawable.ic_tick_unselected_gray
                )

                ivSelect.setOnClickListener {
                    if (selectedFiles.contains(fileItem)) {
                        selectedFiles.remove(fileItem)
                    } else {
                        selectedFiles.add(fileItem)
                    }
                    onSelectionChanged(selectedFiles)
                    notifyDataSetChanged()
                }

                root.setOnClickListener { onItemClick(fileItem) }
            }
        }
    }
}