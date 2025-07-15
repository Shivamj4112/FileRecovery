package com.example.filerecovery.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.filerecovery.R
import com.example.filerecovery.databinding.ItemBackupFileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupFilesAdapter(
   private val fileSelected: (File) -> Unit
) : RecyclerView.Adapter<BackupFilesAdapter.FileViewHolder>() {

    private var files: List<File> = emptyList()
    private val contactCounts = mutableMapOf<String, Int>()

    class FileViewHolder(val binding: ItemBackupFileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val binding = ItemBackupFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.binding.apply {
            tvContactName.text = file.name
            tvContactsCount.text = contactCounts[file.absolutePath]?.toString() ?: "0"
            tvContactDate.text = SimpleDateFormat("yyyy-MM-dd   HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
            root.setOnClickListener {
                fileSelected.invoke(file)
            }
        }
    }

    override fun getItemCount(): Int = files.size

    fun setFiles(newFiles: List<File>, scope: CoroutineScope) {
        files = newFiles
        contactCounts.clear()
        scope.launch(Dispatchers.Main) {
            newFiles.forEach { file ->
                val count = withContext(Dispatchers.IO) { getContactCount(file) }
                contactCounts[file.absolutePath] = count
            }
            notifyDataSetChanged()
        }
    }

    private fun getContactCount(file: File): Int {
        return try {
            FileInputStream(file).use { fis ->
                val content = fis.readBytes().toString(Charsets.UTF_8)
                val vcfEntries = content.split("END:VCARD").filter { it.contains("BEGIN:VCARD") }.count()
                Log.d("BackupFilesAdapter", "File: ${file.name}, Contact count: $vcfEntries")
                vcfEntries
            }
        } catch (e: Exception) {
            Log.e("BackupFilesAdapter", "Error reading VCF file ${file.name}: ${e.message}")
            0
        }
    }
}