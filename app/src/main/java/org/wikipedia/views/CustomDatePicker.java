package org.wikipedia.views;

import android.app.Dialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.ResourceUtil;

import java.util.Calendar;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CustomDatePicker extends DialogFragment {
    public interface Callback {
        void onDatePicked(int month, int day);
    }

    public static final int LEAP_YEAR = 2016;
    public static final int MAX_COLUMN_SPAN = 7;
    private Callback callback;

    @BindView(R.id.day) TextView day;
    @BindView(R.id.month_string) TextView monthString;
    @BindView(R.id.grid) RecyclerView monthGrid;
    @BindView(R.id.previous_month) ImageView previousMonthBtn;
    @BindView(R.id.next_month) ImageView nextMonthBtn;

    private Calendar today, selectedDay = Calendar.getInstance(), callbackDay = Calendar.getInstance();
    private View dialog;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), ResourceUtil.getThemedAttributeId(getContext(), R.attr.dialogTheme));
        today = Calendar.getInstance();
        dialog = getActivity().getLayoutInflater().inflate(R.layout.date_picker_dialog, null);
        ButterKnife.bind(this, dialog);
        setUpMonthGrid();
        setMonthString();
        setDayString();
        builder.setView(dialog)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> callback.onDatePicked(callbackDay.get(Calendar.MONTH), callbackDay.get(Calendar.DATE)))
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.dismiss());

        return builder.create();
    }

    @OnClick(R.id.previous_month)
    void onPreviousMonthClicked() {
        int currentMonth = selectedDay.get(Calendar.MONTH);
        selectedDay.set(LEAP_YEAR, currentMonth == 0 ? Calendar.DECEMBER : currentMonth - 1, 1);
        setMonthString();
        monthGrid.getAdapter().notifyDataSetChanged();
    }

    @OnClick(R.id.next_month)
    void onNextMonthClicked() {
        int currentMonth = selectedDay.get(Calendar.MONTH);
        selectedDay.set(LEAP_YEAR, currentMonth == Calendar.DECEMBER ? Calendar.JANUARY : currentMonth + 1, 1);
        setMonthString();
        monthGrid.getAdapter().notifyDataSetChanged();
    }

    private void setUpMonthGrid() {
        monthGrid.setLayoutManager(new GridLayoutManager(getContext(), MAX_COLUMN_SPAN));
        monthGrid.setAdapter(new CustomCalendarAdapter());
    }

    private void setMonthString() {
        monthString.setText(DateUtil.getMonthOnlyWithoutDayDateString(selectedDay.getTime()));
    }


    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public class CustomCalendarAdapter extends RecyclerView.Adapter<CustomCalendarAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.view_custom_calendar_day, parent, false);
            return new ViewHolder(view);
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
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(getContext(), R.attr.colorAccent));
                } else {
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(getContext(), R.attr.primary_text_color));
                }

                if (position == callbackDay.get(Calendar.DATE) && (selectedDay.get(Calendar.MONTH) == callbackDay.get(Calendar.MONTH))) {
                    dayTextView.setTextColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
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

}
