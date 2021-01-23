package org.wikipedia.views

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.Unbinder
import org.wikipedia.R
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import java.util.*

class CustomDatePicker : DialogFragment() {
    interface Callback {
        fun onDatePicked(month: Int, day: Int)
    }

    private var callback: Callback? = null
    private var unbinder: Unbinder? = null

    @BindView(R.id.day)
    var day: TextView? = null

    @BindView(R.id.month_string)
    var monthString: TextView? = null

    @BindView(R.id.grid)
    var monthGrid: RecyclerView? = null

    @BindView(R.id.previous_month)
    var previousMonthBtn: ImageView? = null

    @BindView(R.id.next_month)
    var nextMonthBtn: ImageView? = null
    private var today: Calendar? = null
    private val selectedDay = Calendar.getInstance()
    private val callbackDay = Calendar.getInstance()
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = View.inflate(requireContext(), R.layout.date_picker_dialog, null)
        unbinder = ButterKnife.bind(this, view)
        today = Calendar.getInstance()
        setUpMonthGrid()
        setMonthString()
        setDayString()
        return AlertDialog.Builder(requireActivity())
                .setView(view)
                .setPositiveButton(R.string.custom_date_picker_dialog_ok_button_text
                ) { dialog: DialogInterface?, id: Int -> callback!!.onDatePicked(callbackDay[Calendar.MONTH], callbackDay[Calendar.DATE]) }
                .setNegativeButton(R.string.custom_date_picker_dialog_cancel_button_text
                ) { dialog: DialogInterface, id: Int -> dialog.dismiss() }
                .create()
    }

    @OnClick(R.id.previous_month)
    fun onPreviousMonthClicked() {
        val currentMonth = selectedDay[Calendar.MONTH]
        selectedDay[LEAP_YEAR, if (currentMonth == 0) Calendar.DECEMBER else currentMonth - 1] = 1
        setMonthString()
        monthGrid!!.adapter!!.notifyDataSetChanged()
    }

    @OnClick(R.id.next_month)
    fun onNextMonthClicked() {
        val currentMonth = selectedDay[Calendar.MONTH]
        selectedDay[LEAP_YEAR, if (currentMonth == Calendar.DECEMBER) Calendar.JANUARY else currentMonth + 1] = 1
        setMonthString()
        monthGrid!!.adapter!!.notifyDataSetChanged()
    }

    private fun setUpMonthGrid() {
        monthGrid!!.layoutManager = GridLayoutManager(requireContext(), MAX_COLUMN_SPAN)
        monthGrid!!.adapter = CustomCalendarAdapter()
    }

    private fun setMonthString() {
        monthString!!.text = DateUtil.getMonthOnlyWithoutDayDateString(selectedDay.time)
    }

    fun setCallback(callback: Callback?) {
        this.callback = callback
    }

    inner class CustomCalendarAdapter : RecyclerView.Adapter<CustomCalendarAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.view_custom_calendar_day, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setFields(position + 1)
        }

        override fun getItemCount(): Int {
            return selectedDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            private val dayTextView: TextView
            private val circleBackGround: ImageView
            override fun onClick(view: View) {
                selectedDay[Calendar.DATE] = adapterPosition + 1
                callbackDay[LEAP_YEAR, selectedDay[Calendar.MONTH]] = adapterPosition + 1
                setDayString()
                notifyDataSetChanged()
            }

            fun setFields(position: Int) {
                if (position == today!![Calendar.DATE] && today!![Calendar.MONTH] == selectedDay[Calendar.MONTH]) {
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
                } else {
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_text_color))
                }
                if (position == callbackDay[Calendar.DATE] && selectedDay[Calendar.MONTH] == callbackDay[Calendar.MONTH]) {
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
                    dayTextView.typeface = Typeface.DEFAULT_BOLD
                    circleBackGround.visibility = View.VISIBLE
                } else {
                    dayTextView.typeface = Typeface.DEFAULT
                    circleBackGround.visibility = View.GONE
                }
                dayTextView.text = String.format(Locale.getDefault(), "%d", position)
            }

            init {
                dayTextView = itemView.findViewById(R.id.custom_day)
                circleBackGround = itemView.findViewById(R.id.circle)
                itemView.setOnClickListener(this)
            }
        }
    }

    private fun setDayString() {
        day!!.text = DateUtil.getFeedCardShortDateString(selectedDay)
    }

    fun setSelectedDay(month: Int, day: Int) {
        selectedDay[LEAP_YEAR, month] = day
        callbackDay[LEAP_YEAR, month] = day
    }

    override fun onDestroyView() {
        if (unbinder != null) {
            unbinder!!.unbind()
            unbinder = null
        }
        super.onDestroyView()
    }

    companion object {
        const val LEAP_YEAR = 2016
        private const val MAX_COLUMN_SPAN = 7
    }
}