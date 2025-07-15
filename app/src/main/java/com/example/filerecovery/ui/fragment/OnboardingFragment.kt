package com.example.filerecovery.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.filerecovery.R
import com.example.filerecovery.databinding.FragmentOnboardingBinding


class OnboardingFragment : Fragment() {

    private lateinit var binding : FragmentOnboardingBinding

    companion object {
        private const val ARG_POSITION = "position"

        fun newInstance(position: Int): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false)

        // Content for each screen
        val position = arguments?.getInt(ARG_POSITION) ?: 0

        binding.apply {
            when (position) {
                0 -> {
                    imageView.setImageResource(R.drawable.img_onboard1)
//                    titleText.text = getString(R.string.onBoardingTitleOne)
//                    descriptionText.text = getString(R.string.onBoardingDescriptionOne)
                }

                1 -> {
                    imageView.setImageResource(R.drawable.img_onboard2)
//                    titleText.text = getString(R.string.onBoardingTitleTwo)
//                    descriptionText.text = getString(R.string.onBoardingDescriptionTwo)
                }

                2 -> {
                    imageView.setImageResource(R.drawable.img_onboard3)
//                    titleText.text = getString(R.string.onBoardingTitleThird)
//                    descriptionText.text = getString(R.string.onBoardingDescriptionThird)
                }
            }
        }

        return binding.root
    }
}