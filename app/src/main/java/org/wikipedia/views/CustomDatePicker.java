package org.wikipedia.views;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.databinding.DatePickerDialogBinding;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.ResourceUtil;

import java.util.Calendar;
import java.util.Locale;

public class CustomDatePicker extends DialogFragment {
    public interface Callback {
        void onDatePicked(int month, int day);
    }

    public static final int LEAP_YEAR = 2016;
    private static final int MAX_COLUMN_SPAN = 7;
    private Callback callback;
    private DatePickerDialogBinding binding;

    private TextView day;
    private TextView monthString;
    private RecyclerView monthGrid;
    private ImageView previousMonthBtn;
    private ImageView nextMonthBtn;

    private Calendar today, selectedDay = Calendar.getInstance(), callbackDay = Calendar.getInstance();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(requireContext(), R.layout.date_picker_dialog, null);
        binding = DatePickerDialogBinding.inflate(requireActivity().getLayoutInflater());

        day = binding.day;
        monthString = binding.monthString;
        monthGrid = binding.grid;
        previousMonthBtn = binding.previousMonth;
        nextMonthBtn = binding.nextMonth;

        setOnClickListeners();

        today = Calendar.getInstance();
        setUpMonthGrid();
        setMonthString();
        setDayString();
        return new AlertDialog.Builder(requireActivity(), ResourceUtil.getThemedAttributeId(requireContext(), R.attr.dialogTheme))
                .setView(view)
                .setPositiveButton(R.string.custom_date_picker_dialog_ok_button_text,
                        (dialog, id) -> callback.onDatePicked(callbackDay.get(Calendar.MONTH), callbackDay.get(Calendar.DATE)))
                .setNegativeButton(R.string.custom_date_picker_dialog_cancel_button_text,
                        (dialog, id) -> dialog.dismiss())
                .create();
    }

    private void setOnClickListeners() {
        previousMonthBtn.setOnClickListener(v -> {
            int currentMonth = selectedDay.get(Calendar.MONTH);
            selectedDay.set(LEAP_YEAR, currentMonth == 0 ? Calendar.DECEMBER : currentMonth - 1, 1);
            setMonthString();
            monthGrid.getAdapter().notifyDataSetChanged();
        });
        nextMonthBtn.setOnClickListener(v -> {
            int currentMonth = selectedDay.get(Calendar.MONTH);
            selectedDay.set(LEAP_YEAR, currentMonth == Calendar.DECEMBER ? Calendar.JANUARY : currentMonth + 1, 1);
            setMonthString();
            monthGrid.getAdapter().notifyDataSetChanged();
        });
    }

    private void setUpMonthGrid() {
        monthGrid.setLayoutManager(new GridLayoutManager(requireContext(), MAX_COLUMN_SPAN));
        monthGrid.setAdapter(new CustomCalendarAdapter());
    }

    private void setMonthString() {
        monthString.setText(DateUtil.getMonthOnlyWithoutDayDateString(selectedDay.getTime()));
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public class CustomCalendarAdapter extends RecyclerView.Adapter<CustomCalendarAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(requireContext()).inflate(R.layout.view_custom_calendar_day, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.setFields(position + 1);
        }

        @Override
        public int getItemCount() {
            return selectedDay.getActualMaximum(Calendar.DAY_OF_MONTH);
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private TextView dayTextView;
            private ImageView circleBackGround;

            ViewHolder(View itemView) {
                super(itemView);
                dayTextView = itemView.findViewById(R.id.custom_day);
                circleBackGround = itemView.findViewById(R.id.circle);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                selectedDay.set(Calendar.DATE, getAdapterPosition() + 1);
                callbackDay.set(LEAP_YEAR, selectedDay.get(Calendar.MONTH), getAdapterPosition() + 1);
                setDayString();
                notifyDataSetChanged();
            }

            void setFields(int position) {
                if ((position == today.get(Calendar.DATE)) && (today.get(Calendar.MONTH) == selectedDay.get(Calendar.MONTH))) {
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.colorAccent));
                } else {
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.primary_text_color));
                }

                if (position == callbackDay.get(Calendar.DATE) && (selectedDay.get(Calendar.MONTH) == callbackDay.get(Calendar.MONTH))) {
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color));
                    dayTextView.setTypeface(Typeface.DEFAULT_BOLD);
                    circleBackGround.setVisibility(View.VISIBLE);
                } else {
                    dayTextView.setTypeface(Typeface.DEFAULT);
                    circleBackGround.setVisibility(View.GONE);
                }

                dayTextView.setText(String.format(Locale.getDefault(), "%d", (position)));
            }
        }
    }

    private void setDayString() {
        day.setText(DateUtil.getFeedCardShortDateString(selectedDay));
    }

    public void setSelectedDay(int month, int day) {
        selectedDay.set(LEAP_YEAR, month, day);
        callbackDay.set(LEAP_YEAR, month, day);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
