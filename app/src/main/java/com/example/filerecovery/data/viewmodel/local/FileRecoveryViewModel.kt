package com.example.filerecovery.data.viewmodel.local

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filerecovery.data.model.AlbumPhoto
import com.example.filerecovery.data.model.ContactItem
import com.example.filerecovery.data.model.FileItem
import com.example.filerecovery.data.model.RecoveryCategory
import com.example.filerecovery.data.model.ScanProgress
import com.example.filerecovery.data.repository.local.FileRecoveryRepository
import com.example.filerecovery.utils.FileType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class FileRecoveryViewModel @Inject constructor(
    private val repository: FileRecoveryRepository
) : ViewModel() {

    private val _files = MutableLiveData<List<FileItem>>()
    val files: LiveData<List<FileItem>> get() = _files

    private val _filesCategories = MutableLiveData<List<RecoveryCategory>>()
    val filesCategories: LiveData<List<RecoveryCategory>> get() = _filesCategories

    private val _scanProgress = MutableLiveData<ScanProgress>()
    val scanProgress: LiveData<ScanProgress> get() = _scanProgress

    private val _contacts = MutableLiveData<List<ContactItem>>()
    val contacts: LiveData<List<ContactItem>> get() = _contacts

    private val _recoveryStatus = MutableLiveData<Boolean>()
    val recoveryStatus: LiveData<Boolean> get() = _recoveryStatus

    private val _filteredFolders = MutableLiveData<List<RecoveryCategory>>()
    val filteredFolders: LiveData<List<RecoveryCategory>> get() = _filteredFolders

    private val _albums = MutableLiveData<List<AlbumPhoto>>()
    val albums: LiveData<List<AlbumPhoto>> get() = _albums

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private var showHiddenFiles = false

    fun loadAlbums() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val albums = if (showHiddenFiles) {
                    repository.getHiddenAlbums()
                } else {
                    repository.getAllAlbums()
                }
                _albums.value = albums
            } catch (e: Exception) {
                _error.value = "Failed to load albums: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Get all files from the specified directory

    fun scanDeletedFiles(fileType: String, maxRetries: Int = 3) {
        viewModelScope.launch {
            var retryCount = 0
            var success = false

            while (retryCount < maxRetries && !success) {
                try {
                    _isLoading.value = true
                    val files = repository.scanDeletedFiles(fileType) { progress ->
                        _scanProgress.postValue(progress)
                    }
                    _files.value = files

                    val recoveryCategoryMap = mutableMapOf<String, MutableList<FileItem>>()
                    files.forEach { file ->
                        val parentDir = File(file.path).parentFile?.name ?: "Uncategorized"
                        recoveryCategoryMap.getOrPut(parentDir) { mutableListOf() }.add(file)
                    }
                    val categories = recoveryCategoryMap.map { (name, files) ->
                        RecoveryCategory(name, files)
                    }.filter { it.files.isNotEmpty() }
                    _filesCategories.postValue(categories ?: emptyList())
                    success = true
                } catch (e: Exception) {
                    retryCount++
                    Log.e("FileRecoveryViewModel", "Error scanning files (Attempt $retryCount): ${e.message}", e)
                    if (retryCount == maxRetries) {
                        _error.postValue("Failed to scan files after $maxRetries attempts: ${e.message}")
                        _files.postValue(emptyList())
                    }
                } finally {
                    if (retryCount >= maxRetries || success) {
                        _isLoading.value = false
                    }
                }
            }
        }
    }

    fun scanDeletedContacts() {
        viewModelScope.launch {
            _contacts.value = repository.scanDeletedContacts()
        }
    }

    fun recoverFile(fileItem: FileItem) {
        viewModelScope.launch {
            _recoveryStatus.value = repository.recoverFile(fileItem)

        }
    }
    fun updateCategory(updatedCategory: RecoveryCategory) {
        val currentCategories = _filesCategories.value?.toMutableList() ?: mutableListOf()
        val index = currentCategories.indexOfFirst { it.categoryName == updatedCategory.categoryName }
        if (index != -1) {
            currentCategories[index] = updatedCategory
        } else {
            currentCategories.add(updatedCategory)
        }
        _filesCategories.postValue(currentCategories)
    }
}