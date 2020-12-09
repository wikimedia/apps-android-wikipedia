package org.wikipedia.watchlist

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.dialog_watchlist_expiry.*
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.DimenUtil


class WatchlistExpiryDialog : ExtendedBottomSheetDialogFragment() {

    interface Callback {
        fun onExpirySelect(expiry: WatchlistExpiry)
    }

    private lateinit var expiry: WatchlistExpiry

    private lateinit var expiryOptions: Array<View>

    private val expiryList = arrayOf(WatchlistExpiry.NEVER, WatchlistExpiry.ONE_WEEK, WatchlistExpiry.ONE_MONTH,
            WatchlistExpiry.THREE_MONTH, WatchlistExpiry.SIX_MONTH)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        expiry = requireArguments().getSerializable(ARG_EXPIRY) as WatchlistExpiry
        return inflater.inflate(R.layout.dialog_watchlist_expiry, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expiryOptions = arrayOf(watchlistExpiryPermanent, watchlistExpiryOneWeek, watchlistExpiryOneMonth,
                watchlistExpiryThreeMonths, watchlistExpirySixMonths)
        setupListeners()
        resetAllOptions()
        selectOption(expiry)
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.navTabDialogPeekHeight))
    }

    private fun setupListeners() {
        expiryOptions.forEachIndexed { index, view ->
            view.tag = expiryList[index]
            view.setOnClickListener(ExpiryOptionClickListener())
        }
    }

    private fun selectOption(expiry: WatchlistExpiry) {
        expiryOptions.find { it.tag == expiry }?.let { update(it, true) }
    }

    private fun resetAllOptions() {
        expiryOptions.forEach {
            update(it, false)
        }
    }

    private fun update(view: View, enabled: Boolean) {
        view.findViewWithTag<TextView>("text").typeface = if (enabled) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        view.findViewWithTag<ImageView>("check").visibility = if (enabled) View.VISIBLE else View.GONE
    }

    inner class ExpiryOptionClickListener : View.OnClickListener {
        override fun onClick(view: View) {
            resetAllOptions()
            update(view, true)
            callback()?.onExpirySelect(view.tag as WatchlistExpiry)
        }
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        private const val ARG_EXPIRY = "expiry"

        @JvmStatic
        fun newInstance(expiry: WatchlistExpiry): WatchlistExpiryDialog {
            val dialog = WatchlistExpiryDialog()
            val args = Bundle()
            args.putSerializable(ARG_EXPIRY, expiry)
            dialog.arguments = args
            return dialog
        }
    }
}