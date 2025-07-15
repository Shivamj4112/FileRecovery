package com.example.filerecovery.ui.activity

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.filerecovery.R
import com.example.filerecovery.databinding.ActivityLanguageBinding
import com.example.filerecovery.ui.adapter.LanguageAdapter
import com.example.filerecovery.utils.SharedPref
import com.example.filerecovery.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class LanguageActivity : BaseActivity() {

    private lateinit var binding: ActivityLanguageBinding
    private var selectedLanguageCode: String = SharedPref.selectedLanguage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        updateLanguageTitle()

        binding.apply {
            ivArrowBack.setOnClickListener { finish() }
        }

        val adapter = LanguageAdapter(Utils.displayLanguage()) { language ->
            selectedLanguageCode = language.code
            updateLanguageTitle()
        }

        binding.rvLanguage.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        binding.rvLanguage.adapter = adapter

        binding.ivDone.setOnClickListener {
            SharedPref.isLanguageScreenShown = true
            applyLanguageChange(selectedLanguageCode)
        }

        if (SharedPref.isOnBoardingShown) {
            binding.ivDone.setImageResource(R.drawable.ic_done)
        } else {
            binding.ivDone.setImageResource(R.drawable.ic_android_arrow_right)
        }

    }

    private fun updateLanguageTitle() {
        val effectiveLanguageCode =
            if (selectedLanguageCode == "system" || selectedLanguageCode.isEmpty()) "en" else selectedLanguageCode
        val translatedText =
            Utils.getLanguageTranslations()[effectiveLanguageCode] ?: "Choose Language"
        binding.languageSelectionTitle.text = translatedText
    }

    private fun applyLanguageChange(languageCode: String) {
        SharedPref.selectedLanguage = languageCode

        val intent = if (SharedPref.isOnBoardingShown) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, OnboardingActivity::class.java)
        }
        startActivity(intent)
        finishAffinity()

    }
}
