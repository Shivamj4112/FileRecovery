package com.example.filerecovery.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.filerecovery.R
import com.example.filerecovery.databinding.ActivitySplashBinding
import com.example.filerecovery.utils.SharedPref

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {

            Handler(Looper.getMainLooper()).postDelayed({

                if (SharedPref.isLanguageScreenShown){
                    if (SharedPref.isOnBoardingShown){
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    }
                    else{
                        startActivity(Intent(this@SplashActivity, OnboardingActivity::class.java))
                    }
                }else{
                    startActivity(Intent(this@SplashActivity, LanguageActivity::class.java))
                }

                finish()
            },3000)

            Glide.with(this@SplashActivity).load(R.drawable.ic_progress_bar).into(ivProgress)
        }
    }
}