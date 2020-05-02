package org.wikipedia.feed.model;

import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;

public class UtcDate {
    private final ZonedDateTime zonedDateTime;
    private final String year;
    private final String month;
    private final String date;

    public UtcDate(int age) {
        this.zonedDateTime = ZonedDateTime.now(ZoneOffset.UTC).minusDays(age);
        this.year = Integer.toString(zonedDateTime.getYear());
        this.month = pad(Integer.toString(zonedDateTime.getMonthValue()));
        this.date = pad(Integer.toString(zonedDateTime.getDayOfMonth()));
    }

    public ZonedDateTime baseZonedDateTime() {
        return zonedDateTime;
    }

    public String year() {
        return year;
    }

    public String month() {
        return month;
    }

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
