package org.wikipedia.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtil {

    public static SimpleDateFormat getIso8601DateFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    // Ex. "2015-07-18T18:11:52Z"
    public static long fromMwApiTimestamp(String timestamp) {
        long timeInMilliseconds = 0;
        try {
            Date date = getIso8601DateFormat().parse(timestamp);
            timeInMilliseconds = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return timeInMilliseconds;
    }

    private DateUtil() {
    }
}
