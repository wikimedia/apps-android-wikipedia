package org.wikipedia.util;

import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.UtcDate;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtil {

    public static SimpleDateFormat getIso8601DateFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    public static String getFeedCardDateString(@NonNull Calendar date) {
        return getFeedCardDateString(date.getTime());
    }

    public static String getFeedCardDateString(@NonNull Date date) {
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(WikipediaApp.getInstance());
        return dateFormat.format(date);
    }

    public static UtcDate getUtcRequestDateFor(int age) {
        return new UtcDate(age);
    }

    private DateUtil() {
    }
}
