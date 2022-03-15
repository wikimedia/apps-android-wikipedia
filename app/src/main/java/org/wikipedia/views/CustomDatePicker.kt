package org.wikipedia.views

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.databinding.DatePickerDialogBinding
import org.wikipedia.databinding.ViewCustomCalendarDayBinding
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ResourceUtil
import java.util.*

class CustomDatePicker : DialogFragment() {
    interface Callback {
        fun onDatePicked(month: Int, day: Int)
    }

    private var _binding: DatePickerDialogBinding? = null
    private val binding get() = _binding!!

    private val today = Calendar.getInstance()
    private val selectedDay = Calendar.getInstance()
    private val callbackDay = Calendar.getInstance()

    var callback: Callback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DatePickerDialogBinding.inflate(LayoutInflater.from(requireContext()))
        setUpMonthGrid()
        setMonthString()
        setDayString()
        setPreviousMonthClickListener()
        setNextMonthClickListener()
        return AlertDialog.Builder(requireActivity())
                .setView(binding.root)
                .setPositiveButton(R.string.custom_date_picker_dialog_ok_button_text) { _: DialogInterface?, _: Int -> callback?.onDatePicked(callbackDay[Calendar.MONTH], callbackDay[Calendar.DATE]) }
                .setNegativeButton(R.string.custom_date_picker_dialog_cancel_button_text) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
    }

    private fun setPreviousMonthClickListener() {
        binding.previousMonth.setOnClickListener {
            val currentMonth = selectedDay[Calendar.MONTH]
            selectedDay[LEAP_YEAR, if (currentMonth == 0) Calendar.DECEMBER else currentMonth - 1] = 1
            setMonthString()
        }
    }

    private fun setNextMonthClickListener() {
        binding.nextMonth.setOnClickListener {
            val currentMonth = selectedDay[Calendar.MONTH]
            selectedDay[LEAP_YEAR, if (currentMonth == Calendar.DECEMBER) Calendar.JANUARY else currentMonth + 1] = 1
            setMonthString()
        }
    }

    private fun setUpMonthGrid() {
        binding.calendarGrid.layoutManager = GridLayoutManager(requireContext(), MAX_COLUMN_SPAN)
        binding.calendarGrid.adapter = CustomCalendarAdapter()
    }

    private fun setMonthString() {
        binding.currentMonth.text = DateUtil.getMonthOnlyWithoutDayDateString(selectedDay.time)
        binding.calendarGrid.adapter!!.notifyDataSetChanged()
    }

    inner class CustomCalendarAdapter : RecyclerView.Adapter<CustomCalendarAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(ViewCustomCalendarDayBinding.inflate(LayoutInflater.from(requireContext()), parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setFields(position + 1)
        }

        override fun getItemCount(): Int {
            return selectedDay.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        inner class ViewHolder internal constructor(private val binding: ViewCustomCalendarDayBinding) :
                RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    selectedDay[Calendar.DATE] = bindingAdapterPosition + 1
                    callbackDay[LEAP_YEAR, selectedDay[Calendar.MONTH]] = bindingAdapterPosition + 1
                    setDayString()
                    notifyItemRangeChanged(0, itemCount)
                }
            }

            fun setFields(position: Int) {
                if (position == today[Calendar.DATE] && today[Calendar.MONTH] == selectedDay[Calendar.MONTH]) {
                    binding.dayText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
                } else {
                    binding.dayText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_text_color))
                }
                if (position == callbackDay[Calendar.DATE] && selectedDay[Calendar.MONTH] == callbackDay[Calendar.MONTH]) {
                    binding.dayText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
                    binding.dayText.typeface = Typeface.DEFAULT_BOLD
                    binding.dayCircleBackground.visibility = View.VISIBLE
                } else {
                    binding.dayText.typeface = Typeface.DEFAULT
                    binding.dayCircleBackground.visibility = View.GONE
                }
                binding.dayText.text = String.format(Locale.getDefault(), "%d", position)
            }
        }
    }

    private fun setDayString() {
        binding.calendarDay.text = DateUtil.getFeedCardShortDateString(selectedDay)
    }

    fun setSelectedDay(month: Int, day: Int) {
        selectedDay[LEAP_YEAR, month] = day
        callbackDay[LEAP_YEAR, month] = day
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val MAX_COLUMN_SPAN = 7
        const val LEAP_YEAR = 2016
    }
}
