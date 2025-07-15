package com.example.filerecovery.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.filerecovery.data.model.RecoveryCategory
import com.example.filerecovery.databinding.LayoutDocumentRecoveryBinding
import com.example.filerecovery.databinding.LayoutPhotoRecoveryBinding
import com.example.filerecovery.utils.FileType

class FileAdapter(
    private val fileType: String,
    private val onCategoryClick: (RecoveryCategory) -> Unit
) : ListAdapter<RecoveryCategory, RecyclerView.ViewHolder>(RecoveryCategoryDiffCallback()) {

    private lateinit var context: Context

    private companion object {
        const val VIEW_TYPE_PHOTO_VIDEO = 0
        const val VIEW_TYPE_OTHER = 1
    }

    class PhotoVideoViewHolder(val binding: LayoutPhotoRecoveryBinding) :
        RecyclerView.ViewHolder(binding.root)

    class DocumentViewHolder(val binding: LayoutDocumentRecoveryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (fileType) {
            FileType.Photos.toString(), FileType.Videos.toString() -> VIEW_TYPE_PHOTO_VIDEO
            else -> VIEW_TYPE_OTHER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        return when (viewType) {
            VIEW_TYPE_PHOTO_VIDEO -> {
                val binding = LayoutPhotoRecoveryBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                PhotoVideoViewHolder(binding)
            }
            VIEW_TYPE_OTHER -> {
                val binding = LayoutDocumentRecoveryBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
                DocumentViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val category = getItem(position)
        when (holder) {
            is PhotoVideoViewHolder -> {
                holder.binding.apply {
                    tvCategoryName.text = "${category.categoryName} (${category.files.size})"
                    val files = category.files.take(4)
                    val layoutItem = listOf(ivImage1, ivImage2, ivImage3, ivImage4)

                    layoutItem.forEachIndexed { index, imageView ->
                        if (index < files.size) {
                            Glide.with(context)
                                .load(files[index].path)
                                .thumbnail(0.25f) // Load smaller thumbnails
                                .diskCacheStrategy(DiskCacheStrategy.DATA)
                                .override(100, 100) // Resize to reduce memory usage
                                .into(imageView.ivImage)
                            imageView.root.visibility = View.VISIBLE
                            imageView.ivPlay.visibility = if (fileType == FileType.Videos.toString()) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                        } else {
                            imageView.root.visibility = View.INVISIBLE
                        }
                    }

                    root.setOnClickListener { onCategoryClick(category) }
                }
            }
            is DocumentViewHolder -> {
                holder.binding.apply {
                    tvFileName.text = category.categoryName
                    tvFileCount.text = category.files.size.toString()
                    root.setOnClickListener { onCategoryClick(category) }
                }
            }
        }
    }
}

class RecoveryCategoryDiffCallback : DiffUtil.ItemCallback<RecoveryCategory>() {
    override fun areItemsTheSame(oldItem: RecoveryCategory, newItem: RecoveryCategory): Boolean {
        return oldItem.categoryName == newItem.categoryName
    }

    override fun areContentsTheSame(oldItem: RecoveryCategory, newItem: RecoveryCategory): Boolean {
        return oldItem == newItem
    }
}