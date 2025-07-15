package com.example.filerecovery.ui.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.filerecovery.R
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.databinding.FragmentCategoryBinding
import com.example.filerecovery.ui.activity.MainActivity
import com.example.filerecovery.ui.activity.PreviewActivity
import com.example.filerecovery.ui.activity.ScanningActivity
import com.example.filerecovery.ui.adapter.RecoveredFilesAdapter
import com.example.filerecovery.utils.DialogUtil
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.FileUtils
import com.example.filerecovery.utils.ViewModelSingleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecoveredFilesAdapter
    private val fileRecoveryModel = ViewModelSingleton.fileRecoveryModel
    private lateinit var category: String
    var onSelectionChanged: ((Set<FileItem>) -> Unit)? = null

    private val manageStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {

                startActivity(
                    Intent(requireContext(), ScanningActivity::class.java).putExtra("fileType", category)
                )
                requireActivity().finish()
            }
        }
    }
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        if (readGranted && writeGranted) {
            startActivity(
                Intent(requireContext(), ScanningActivity::class.java).putExtra("fileType", category)
            )
            requireActivity().finish()
        }
    }


    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(fileType: String): CategoryFragment {
            val fragment = CategoryFragment()
            val args = Bundle()
            args.putString(ARG_CATEGORY, fileType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = arguments?.getString(ARG_CATEGORY) ?: FileType.Photos.toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupEmpty()
        scanRecoveredFiles()
        binding.btRescan.setOnClickListener {
            checkStoragePermissionsAndNavigate(category)
        }
    }

    override fun onResume() {
        super.onResume()

        scanRecoveredFiles()
    }

    fun checkStoragePermissionsAndNavigate(fileType: String) {
        checkStoragePermission(action = {
            startActivity(
                Intent(
                    requireContext(),
                    ScanningActivity::class.java
                ).putExtra("fileType", fileType)
            )
            requireActivity().finish()
        })
    }

    fun checkStoragePermission(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                DialogUtil.showPermissionDialog(requireContext()) {
                    manageStoragePermissionLauncher.launch(intent)
                }
            } else {
                action()
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                DialogUtil.showPermissionDialog(requireContext()) {
                    storagePermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            } else {
                action()
            }

        }
    }

    private fun setupRecyclerView() {
        adapter = RecoveredFilesAdapter(
            requireContext(),
            fileType = category,
            onItemClick = {
                startActivity(
                    Intent(requireContext(), PreviewActivity::class.java).apply {
                        putExtra("fileItem", it)
                        putExtra("hasRecovered", true)
                    }
                )
            },
            onSelectionChanged = { selectedFiles ->
                onSelectionChanged?.invoke(selectedFiles)
            }
        )
        binding.rvRecoveryFiles.layoutManager = when (category) {
            FileType.Photos.toString(), FileType.Videos.toString() -> GridLayoutManager(
                requireContext(),
                3
            )

            FileType.Audios.toString(), FileType.Documents.toString() -> LinearLayoutManager(
                requireContext()
            )

            else -> GridLayoutManager(requireContext(), 3) // Fallback to grid for safety
        }
        binding.rvRecoveryFiles.adapter = adapter
    }

    private fun setupEmpty() {
        binding.apply {
            when (category) {
                FileType.Photos.toString() -> {
                    ivEmpty.setImageResource(R.drawable.ic_empty_photos)
                    tvEmpty.text = getString(R.string.no_photo_found_for_recovered)
                    btRescan.text = getString(R.string.scan_deleted_photos)
                }

                FileType.Videos.toString() -> {
                    ivEmpty.setImageResource(R.drawable.ic_empty_videos)
                    tvEmpty.text = getString(R.string.no_video_found_for_recovered)
                    btRescan.text = getString(R.string.scan_deleted_videos)
                }

                FileType.Audios.toString() -> {
                    ivEmpty.setImageResource(R.drawable.ic_empty_audio)
                    tvEmpty.text = getString(R.string.no_audio_found_for_recovered)
                    btRescan.text = getString(R.string.scan_deleted_audios)
                }

                FileType.Documents.toString() -> {
                    ivEmpty.setImageResource(R.drawable.ic_empty_document)
                    tvEmpty.text = getString(R.string.no_document_found_for_recovered)
                    btRescan.text = getString(R.string.scan_deleted_documents)
                }
            }
        }
    }

    private fun startRescan() {
        binding.apply {
            progressBar.visibility = View.VISIBLE
            rvRecoveryFiles.visibility = View.GONE
        }

        lifecycleScope.launch {
            val scanJob = launch(Dispatchers.IO) {
                val files = FileUtils.getRecoveredFiles(getString(R.string.app_name))
                val filteredFiles = files.filter { it.type.toString() == category }
                withContext(Dispatchers.Main) {
                    adapter.submitList(filteredFiles)
                    updateEmptyView(filteredFiles.isEmpty())
                }
            }

            delay(3000)
            binding.progressBar.visibility = View.GONE
            scanJob.join()
        }
    }

    fun scanRecoveredFiles() {
        lifecycleScope.launch {
            val files =
                withContext(Dispatchers.IO) { FileUtils.getRecoveredFiles(getString(R.string.app_name)) }
            val filteredFiles = files.filter { it.type.toString() == category }
            adapter.submitList(filteredFiles)
            updateEmptyView(filteredFiles.isEmpty())
        }
    }


    fun clearSelections() {
        adapter.clearSelections()
        onSelectionChanged?.invoke(emptySet())
    }


    private fun updateEmptyView(isEmpty: Boolean) {
        binding.apply {
            rvRecoveryFiles.visibility = if (isEmpty) View.GONE else View.VISIBLE
            llEmptyLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
            btRescan.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}