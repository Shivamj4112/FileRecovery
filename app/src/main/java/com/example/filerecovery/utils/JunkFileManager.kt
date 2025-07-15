package com.example.filerecovery.utils

import android.app.Dialog
import android.content.Context
import android.os.Environment
import android.widget.TextView
import android.widget.Toast
import com.example.filerecovery.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class JunkFileManager(private val context: Context) {

    private val junkExtensions = listOf(
        ".tmp", ".log", ".bak", ".cache", ".apk",
        ".temp", ".dmp", ".thumbnail", ".tmb",
        ".ad", ".ads", ".advert", ".old",
        ".crdownload", ".part", ".download"
    )

    private val junkDirs: List<File> by lazy {
        listOf(
            File(Environment.getExternalStorageDirectory(), "Download"),
            File(Environment.getExternalStorageDirectory(), "Android/data"),
            File(Environment.getExternalStorageDirectory(), "Android/obb"),
            File(Environment.getExternalStorageDirectory(), "DCIM/.thumbnails"),
            File(Environment.getExternalStorageDirectory(), "Advertisements"),
            File(context.cacheDir, ""),
            File(context.externalCacheDir ?: context.cacheDir, ""),
            File(Environment.getExternalStorageDirectory(), "temp"),
            File(Environment.getExternalStorageDirectory(), "cache"),
            File(Environment.getExternalStorageDirectory(), "LOST.DIR"),
            File(Environment.getExternalStorageDirectory(), "backup"),
            File(Environment.getExternalStorageDirectory(), ".trash"),
            File(Environment.getExternalStorageDirectory(), "tmp")
        )
    }

    fun updateJunkFilesSize(textView: TextView) {
        CoroutineScope(Dispatchers.Main).launch {
            val size = withContext(Dispatchers.IO) { calculateJunkFilesSize() }
            textView.text = formatFileSize(size)
        }
    }

    fun handleJunkRemoval(textView: TextView) {

        val loadingDialog = DialogUtil.showLoadingDialog(context)

        CoroutineScope(Dispatchers.Main).launch {
            val junkSize = withContext(Dispatchers.IO) { calculateJunkFilesSize() }

            loadingDialog.dismiss()

            showJunkRemovalDialog(junkSize) {
                val cleaningDialog = DialogUtil.showLoadingDialog(context,context.getString(R.string.cleaning_junk_files))
                cleanJunkFiles(textView, cleaningDialog)
            }
        }
    }

    private fun calculateJunkFilesSize(): Long {
        var totalSize = 0L
        junkDirs.forEach { dir ->
            if (dir.exists() && dir.isDirectory) {
                totalSize += FileUtils.getFolderSize(dir)
            }
        }

        totalSize += FileUtils.scanForJunkFiles(Environment.getExternalStorageDirectory(), junkExtensions)
        return totalSize
    }

    private fun showJunkRemovalDialog(junkSize: Long, onClean: () -> Unit) {
        DialogUtil.showDeleteDialog(
            context = context,
            title = context.getString(R.string.junk_removal_title),
            message = context.getString(R.string.junk_removal_message, formatFileSize(junkSize)),
            positiveButtonText = context.getString(R.string.clean),
            negativeButtonText = context.getString(R.string.cancel),
            onDeleteClick = onClean
        )
    }

    private fun cleanJunkFiles(textView: TextView, cleaningDialog: Dialog) {
        CoroutineScope(Dispatchers.IO).launch {
            var deletedSize = 0L
            junkDirs.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    deletedSize += FileUtils.deleteFolderContents(dir)
                }
            }

            deletedSize += FileUtils.deleteJunkFiles(Environment.getExternalStorageDirectory(), junkExtensions)

            withContext(Dispatchers.Main) {
                cleaningDialog.dismiss()
                Toast.makeText(
                    context,
                    context.getString(R.string.junk_removed_message),
                    Toast.LENGTH_SHORT
                ).show()
                updateJunkFilesSize(textView)
            }
        }
    }

    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            else -> String.format("%.2f KB", kb)
        }
    }
}