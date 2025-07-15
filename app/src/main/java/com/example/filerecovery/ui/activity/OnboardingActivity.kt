package com.example.filerecovery.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.filerecovery.R
import com.example.filerecovery.databinding.ActivityOnboardingBinding
import com.example.filerecovery.ui.adapter.OnboardingAdapter
import com.example.filerecovery.utils.SharedPref

class OnboardingActivity : BaseActivity() {

    private lateinit var binding : ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.apply {

            viewPager.adapter = OnboardingAdapter(this@OnboardingActivity)

            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {

                    when (position){
                        0 -> tvonBoarding.text = getString(R.string.recover_all_deleted_photos_and_videos)
                        1 -> tvonBoarding.text = getString(R.string.recover_easily_and_quickly)
                        2 -> tvonBoarding.text = getString(R.string.contacts_recovery)
                    }

                    btnNext.text = if (position == 2) getString(R.string.start) else getString(R.string.next)
                }
            })

            dotsIndicator.attachTo(viewPager)

            btnNext.setOnClickListener {
                if (viewPager.currentItem < 2) {
                    viewPager.currentItem += 1
                } else {
                    startLanguageActivity()
                }
            }

        }
    }
    private fun startLanguageActivity() {
        SharedPref.isOnBoardingShown = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

}