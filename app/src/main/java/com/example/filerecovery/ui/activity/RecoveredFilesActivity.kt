package com.example.filerecovery.ui.activity

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.filerecovery.R
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.data.viewmodel.local.FileRecoveryViewModel
import com.example.filerecovery.databinding.ActivityRecoveredFilesBinding
import com.example.filerecovery.ui.adapter.RecoveredFilesAdapter
import com.example.filerecovery.ui.fragment.CategoryFragment
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.ViewModelSingleton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class RecoveredFilesActivity : BaseActivity() {

    private lateinit var binding: ActivityRecoveredFilesBinding
    private val fileRecoveryModel = ViewModelSingleton.fileRecoveryModel
    private var currentCategory: String = FileType.Photos.toString()
    private var selectedFiles: Set<FileItem> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRecoveredFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fileType = intent.getStringExtra("fileType") ?: FileType.Photos.toString()
        currentCategory = fileType

        setupViewPager()
        setupCategoryButtons()
        setupDeleteButton()
        binding.ivArrowBack.setOnClickListener { finish() }


        val initialPosition = getPositionForFileType(fileType)
        binding.viewPager.setCurrentItem(initialPosition, false)
        updateCategoryButtonAppearance()
    }

    private fun setupViewPager() {
        val categories = listOf(
            FileType.Photos.toString(),
            FileType.Videos.toString(),
            FileType.Audios.toString(),
            FileType.Documents.toString()
        )

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = categories.size
            override fun createFragment(position: Int): Fragment {
                val fragment = CategoryFragment.newInstance(categories[position])
                fragment.onSelectionChanged = { selected ->
                    selectedFiles = selected
                    updateDeleteButtonVisibility()
                }
                return fragment
            }
        }

        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val previousFragment = supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
                if (previousFragment is CategoryFragment) {
                    previousFragment.clearSelections()
                }
                currentCategory = categories[position]
                updateCategoryButtonAppearance()
                selectedFiles = emptySet()
                updateDeleteButtonVisibility()
            }
        })
    }

    private fun setupCategoryButtons() {
        val buttonList =
            listOf(binding.tvPhotos, binding.tvVideos, binding.tvAudios, binding.tvDocuments)
        val categories = listOf(
            FileType.Photos.toString(),
            FileType.Videos.toString(),
            FileType.Audios.toString(),
            FileType.Documents.toString()
        )

        buttonList.forEachIndexed { index, button ->
            button.setOnClickListener {
                binding.viewPager.setCurrentItem(index, true)
                currentCategory = categories[index]
                updateCategoryButtonAppearance()
            }
        }
    }

    private fun setupDeleteButton() {
        binding.btDeleteAll.setOnClickListener {
            DialogUtil.showDeleteDialog(this@RecoveredFilesActivity) {
                lifecycleScope.launch {
                    deleteSelectedFiles()
                    selectedFiles = emptySet()
                    updateDeleteButtonVisibility()
                    fileRecoveryModel.scanDeletedFiles(currentCategory)
                    val currentFragment = supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")
                    if (currentFragment is CategoryFragment) {
                        currentFragment.clearSelections()
                        currentFragment.scanRecoveredFiles()
                    }
                }
            }
        }
    }

    private fun updateCategoryButtonAppearance() {
        val buttonList = listOf(binding.tvPhotos, binding.tvVideos, binding.tvAudios, binding.tvDocuments)
        val selectedTextColor = ContextCompat.getColor(this, R.color.white)
        val unselectedTextColor = ContextCompat.getColor(this, R.color.dark_gray)
        val selectedBackground = ContextCompat.getColor(this, R.color.photos_storage_color)
        val unselectedBackground = ContextCompat.getColor(this, R.color.selection_card_background)

        buttonList.forEachIndexed { index, button ->
            val isSelected = currentCategory == when (button.id) {
                binding.tvPhotos.id -> FileType.Photos.toString()
                binding.tvVideos.id -> FileType.Videos.toString()
                binding.tvAudios.id -> FileType.Audios.toString()
                binding.tvDocuments.id -> FileType.Documents.toString()
                else -> FileType.Photos.toString()
            }
            button.backgroundTintList = ColorStateList.valueOf(
                if (isSelected) selectedBackground else unselectedBackground
            )
            button.setTextColor(if (isSelected) selectedTextColor else unselectedTextColor)
        }
    }

    private fun updateDeleteButtonVisibility() {
        binding.btDeleteAll.visibility = if (selectedFiles.isNotEmpty()) View.VISIBLE else View.GONE
        binding.btDeleteAll.text = if (selectedFiles.isNotEmpty()) {
            getString(R.string.delete_size, selectedFiles.size.toString())
        } else {
            getString(R.string.delete_size, "0")
        }
    }

    private suspend fun deleteSelectedFiles() = withContext(Dispatchers.IO) {
        selectedFiles.forEach { fileItem ->
            val file = File(fileItem.path)
            if (file.exists()) {
                file.delete()
            }
        }
    }
    private fun getPositionForFileType(fileType: String): Int {
        return when (fileType) {
            FileType.Photos.toString() -> 0
            FileType.Videos.toString() -> 1
            FileType.Audios.toString() -> 2
            FileType.Documents.toString() -> 3
            else -> 0
        }
    }

}

