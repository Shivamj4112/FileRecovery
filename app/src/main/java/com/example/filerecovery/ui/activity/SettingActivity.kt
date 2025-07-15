package com.example.filerecovery.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.filerecovery.R
import com.example.filerecovery.databinding.ActivitySettingBinding
import com.example.filerecovery.utils.Utils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingActivity : BaseActivity() {

    private lateinit var binding : ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {

            ivArrowBack.setOnClickListener { finish() }

            llChangeLanguage.setOnClickListener {
                startActivity(Intent(this@SettingActivity, LanguageActivity::class.java))
            }

            llRateUS.setOnClickListener {
                Utils.rateUs(this@SettingActivity)
            }

            llShareApp.setOnClickListener {
                Utils.shareApp(this@SettingActivity)
            }


            tvVersion.text = getString(R.string.version, packageManager.getPackageInfo(packageName, 0).versionName)
        }
    }
}