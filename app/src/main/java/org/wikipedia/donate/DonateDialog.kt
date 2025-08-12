package org.wikipedia.donate

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.databinding.DialogDonateBinding
import org.wikipedia.donate.donationreminder.DonationReminderHelper
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.util.CustomTabsUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource

class DonateDialog : ExtendedBottomSheetDialogFragment() {
    private var _binding: DialogDonateBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DonateViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogDonateBinding.inflate(inflater, container, false)

        binding.donateOtherButton.setOnClickListener {
            DonorExperienceEvent.logAction("webpay_click", if (arguments?.getString(ARG_CAMPAIGN_ID).isNullOrEmpty()) "setting" else "article_banner")
            onDonateClicked()
        }

        binding.donateGooglePayButton.setOnClickListener {
            invalidateCampaign()
            DonorExperienceEvent.logAction("gpay_click", if (arguments?.getString(ARG_CAMPAIGN_ID).isNullOrEmpty()) "setting" else "article_banner")
            (requireActivity() as? BaseActivity)?.launchDonateActivity(
                GooglePayComponent.getDonateActivityIntent(requireActivity(), arguments?.getString(ARG_CAMPAIGN_ID), arguments?.getString(ARG_DONATE_URL)))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect {
                        when (it) {
                            is Resource.Loading -> {
                                binding.progressBar.isVisible = true
                                binding.contentsContainer.isVisible = false
                            }

                            is Resource.Error -> {
                                binding.progressBar.isVisible = false
                                FeedbackUtil.showMessage(
                                    this@DonateDialog,
                                    it.throwable.localizedMessage.orEmpty()
                                )
                            }

                            is Resource.Success -> {
                                // if Google Pay is not available, then bounce right out to external workflow.
                                if (!it.data) {
                                    onDonateClicked()
                                    return@collect
                                }
                                binding.progressBar.isVisible = false
                                binding.contentsContainer.isVisible = true
                            }
                        }
                    }
                }
                if (arguments?.getBoolean(ARG_FROM_DONATION_REMINDER) == true) {
                    setupDirectGooglePayButton()
                }
            }
        }

        viewModel.checkGooglePayAvailable(requireActivity())

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun onDonateClicked() {
        launchDonateLink(requireContext(), arguments?.getString(ARG_DONATE_URL))
        invalidateCampaign()
        dismiss()
    }

    private fun invalidateCampaign() {
        arguments?.getString(ARG_CAMPAIGN_ID)?.let {
            Prefs.announcementShownDialogs = setOf(it)
        }
    }

    private fun setupDirectGooglePayButton() {
        val donateAmount = Prefs.donationReminderConfig.donateAmount
        val donateAmountText =
            DonateUtil.currencyFormat.format(Prefs.donationReminderConfig.donateAmount)
        val donateButtonText = getString(R.string.donation_reminders_gpay_text, donateAmountText)
        binding.donateGooglePayButton.text = donateButtonText
        binding.donateGooglePayButton.setOnClickListener {
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_milestone",
                action = "gpay_click",
                campaignId = DonationReminderHelper.CAMPAIGN_ID
            )
            (requireActivity() as? BaseActivity)?.launchDonateActivity(
                GooglePayComponent.getDonateActivityIntent(requireActivity(), filledAmount = donateAmount, campaignId = DonationReminderHelper.CAMPAIGN_ID))
        }
        binding.donateGooglePayDifferentAmountButton.isVisible = true
        binding.donateGooglePayDifferentAmountButton.setOnClickListener {
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_milestone",
                action = "other_gpay_click",
                campaignId = DonationReminderHelper.CAMPAIGN_ID
            )
            (requireActivity() as? BaseActivity)?.launchDonateActivity(
                GooglePayComponent.getDonateActivityIntent(requireActivity(), campaignId = DonationReminderHelper.CAMPAIGN_ID))
        }
        binding.donateOtherButton.setOnClickListener {
            DonorExperienceEvent.logDonationReminderAction(
                activeInterface = "reminder_milestone",
                action = "other_method_click",
                campaignId = DonationReminderHelper.CAMPAIGN_ID
            )
            onDonateClicked()
        }
        binding.gPayHeaderContainer.isVisible = false
    }

    companion object {
        const val ARG_CAMPAIGN_ID = "campaignId"
        const val ARG_DONATE_URL = "donateUrl"
        const val ARG_FROM_DONATION_REMINDER = "fromDonationReminder"

        fun newInstance(campaignId: String? = null, donateUrl: String? = null, fromDonationReminder: Boolean = false): DonateDialog {
            return DonateDialog().apply {
                arguments = bundleOf(
                    ARG_CAMPAIGN_ID to campaignId,
                    ARG_DONATE_URL to donateUrl,
                    ARG_FROM_DONATION_REMINDER to fromDonationReminder
                )
            }
        }

        fun launchDonateLink(context: Context, url: String? = null) {
            val donateUrl = url ?: context.getString(R.string.donate_url,
                WikipediaApp.instance.languageState.systemLanguageCode, BuildConfig.VERSION_NAME)
            CustomTabsUtil.openInCustomTab(context, donateUrl)
        }
    }
}
