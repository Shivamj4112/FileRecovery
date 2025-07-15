package com.example.filerecovery.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.filerecovery.R
import com.example.filerecovery.databinding.BottomSheetFilterBinding
import com.example.filerecovery.utils.FileType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFilterBinding
    private var onFilterSelected: ((FilterOption, SortOption, DurationOption,SizeOption) -> Unit)? = null
    private var selectedFilter: FilterOption = FilterOption.ALL_DAYS
    private var selectedSort: SortOption = SortOption.NEWEST
    private var selectedDuration: DurationOption = DurationOption.ALL_DURATIONS
    private var selectedSize: SizeOption = SizeOption.ALL_SIZES
    private var fileType: String = ""

    companion object {
        private const val ARG_FILTER = "filter"
        private const val ARG_SORT = "sort"
        private const val ARG_DURATION = "duration"
        private const val ARG_SIZE = "size"
        private const val ARG_FILE_TYPE = "fileType"

        fun newInstance(
            filter: FilterOption,
            sort: SortOption,
            duration: DurationOption,
            size: SizeOption,
            fileType: String
        ): FilterBottomSheetDialog {
            return FilterBottomSheetDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILTER, filter.name)
                    putString(ARG_SORT, sort.name)
                    putString(ARG_DURATION, duration.name)
                    putString(ARG_SIZE, size.name)
                    putString(ARG_FILE_TYPE, fileType)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedFilter =
                FilterOption.valueOf(it.getString(ARG_FILTER, FilterOption.ALL_DAYS.name))
            selectedSort = SortOption.valueOf(it.getString(ARG_SORT, SortOption.NEWEST.name))
            selectedDuration = DurationOption.valueOf(
                it.getString(
                    ARG_DURATION,
                    DurationOption.ALL_DURATIONS.name
                )
            )
            selectedSize = SizeOption.valueOf(it.getString(ARG_SIZE, SizeOption.ALL_SIZES.name))
            fileType = it.getString(ARG_FILE_TYPE, "")
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetFilterBinding.inflate(inflater, container, false)

        binding.llDurationSection.visibility =
            if (fileType == FileType.Audios.toString()) View.VISIBLE else View.GONE
        binding.llSizeSection.visibility =
            if (fileType == FileType.Documents.toString()) View.VISIBLE else View.GONE

        updateFilterButtonBackgrounds()
        updateSortButtonBackgrounds()
        updateDurationButtonBackgrounds()
        updateSizeButtonBackgrounds()

        binding.btnAllDay.setOnClickListener {
            selectedFilter = FilterOption.ALL_DAYS
            updateFilterButtonBackgrounds()
        }
        binding.btn7Days.setOnClickListener {
            selectedFilter = FilterOption.WITHIN_7_DAYS
            updateFilterButtonBackgrounds()
        }
        binding.btn1Month.setOnClickListener {
            selectedFilter = FilterOption.WITHIN_1_MONTH
            updateFilterButtonBackgrounds()
        }
        binding.btn6Months.setOnClickListener {
            selectedFilter = FilterOption.WITHIN_6_MONTHS
            updateFilterButtonBackgrounds()
        }

        binding.btnLargest.setOnClickListener {
            selectedSort = SortOption.LARGEST
            updateSortButtonBackgrounds()
        }
        binding.btnSmallest.setOnClickListener {
            selectedSort = SortOption.SMALLEST
            updateSortButtonBackgrounds()
        }
        binding.btnNewest.setOnClickListener {
            selectedSort = SortOption.NEWEST
            updateSortButtonBackgrounds()
        }
        binding.btnOldest.setOnClickListener {
            selectedSort = SortOption.OLDEST
            updateSortButtonBackgrounds()
        }

        binding.btnDuration05.setOnClickListener {
            selectedDuration = DurationOption.DURATION_0_5
            updateDurationButtonBackgrounds()
        }
        binding.btnDuration520.setOnClickListener {
            selectedDuration = DurationOption.DURATION_5_20
            updateDurationButtonBackgrounds()
        }
        binding.btnDuration2060.setOnClickListener {
            selectedDuration = DurationOption.DURATION_20_60
            updateDurationButtonBackgrounds()
        }
        binding.btnDurationOver60.setOnClickListener {
            selectedDuration = DurationOption.DURATION_OVER_60
            updateDurationButtonBackgrounds()
        }

        binding.btnSize0500kb.setOnClickListener {
            selectedSize = SizeOption.SIZE_0_500KB
            updateSizeButtonBackgrounds()
        }
        binding.btnSize500kb1mb.setOnClickListener {
            selectedSize = SizeOption.SIZE_500KB_1MB
            updateSizeButtonBackgrounds()
        }
        binding.btnSize1mb10mb.setOnClickListener {
            selectedSize = SizeOption.SIZE_1MB_10MB
            updateSizeButtonBackgrounds()
        }
        binding.btnSizeOver10mb.setOnClickListener {
            selectedSize = SizeOption.SIZE_OVER_10MB
            updateSizeButtonBackgrounds()
        }


        // Apply button
        binding.btnApply.setOnClickListener {
            onFilterSelected?.invoke(selectedFilter, selectedSort, selectedDuration, selectedSize)
            dismiss()
        }

        // Clear button
        binding.btnClear.setOnClickListener {
            selectedFilter = FilterOption.ALL_DAYS
            selectedSort = SortOption.NEWEST
            selectedDuration = DurationOption.ALL_DURATIONS
            selectedSize = SizeOption.ALL_SIZES
            updateFilterButtonBackgrounds()
            updateSortButtonBackgrounds()
            updateDurationButtonBackgrounds()
            onFilterSelected?.invoke(selectedFilter, selectedSort, selectedDuration, selectedSize)
            dismiss()
        }


        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selectedFilter", selectedFilter.name)
        outState.putString("selectedSort", selectedSort.name)
        outState.putString("selectedDuration", selectedDuration.name)
        outState.putString("selectedSize", selectedSize.name)
    }


    private fun updateFilterButtonBackgrounds() {
        binding.btnAllDay.apply {
            setBackgroundResource(
                if (selectedFilter == FilterOption.ALL_DAYS) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedFilter == FilterOption.ALL_DAYS) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btn7Days.apply {
            setBackgroundResource(
                if (selectedFilter == FilterOption.WITHIN_7_DAYS) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedFilter == FilterOption.WITHIN_7_DAYS) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btn1Month.apply {
            setBackgroundResource(
                if (selectedFilter == FilterOption.WITHIN_1_MONTH) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedFilter == FilterOption.WITHIN_1_MONTH) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btn6Months.apply {
            setBackgroundResource(
                if (selectedFilter == FilterOption.WITHIN_6_MONTHS) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedFilter == FilterOption.WITHIN_6_MONTHS) R.color.white else R.color.black,
                    null
                )
            )
        }
    }

    private fun updateSortButtonBackgrounds() {
        binding.btnLargest.apply {
            setBackgroundResource(
                if (selectedSort == SortOption.LARGEST) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedSort == SortOption.LARGEST) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnSmallest.apply {
            setBackgroundResource(
                if (selectedSort == SortOption.SMALLEST) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedSort == SortOption.SMALLEST) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnNewest.apply {
            setBackgroundResource(
                if (selectedSort == SortOption.NEWEST) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedSort == SortOption.NEWEST) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnOldest.apply {
            setBackgroundResource(
                if (selectedSort == SortOption.OLDEST) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedSort == SortOption.OLDEST) R.color.white else R.color.black,
                    null
                )
            )
        }
    }

    private fun updateDurationButtonBackgrounds() {
        binding.btnDuration05.apply {
            setBackgroundResource(
                if (selectedDuration == DurationOption.DURATION_0_5) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedDuration == DurationOption.DURATION_0_5) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnDuration520.apply {
            setBackgroundResource(
                if (selectedDuration == DurationOption.DURATION_5_20) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedDuration == DurationOption.DURATION_5_20) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnDuration2060.apply {
            setBackgroundResource(
                if (selectedDuration == DurationOption.DURATION_20_60) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedDuration == DurationOption.DURATION_20_60) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnDurationOver60.apply {
            setBackgroundResource(
                if (selectedDuration == DurationOption.DURATION_OVER_60) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedDuration == DurationOption.DURATION_OVER_60) R.color.white else R.color.black,
                    null
                )
            )
        }
    }

    private fun updateSizeButtonBackgrounds() {
        binding.btnSize0500kb.apply {
            setBackgroundResource(
                if (selectedSize == SizeOption.SIZE_0_500KB) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedSize == SizeOption.SIZE_0_500KB) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnSize500kb1mb.apply {
            setBackgroundResource(
                if (selectedSize == SizeOption.SIZE_500KB_1MB) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedSize == SizeOption.SIZE_500KB_1MB) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnSize1mb10mb.apply {
            setBackgroundResource(
                if (selectedSize == SizeOption.SIZE_1MB_10MB) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedSize == SizeOption.SIZE_1MB_10MB) R.color.white else R.color.black,
                    null
                )
            )
        }
        binding.btnSizeOver10mb.apply {
            setBackgroundResource(
                if (selectedSize == SizeOption.SIZE_OVER_10MB) R.drawable.language_card_selected
                else R.drawable.language_card_unselected
            )
            setTextColor(
                resources.getColor(
                    if (selectedSize == SizeOption.SIZE_OVER_10MB) R.color.white else R.color.black,
                    null
                )
            )
        }
    }

    fun setOnFilterSelected(listener: (FilterOption, SortOption, DurationOption, SizeOption) -> Unit) {
        onFilterSelected = listener
    }


    enum class FilterOption {
        ALL_DAYS, WITHIN_7_DAYS, WITHIN_1_MONTH, WITHIN_6_MONTHS
    }

    enum class SortOption {
        LARGEST, SMALLEST, NEWEST, OLDEST
    }

    enum class DurationOption {
        ALL_DURATIONS, DURATION_0_5, DURATION_5_20, DURATION_20_60, DURATION_OVER_60
    }

    enum class SizeOption {
        ALL_SIZES, SIZE_0_500KB, SIZE_500KB_1MB, SIZE_1MB_10MB, SIZE_OVER_10MB
    }
}