package org.wikipedia.views;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

import org.wikipedia.R;

import java.util.Calendar;

public class DatePickerFragment extends DialogFragment {
    public interface Callback {
        void onDatePicked(int year, int month, int day);
    }

    private Callback callback;
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), R.style.DialogLight, dateSetListener, year, month, day);
        datePickerDialog.setCanceledOnTouchOutside(true);
        return datePickerDialog;
    }

    private DatePickerDialog.OnDateSetListener dateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int month, int day) {
                    if (callback != null) {
                        callback.onDatePicked(year, month, day);
                    }
                }
            };

    public void setCallback(Callback callback) {
        this.callback = callback;
    }
}
