package org.wikipedia.watchlist

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogWatchlistExpiryBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil

class WatchlistExpiryDialog : ExtendedBottomSheetDialogFragment() {

    interface Callback {
        fun onExpiryChanged(expiry: WatchlistExpiry)
    }

    private val viewModel: WatchlistExpiryDialogViewModel by viewModels { WatchlistExpiryDialogViewModel.Factory(requireArguments()) }
    private var _binding: DialogWatchlistExpiryBinding? = null
    private val binding get() = _binding!!
    private lateinit var expiryOptions: Array<View>
    private val expiryList = arrayOf(WatchlistExpiry.NEVER, WatchlistExpiry.ONE_WEEK, WatchlistExpiry.ONE_MONTH,
            WatchlistExpiry.THREE_MONTH, WatchlistExpiry.SIX_MONTH)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogWatchlistExpiryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expiryOptions = arrayOf(binding.watchlistExpiryPermanent, binding.watchlistExpiryOneWeek, binding.watchlistExpiryOneMonth,
                binding.watchlistExpiryThreeMonths, binding.watchlistExpirySixMonths)
        setupDialog()
        resetAllOptions()
        selectOption(viewModel.expiry)
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil.displayHeightPx
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupDialog() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is WatchlistExpiryDialogViewModel.UiState.Success -> {
                            FeedbackUtil.makeSnackbar(requireActivity(), getString(R.string.watchlist_page_add_to_watchlist_snackbar,
                                viewModel.pageTitle.displayText, getString(it.newExpiry.stringId))).show()
                            callback()?.onExpiryChanged(it.newExpiry)
                            dismiss()
                        }
                        is WatchlistExpiryDialogViewModel.UiState.Error -> {
                            FeedbackUtil.showError(requireActivity(), it.throwable)
                            dismiss()
                        }
                    }
                }
            }
        }

        expiryOptions.forEachIndexed { index, view ->
            view.tag = expiryList[index]
            view.setOnClickListener(ExpiryOptionClickListener())
        }
    }

    private fun selectOption(expiry: WatchlistExpiry) {
        expiryOptions.find { it.tag == expiry }?.let { updateOptionView(it, true) }
    }

    private fun resetAllOptions() {
        expiryOptions.forEach {
            updateOptionView(it, false)
        }
    }

    private fun updateOptionView(view: View, enabled: Boolean) {
        view.findViewWithTag<TextView>("text").typeface = if (enabled) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        view.findViewWithTag<ImageView>("check").visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    inner class ExpiryOptionClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            resetAllOptions()
            updateOptionView(view, true)
            viewModel.changeExpiry(view.tag as WatchlistExpiry)
        }
    }

    companion object {
        const val ARG_PAGE_TITLE = "pageTitle"
        const val ARG_EXPIRY = "expiry"

        fun newInstance(pageTitle: PageTitle, expiry: WatchlistExpiry): WatchlistExpiryDialog {
            val dialog = WatchlistExpiryDialog()
            dialog.arguments = bundleOf(
                ARG_PAGE_TITLE to pageTitle,
                ARG_EXPIRY to expiry
            )
            return dialog
        }
    }
}
