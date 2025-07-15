package com.example.filerecovery.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import com.example.filerecovery.databinding.ExitDialogItemBinding
import androidx.core.graphics.drawable.toDrawable
import com.bumptech.glide.Glide
import com.example.filerecovery.R
import com.example.filerecovery.databinding.DeleteDialogItemBinding
import com.example.filerecovery.databinding.DialogPermissionBinding
import com.example.filerecovery.databinding.LoadingDialogBinding

object DialogUtil {

    fun showLoadingDialog(
        context: Context,
        message: String = context.getString(R.string.please_wait),
        isCancelable: Boolean = false,

    ): Dialog {
        val dialog = Dialog(context)
        val binding = LoadingDialogBinding.inflate(LayoutInflater.from(context))

        binding.apply {
            Glide.with(context).load(R.drawable.ic_progress_bar).into(ivLoading)
            tvMessage.text = message
        }

       return dialog.apply {
            setContentView(binding.root)
            setCancelable(isCancelable)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            show()
        }
    }

    fun showExitDialog(
        context: Context,
        isCancelable: Boolean = false,
        onExitClick: () -> Unit,
    ) {
        val dialog = Dialog(context)
        val binding = ExitDialogItemBinding.inflate(LayoutInflater.from(context))

        binding.apply {

            btExit.setOnClickListener {
                onExitClick()
            }

            btCancel.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.apply {
            setContentView(binding.root)
            setCancelable(isCancelable)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            show()
        }
    }

    fun showDeleteDialog(
        context: Context,
        title : String = context.getString(R.string.delete_this_file),
        message : String = context.getString(R.string.this_files_will_be_completely_deleted_and_cannot_be_recovered),
        positiveButtonText : String = context.getString(R.string.delete),
        negativeButtonText : String = context.getString(R.string.cancel),
        isCancelable: Boolean = true,
        onDeleteClick: () -> Unit,
    ) {
        val dialog = Dialog(context)
        val binding = DeleteDialogItemBinding.inflate(LayoutInflater.from(context))

        binding.apply {

            tvTitle.text = title
            tvMessage.text = message
            btDelete.text = positiveButtonText
            btCancel.text = negativeButtonText

            btDelete.setOnClickListener {
                onDeleteClick()
                dialog.dismiss()
            }

            btCancel.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.apply {
            setContentView(binding.root)
            setCancelable(isCancelable)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            show()
        }
    }

    fun showPermissionDialog(
        context: Context,
        isCancelable: Boolean = true,
        onAllowClick: () -> Unit,
    ) {
        val dialog = Dialog(context)
        val binding = DialogPermissionBinding.inflate(LayoutInflater.from(context))

        binding.apply {

            btAllow.setOnClickListener {
                onAllowClick()
                dialog.dismiss()
            }
        }

        dialog.apply {
            setContentView(binding.root)
            setCancelable(isCancelable)
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            show()
        }
    }



}