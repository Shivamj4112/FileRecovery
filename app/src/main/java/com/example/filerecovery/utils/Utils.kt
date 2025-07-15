package com.example.filerecovery.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.filerecovery.ui.activity.Language
import java.util.concurrent.TimeUnit

object Utils {

    fun formatDuration(milliseconds: Int): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds.toLong())
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds.toLong()) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun getLanguageList(): List<Language> {
        return listOf(
            Language("system", "Default", "System Language"),
            Language("en", "English", "English"),
            Language("hi", "Hindi", "हिन्दी"),
            Language("gu", "Gujarati", "ગુજરાતી"),
            Language("in", "Indonesian", "Bahasa Indonesia"),
            Language("es", "Spanish", "Español"),
            Language("zh", "Chinese", "中文"),
            Language("fil", "Filipino", "Pilipino"),
            Language("tr", "Turkish", "Türkçe"),
            Language("ta", "Tamil", "தமிழ்"),
        )
    }

    fun getLanguageTranslations(): Map<String, String> {
        return mapOf(
            "system" to "Choose Language",
            "en" to "Choose Language",
            "hi" to "भाषा चुनें",
            "gu" to "ભાષા પસંદ કરો",
            "in" to "Pilih Bahasa",
            "es" to "Elija el idioma",
            "zh" to "选择语言",
            "fil" to "Pumili ng Wika",
            "tr" to "Dil Seçin",
            "ta" to "மொழியைத் தேர்ந்தெடுக்கவும்",
        )
    }

    fun displayLanguage(): List<Language> {
        return getLanguageList().map {
            it.name.replace("Default","English")
            it
        }
    }

    fun shareApp(context: Context){
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
            putExtra(
                Intent.EXTRA_TEXT,
                "Hey, check out this app on Play Store:\nhttps://play.google.com/store/apps/details?id=${context.packageName}"
            )
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    fun rateUs(context: Context){
        val uri = "https://play.google.com/store/apps/details?id=$context.packageName".toUri()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }



}