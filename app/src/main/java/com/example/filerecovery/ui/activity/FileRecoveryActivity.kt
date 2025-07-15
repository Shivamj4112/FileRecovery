package com.example.filerecovery.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filerecovery.R
import com.example.filerecovery.data.model.RecoveryCategory
import com.example.filerecovery.databinding.ActivityFileRecoveryBinding
import com.example.filerecovery.ui.adapter.FileAdapter
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.ViewModelSingleton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class FileRecoveryActivity : BaseActivity() {

    private lateinit var binding: ActivityFileRecoveryBinding
    var fileRecoveryModel = ViewModelSingleton.fileRecoveryModel
    private lateinit var fileAdapter: FileAdapter

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            DialogUtil.showExitDialog(
                this@FileRecoveryActivity,
                onExitClick = { finish() }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityFileRecoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, backCallback)

        val fileType = getIntentFileType()

        setupUI(fileType)
        setupRecyclerView(fileType)
        observeViewModel()

    }

    private fun setupUI(fileType: String) {
        binding.apply {
            ivArrowBack.setOnClickListener {
                DialogUtil.showExitDialog(
                    this@FileRecoveryActivity,
                    onExitClick = { finish() }
                )
            }
            tvTitleName.text = getString(R.string.recovery, getFileTypeName(fileType))
        }
    }

    private fun setupRecyclerView(fileType: String) {

        fileAdapter = FileAdapter(
            fileType = fileType,
            onCategoryClick = { category ->
                navigationWithCategory(
                    startActivity = this@FileRecoveryActivity,
                    endActivity = FileDetailsActivity::class.java,
                    category = category,
                    fileType = fileType,
                    fileCount = category.files.size.toString()
                )
            }
        )

        binding.rvFilesRecovery.apply {
            layoutManager = LinearLayoutManager(this@FileRecoveryActivity)
            adapter = fileAdapter
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }

    }
    private fun observeViewModel() {
        fileRecoveryModel.files.observe(this) { files ->
            binding.tvFoundedFilesCount.visibility = View.VISIBLE
            binding.pbLoading.visibility = View.GONE
            binding.tvFoundedFilesCount.text = getString(
                R.string.found_file_count,
                getFileTypeName(getIntentFileType()),
                files.size.toString()
            )
        }

        fileRecoveryModel.filesCategories.observe(this) { files ->
            if (files.isNullOrEmpty()) {
                binding.rvFilesRecovery.visibility = View.GONE
                binding.pbLoading.visibility = View.GONE
            } else {
                lifecycleScope.launch(Dispatchers.Default) {
                    val sortedCategories = files.sortedBy { if (it.categoryName == "All") 0 else 1 }
                    withContext(Dispatchers.Main) {
                        binding.pbLoading.visibility = View.GONE
                        binding.rvFilesRecovery.visibility = View.VISIBLE
                        fileAdapter.submitList(sortedCategories)
                    }
                }
            }
        }
    }


    private fun navigationWithCategory(
        startActivity: Activity,
        endActivity: Class<*>,
        category: RecoveryCategory,
        fileType: String,
        fileCount: String,
    ) {
        startActivity(Intent(startActivity, endActivity).apply {
            putExtra("category", category)
            putExtra("fileType", fileType)
            putExtra("fileCount", fileCount)
        })
    }
}