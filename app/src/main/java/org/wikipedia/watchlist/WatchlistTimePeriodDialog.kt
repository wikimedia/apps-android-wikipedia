package org.wikipedia.watchlist

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.view_watchlist_time_period.*
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.DimenUtil


class WatchlistTimePeriodDialog : ExtendedBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_watchlist_time_period, container)
    }

    override fun onResume() {
        super.onResume()
        setup()
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.navTabDialogPeekHeight))
    }

    fun setup() {
        watchlistTimePeriodPermanent.setOnClickListener(TimePeriodOptionClickListener(TIME_PERIOD_PERMANENT))
        watchlistTimePeriodOneWeek.setOnClickListener(TimePeriodOptionClickListener(TIME_PERIOD_ONE_WEEK))
        watchlistTimePeriodOneMonth.setOnClickListener(TimePeriodOptionClickListener(TIME_PERIOD_ONE_MONTH))
        watchlistTimePeriodThreeMonths.setOnClickListener(TimePeriodOptionClickListener(TIME_PERIOD_THREE_MONTH))
        watchlistTimePeriodSixMonths.setOnClickListener(TimePeriodOptionClickListener(TIME_PERIOD_SIX_MONTH))
    }

    fun update(view: View, enabled: Boolean) {
        view.findViewWithTag<TextView>("text").typeface = if (enabled) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        view.findViewWithTag<ImageView>("check").visibility = if (enabled) View.VISIBLE else View.GONE
    }

    inner class TimePeriodOptionClickListener(timePeriod: Long) : View.OnClickListener {
        override fun onClick(view: View) {
            update(view, true)
            // TODO: reset all others
        }
    }

    companion object {
        const val TIME_PERIOD_PERMANENT = 0L
        const val TIME_PERIOD_ONE_WEEK = 7L
        const val TIME_PERIOD_ONE_MONTH = 30L
        const val TIME_PERIOD_THREE_MONTH = 90L
        const val TIME_PERIOD_SIX_MONTH = 180L

        fun newInstance(): WatchlistTimePeriodDialog {
            return WatchlistTimePeriodDialog()
        }
    }
}