package org.wikipedia.util;

import android.text.format.DateUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.UtcDate;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtil {

    public static SimpleDateFormat getIso8601DateFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    public static String getFeedCardDateString(Calendar cal) {
        int flags = DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_UTC;
        return DateUtils.formatDateTime(WikipediaApp.getInstance(), cal.getTimeInMillis(), flags);
    }

    public static UtcDate getUtcRequestDateFor(int age) {
        return new UtcDate(age);
    }

    private DateUtil() {
    }
}
