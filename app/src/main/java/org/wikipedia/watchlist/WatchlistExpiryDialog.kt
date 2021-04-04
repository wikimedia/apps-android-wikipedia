package org.wikipedia.watchlist

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.databinding.DialogWatchlistExpiryBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.DimenUtil

class WatchlistExpiryDialog : ExtendedBottomSheetDialogFragment() {

    interface Callback {
        fun onExpirySelect(expiry: WatchlistExpiry)
    }

    private var _binding: DialogWatchlistExpiryBinding? = null
    private val binding get() = _binding!!
    private lateinit var expiry: WatchlistExpiry
    private lateinit var expiryOptions: Array<View>
    private val expiryList = arrayOf(WatchlistExpiry.NEVER, WatchlistExpiry.ONE_WEEK, WatchlistExpiry.ONE_MONTH,
            WatchlistExpiry.THREE_MONTH, WatchlistExpiry.SIX_MONTH)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogWatchlistExpiryBinding.inflate(inflater, container, false)
        expiry = requireArguments().getSerializable(ARG_EXPIRY) as WatchlistExpiry
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expiryOptions = arrayOf(binding.watchlistExpiryPermanent, binding.watchlistExpiryOneWeek, binding.watchlistExpiryOneMonth,
                binding.watchlistExpiryThreeMonths, binding.watchlistExpirySixMonths)
        setupListeners()
        resetAllOptions()
        selectOption(expiry)
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.navTabDialogPeekHeight))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
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
        view.findViewWithTag<ImageView>("check").isVisible = enabled
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    inner class ExpiryOptionClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            resetAllOptions()
            updateOptionView(view, true)
            callback()?.onExpirySelect(view.tag as WatchlistExpiry)
        }
    }

    companion object {
        private const val ARG_EXPIRY = "expiry"

        @JvmStatic
        fun newInstance(expiry: WatchlistExpiry): WatchlistExpiryDialog {
            val dialog = WatchlistExpiryDialog()
            dialog.arguments = bundleOf(ARG_EXPIRY to expiry)
            return dialog
        }
    }
}
