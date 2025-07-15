package com.example.filerecovery.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.example.filerecovery.R
import com.example.filerecovery.ui.components.MultiSegmentProgressBar

object SegmentUtils {

    fun createSegmentsFromUsage(
        context: Context,
        categoryUsageMap: Map<FileType, Double>,
        totalStorageGB: Double,
        useOnlyUsedStorage: Boolean = false,
        softMinPercent: Float = 2f
    ): List<MultiSegmentProgressBar.Segment> {

        val colorMap = mapOf(
            FileType.Photos to R.color.photos_storage_color,
            FileType.Videos to R.color.videos_storage_color,
            FileType.Audios to R.color.audio_storage_color,
            FileType.Documents to R.color.document_storage_color,
            FileType.Other to R.color.other_storage_color
        )

        val usedStorage = categoryUsageMap.values.sum()
        val base = if (useOnlyUsedStorage) usedStorage else totalStorageGB

        val rawSegments = categoryUsageMap.mapNotNull { (category, sizeInGB) ->
            val percent = if (base > 0) (sizeInGB / base * 100).toFloat() else 0f
            val colorRes = colorMap[category] ?: return@mapNotNull null
            val color = ContextCompat.getColor(context, colorRes)
            Triple(category, percent, color)
        }

        // First, apply soft minimums and calculate total
        val adjusted = rawSegments.map { (category, percent, color) ->
            val visualPercent =
                if (percent > 0f && percent < softMinPercent) softMinPercent else percent
            Triple(category, visualPercent, color)
        }

        // Normalize if total percent exceeds 100 due to softMinPercent additions
        val totalAdjusted = adjusted.sumOf { it.second.toDouble() }.toFloat()
        val normalized = adjusted.map { (_, percent, color) ->
            val finalPercent =
                if (totalAdjusted > 100f) (percent / totalAdjusted * 100f) else percent
            MultiSegmentProgressBar.Segment(finalPercent, color)
        }

        return normalized
    }


}