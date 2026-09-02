package org.wikipedia.games.onthisday

import android.content.Context
import android.content.res.ColorStateList
import com.google.android.material.datepicker.DayViewDecorator
import kotlinx.parcelize.Parcelize
import org.wikipedia.R
import java.time.LocalDate

@Parcelize
class DateDecorator(
    private val startDate: LocalDate,
    private val endDate: LocalDate,
    private val scoreData: Map<LocalDate, Int>
) : DayViewDecorator() {
    override fun getBackgroundColor(
        context: Context,
        year: Int,
        month: Int,
        day: Int,
        valid: Boolean,
        selected: Boolean
    ): ColorStateList? {
        val date = LocalDate.of(year, month + 1, day)
        if (date !in startDate..endDate) {
            return null
        }

        return when (scoreData[date]) {
            0, 1, 2 -> context.getColorStateList(R.color.yellow200)
            3, 4 -> context.getColorStateList(R.color.orange200)
            5 -> context.getColorStateList(R.color.green600)
            else -> null
        }
    }

    override fun getTextColor(
        context: Context,
        year: Int,
        month: Int,
        day: Int,
        valid: Boolean,
        selected: Boolean
    ): ColorStateList? {
        val date = LocalDate.of(year, month + 1, day)
        return when (scoreData[date]) {
            null -> super.getTextColor(context, year, month, day, valid, selected)
            else -> context.getColorStateList(R.color.gray700)
        }
    }
}
