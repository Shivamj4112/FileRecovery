package com.example.filerecovery.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.filerecovery.R
import com.example.filerecovery.databinding.ItemLanguageCardBinding
import com.example.filerecovery.ui.activity.Language
import com.example.filerecovery.utils.SharedPref

class LanguageAdapter(
    private val displayLanguages: List<Language>,
    private val onItemClick: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private lateinit var context : Context

    private var selectedPosition = displayLanguages.indexOfFirst {
        it.code == SharedPref.selectedLanguage
    }.takeIf { it >= 0 } ?: 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): LanguageViewHolder {
        context = parent.context
        val binding = ItemLanguageCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: LanguageViewHolder,
        position: Int
    ) {
        val language = displayLanguages[position]
        val isSelected = position == selectedPosition

        val whiteColor = ContextCompat.getColor(context, R.color.white)
        val blackColor = ContextCompat.getColor(context, R.color.black)
        val grayColor = ContextCompat.getColor(context, R.color.dark_gray)

        holder.binding.apply {

            tvTitle.text = language.name
            tvSubTitle.text = "(${language.convertedName})"

            root.setBackgroundResource(
                if (isSelected)
                    R.drawable.language_card_selected
                else
                    R.drawable.language_card_unselected
            )


            tvTitle.setTextColor(if (isSelected) whiteColor else blackColor)
            tvSubTitle.setTextColor(if (isSelected) whiteColor else grayColor)

            ivSelected.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onItemClick.invoke(language)
            }
        }
    }

    override fun getItemCount(): Int {
        return displayLanguages.size
    }

    class LanguageViewHolder(itemView: ItemLanguageCardBinding) : ViewHolder(itemView.root) {
        val binding = itemView
    }
}