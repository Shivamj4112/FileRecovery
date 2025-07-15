package com.example.filerecovery.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SharedPref {

    private const val PREF_NAME = "file_recovery"
    private lateinit var preference: SharedPreferences

    fun init(context: Context) {
        preference = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var isOnBoardingShown: Boolean
        get() = preference.getBoolean("isOnBoardingShown", false)
        set(value) = preference.edit { putBoolean("isOnBoardingShown", value) }

    var isLanguageScreenShown: Boolean
        get() = preference.getBoolean("isLanguageScreenShown", false)
        set(value) = preference.edit { putBoolean("isLanguageScreenShown", value) }

    var selectedLanguage: String
        get() = preference.getString("selected_language", "system") ?: "system"
        set(value) = preference.edit { putString("selected_language", value) }

    var contactPermissionDialog: Int
        get() = preference.getInt("contact_permission_dialog", 0)
        set(value) = preference.edit { putInt("contact_permission_dialog", value) }
}