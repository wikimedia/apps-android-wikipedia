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
import org.wikipedia.databinding.DialogDonateBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
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
            onDonateClicked()
        }

        binding.donateGooglePayButton.setOnClickListener {
            (requireActivity() as? BaseActivity)?.launchDonateActivity(
                GooglePayComponent.getDonateActivityIntent(requireActivity(), arguments?.getString(ARG_CAMPAIGN_ID), arguments?.getString(ARG_DONATE_URL)))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is Resource.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.contentsContainer.isVisible = false
                        }
                        is Resource.Error -> {
                            binding.progressBar.isVisible = false
                            FeedbackUtil.showMessage(this@DonateDialog, it.throwable.localizedMessage.orEmpty())
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
        dismiss()
    }

    companion object {
        const val ARG_CAMPAIGN_ID = "campaignId"
        const val ARG_DONATE_URL = "donateUrl"

        fun newInstance(campaignId: String? = null, donateUrl: String? = null): DonateDialog {
            return DonateDialog().apply {
                arguments = bundleOf(
                    ARG_CAMPAIGN_ID to campaignId,
                    ARG_DONATE_URL to donateUrl
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
