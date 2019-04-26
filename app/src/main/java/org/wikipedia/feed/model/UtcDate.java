package org.wikipedia.feed.model;

import androidx.annotation.NonNull;

import java.util.Calendar;

import static java.util.TimeZone.getTimeZone;

public class UtcDate {
    @NonNull private Calendar cal;
    @NonNull private String year;
    @NonNull private String month;
    @NonNull private String date;

    public UtcDate(int age) {
        this.cal = Calendar.getInstance(getTimeZone("UTC"));
        cal.add(Calendar.DATE, -age);
        this.year = Integer.toString(cal.get(Calendar.YEAR));
        this.month = pad(Integer.toString(cal.get(Calendar.MONTH) + 1));
        this.date = pad(Integer.toString(cal.get(Calendar.DATE)));
    }

    @NonNull
    public Calendar baseCalendar() {
        return cal;
    }

    @NonNull
    public String year() {
        return year;
    }

    @NonNull
    public String month() {
        return month;
    }

    @NonNull
    public String date() {
        return date;
    }

    private String pad(String value) {
        if (value.length() == 1) {
            return "0" + value;
        }
        return value;
    }
}
