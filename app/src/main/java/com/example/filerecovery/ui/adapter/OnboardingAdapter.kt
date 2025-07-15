package com.example.filerecovery.ui.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.filerecovery.ui.fragment.OnboardingFragment

class OnboardingAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): OnboardingFragment {
            return OnboardingFragment.newInstance(position)
        }
    }