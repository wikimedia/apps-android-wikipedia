package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import org.wikipedia.databinding.ViewDonorBadgeBinding
import org.wikipedia.donate.DonorStatus
import org.wikipedia.usercontrib.ContributionsDashboardHelper

class DonorBadgeView(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    interface Callback {
        fun onDonorBadgeClick()
        fun onBecomeDonorClick()
        fun onUpdateDonorStatusClick()
    }
    val binding = ViewDonorBadgeBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun setup(callback: Callback) {
        if (!ContributionsDashboardHelper.contributionsDashboardEnabled) {
            isVisible = false
            return
        }
        isVisible = true
        binding.donorChip.isVisible = false
        binding.becomeADonorChip.isVisible = false
        binding.updateDonorStatusChip.isVisible = false
        when (DonorStatus.donorStatus()) {
            DonorStatus.DONOR -> {
                binding.donorChip.apply {
                    isVisible = true
                    setOnClickListener {
                        callback.onDonorBadgeClick()
                    }
                }
            }
            DonorStatus.NON_DONOR -> {
                binding.becomeADonorChip.apply {
                    isVisible = true
                    setOnClickListener {
                        callback.onBecomeDonorClick()
                    }
                }
            }
            DonorStatus.UNKNOWN -> {
                binding.updateDonorStatusChip.apply {
                    isVisible = true
                    setOnClickListener {
                        callback.onUpdateDonorStatusClick()
                    }
                }
            }
            else -> {
                // Do nothing
            }
        }
    }
}
