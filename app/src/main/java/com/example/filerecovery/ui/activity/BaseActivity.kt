package com.example.filerecovery.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.filerecovery.R
import com.example.filerecovery.data.viewmodel.local.ContactsViewModel
import com.example.filerecovery.data.viewmodel.local.FileRecoveryViewModel
import com.example.filerecovery.databinding.ItemToastScanningBinding
import com.example.filerecovery.utils.FileType
import com.example.filerecovery.utils.SharedPref
import com.example.filerecovery.utils.ViewModelSingleton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.getValue

data class Language(val code: String, val name: String, val convertedName: String)

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {

    private val contactsViewModel: ContactsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun attachBaseContext(newBase: Context) {
        val selectedLanguage = SharedPref.selectedLanguage
        val locale = if (selectedLanguage == "system" || selectedLanguage.isEmpty()) {
            Locale("en")
        } else {
            Locale(selectedLanguage)
        }
        super.attachBaseContext(getLocalizedContext(newBase, locale))
    }

    private fun getLocalizedContext(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getBackgroundColor(type: String): Int {

        val photosColor = ContextCompat.getColor(this, R.color.photos_storage_color)
        val videosColor = ContextCompat.getColor(this, R.color.videos_storage_color)
        val audiosColor = ContextCompat.getColor(this, R.color.audio_storage_color)
        val documentsColor = ContextCompat.getColor(this, R.color.document_storage_color)
        val otherColor = ContextCompat.getColor(this, R.color.other_storage_color)

        return when (type) {
            FileType.Photos.toString() -> photosColor
            FileType.Videos.toString() -> videosColor
            FileType.Audios.toString() -> audiosColor
            FileType.Documents.toString() -> documentsColor
            else -> otherColor
        }

    }

    fun navigationWithFileType(startActivity: Activity, endActivity: Class<*>, fileType: String) {
        startActivity(Intent(startActivity, endActivity).putExtra("fileType", fileType))
    }

    fun getIntentFileType(): String {
        return intent.getStringExtra("fileType")!!
    }

    fun Context.highlightNumber(
        text: String,
        number: Int,
        textColor: Int = R.color.dark_gray,
        numberColor: Int = R.color.photos_storage_color
    ): SpannableString {
        val textColorInt = ContextCompat.getColor(this, textColor)
        val numberColorInt = ContextCompat.getColor(this, numberColor)

        val numberStr = number.toString()
        val start = text.indexOf(numberStr)
        val end = if (start >= 0) start + numberStr.length else -1

        return SpannableString(text).apply {
            setSpan(ForegroundColorSpan(textColorInt), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            if (start >= 0 && end > start) {
                setSpan(ForegroundColorSpan(numberColorInt), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                Log.w("HighlightDebug", "Number $number not found in text: \"$text\"")
            }
        }
    }




//    fun highlightNumber(text: String, number: Int): SpannableString {
//        val color = ContextCompat.getColor(this, R.color.photos_storage_color)
//        val start = text.indexOf(number.toString())
//        val end = start + number.toString().length
//
//        return SpannableString(text).apply {
//            setSpan(ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//        }
//    }



    fun showScanningToast() {
        val layout = ItemToastScanningBinding.inflate(layoutInflater).root

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = layout
            show()
        }
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    fun formatModifiedDate(timeInMilli: Long): String {
        val currentCal = Calendar.getInstance()
        val fileCal = Calendar.getInstance().apply {
            timeInMillis = timeInMilli
        }

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        return when {
            isSameDay(currentCal, fileCal) -> "Today"
            isYesterday(currentCal, fileCal) -> "Yesterday"
            else -> dateFormat.format(fileCal.time)
        }
    }

    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(todayCal: Calendar, fileCal: Calendar): Boolean {
        val yesterdayCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(yesterdayCal, fileCal)
    }

    fun getFileTypeName(file: String): String {
        return when (file) {
            FileType.Photos.toString() -> getString(R.string.photos)
            FileType.Videos.toString() -> getString(R.string.videos)
            FileType.Audios.toString() -> getString(R.string.audios)
            FileType.Documents.toString() -> getString(R.string.documents)
            FileType.Other.toString() -> getString(R.string.other)
            else -> getString(R.string.other)
        }
    }
}