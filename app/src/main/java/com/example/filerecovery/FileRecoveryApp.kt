package com.example.filerecovery

import android.app.Application
import com.example.filerecovery.utils.SharedPref
import com.example.filerecovery.utils.StorageInfo
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FileRecoveryApp : Application() {

    override fun onCreate() {
        super.onCreate()

        StorageInfo.getStorageStatsInGB(this)
        SharedPref.init(this)

    }
}