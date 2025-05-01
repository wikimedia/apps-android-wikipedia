package org.wikipedia.games.onthisday

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcel
import android.os.Parcelable
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.DayViewDecorator
import org.wikipedia.R
import java.util.Calendar

class DateDecorator(
    private val startDate: Calendar,
    private val endDate: Calendar,
    private val scoreData: Map<String, Int>
) : DayViewDecorator() {

    private val calendar = Calendar.getInstance()

    private fun isDateInRange(year: Int, month: Int, day: Int): Boolean {
        synchronized(calendar) {
            calendar.set(year, month, day, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            return !calendar.before(startDate) && !calendar.after(endDate)
        }
    }

    private fun getDateKey(year: Int, month: Int, day: Int): String {
        return "$year-$month-$day"
    }

    override fun getBackgroundColor(
        context: Context,
        year: Int,
        month: Int,
        day: Int,
        valid: Boolean,
        selected: Boolean
    ): ColorStateList? {
        if (!isDateInRange(year, month, day)) {
            return null
        }

        val dateKey = getDateKey(year, month, day)
        val score = scoreData[dateKey]

        return when (score) {
            1, 2, 3 -> ColorStateList.valueOf(ContextCompat.getColor(context, R.color.yellow500))
            4 -> ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red500))
            5 -> ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green600))
            else -> null
        }
    }

    constructor(parcel: Parcel) : this(
        Calendar.getInstance(),
        Calendar.getInstance(),
        hashMapOf()
    ) {}

    override fun describeContents(): Int { return 0 }

    override fun writeToParcel(dest: Parcel, flags: Int) {}

    companion object CREATOR : Parcelable.Creator<DateDecorator> {
        override fun createFromParcel(parcel: Parcel): DateDecorator {
            return DateDecorator(parcel)
        }

        override fun newArray(size: Int): Array<DateDecorator?> {
            return arrayOfNulls(size)
        }
    }
}
