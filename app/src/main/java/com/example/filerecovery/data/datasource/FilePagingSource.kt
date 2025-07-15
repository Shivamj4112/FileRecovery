package com.example.filerecovery.data.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.filerecovery.data.model.DateGroup
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.ui.fragment.FilterBottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FilePagingSource(
    private val files: List<FileItem>,
    private val fileType: String,
    private val currentFilter: FilterBottomSheetDialog.FilterOption,
    private val currentSort: FilterBottomSheetDialog.SortOption,
    private val currentDuration: FilterBottomSheetDialog.DurationOption
) : PagingSource<Int, DateGroupItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DateGroupItem> {
        return try {
            val page = params.key ?: 0
            val pageSize = params.loadSize
            val startIndex = page * pageSize
            val filteredFiles = filterFiles(files, fileType)
            val sortedFiles = sortFiles(filteredFiles)
            val dateGroups = if (isDateGrouped()) {
                groupFilesByDate(sortedFiles)
            } else {
                listOf(DateGroup("", sortedFiles))
            }

            // Flatten date groups into a list of DateGroupItems (headers and files)
            val allItems = dateGroups.flatMap { group ->
                buildList {
                    add(DateGroupItem.Header(group.date))
                    addAll(group.files.map { DateGroupItem.File(group.date, it) })
                }
            }

            val items = allItems.drop(startIndex).take(pageSize)
            val nextKey = if (startIndex + items.size < allItems.size) page + 1 else null
            val prevKey = if (page > 0) page - 1 else null

            LoadResult.Page(
                data = items,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, DateGroupItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
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

        return if (fileType == "Audios") {
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
    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}

sealed class DateGroupItem {
    data class Header(val date: String) : DateGroupItem()
    data class File(val date: String, val fileItem: FileItem) : DateGroupItem()
}