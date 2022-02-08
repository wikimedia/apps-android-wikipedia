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
import java.time.LocalDate
import java.util.*

class CustomDatePicker : DialogFragment() {
    interface Callback {
        fun onDatePicked(localDate: LocalDate)
    }

    private var _binding: DatePickerDialogBinding? = null
    private val binding get() = _binding!!

    private val today: LocalDate = LocalDate.now()
    private var selectedDay: LocalDate = LocalDate.now()
    private var callbackDay: LocalDate = LocalDate.now()

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
                .setPositiveButton(R.string.custom_date_picker_dialog_ok_button_text) { _: DialogInterface?, _: Int -> callback?.onDatePicked(callbackDay) }
                .setNegativeButton(R.string.custom_date_picker_dialog_cancel_button_text) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                .create()
    }

    private fun setPreviousMonthClickListener() {
        binding.previousMonth.setOnClickListener {
            selectedDay = LocalDate.of(LEAP_YEAR, selectedDay.month - 1, 1)
            setMonthString()
        }
    }

    private fun setNextMonthClickListener() {
        binding.nextMonth.setOnClickListener {
            selectedDay = LocalDate.of(LEAP_YEAR, selectedDay.month + 1, 1)
            setMonthString()
        }
    }

    private fun setUpMonthGrid() {
        binding.calendarGrid.layoutManager = GridLayoutManager(requireContext(), MAX_COLUMN_SPAN)
        binding.calendarGrid.adapter = CustomCalendarAdapter()
    }

    private fun setMonthString() {
        binding.currentMonth.text = DateUtil.getMonthOnlyWithoutDayDateString(selectedDay)
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
            return selectedDay.month.maxLength()
        }

        inner class ViewHolder internal constructor(private val binding: ViewCustomCalendarDayBinding) :
                RecyclerView.ViewHolder(binding.root) {

            init {
                binding.root.setOnClickListener {
                    val adapterPosition = bindingAdapterPosition
                    selectedDay = selectedDay.withDayOfMonth(adapterPosition + 1)
                    callbackDay = LocalDate.of(LEAP_YEAR, selectedDay.month, adapterPosition + 1)
                    setDayString()
                    notifyItemRangeChanged(0, itemCount)
                }
            }

            fun setFields(position: Int) {
                if (position == today.dayOfMonth && today.month == selectedDay.month) {
                    binding.dayText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent))
                } else {
                    binding.dayText.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_text_color))
                }
                if (position == callbackDay.dayOfMonth && selectedDay.month == callbackDay.month) {
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

    fun setSelectedDay(localDate: LocalDate) {
        selectedDay = localDate.withYear(LEAP_YEAR)
        callbackDay = localDate.withYear(LEAP_YEAR)
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
