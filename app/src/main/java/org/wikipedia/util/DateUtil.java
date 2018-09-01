package org.wikipedia.util;

import android.content.Context;
import android.icu.text.RelativeDateTimeFormatter;
import android.os.Build;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.UtcDate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public final class DateUtil {

    public static SimpleDateFormat getIso8601DateFormatShort() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    public static SimpleDateFormat getIso8601DateFormat() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return simpleDateFormat;
    }

    public static SimpleDateFormat getIso8601LocalDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);
    }

    public static String getFeedCardDayHeaderDate(int age) {
        return getDateStringWithSkeletonPattern(new UtcDate(age).baseCalendar().getTime(), "EEEE MMM d");
    }

    public static String getFeedCardDateString(int age) {
        return getFeedCardDateString(new UtcDate(age).baseCalendar());
    }

    public static String getFeedCardDateString(@NonNull Calendar date) {
        return getShortDateString(date.getTime());
    }

    public static String getFeedCardDateString(@NonNull Date date) {
        return getShortDateString(date);
    }

    public static String getFeedCardShortDateString(@NonNull Calendar date) {
        return getExtraShortDateString(date.getTime());
    }

    public static String getMonthOnlyDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MMMM d");
    }

    public static String getMonthOnlyWithoutDayDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MMMM");
    }

    private static String getExtraShortDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MMM d");
    }

    private static String getDateStringWithSkeletonPattern(@NonNull Date date, @NonNull String pattern) {
        return new SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault()).format(date);
    }

    public static String getShortDateString(@NonNull Date date) {
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        DateFormat dateFormat = android.text.format.DateFormat.getMediumDateFormat(WikipediaApp.getInstance());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static UtcDate getUtcRequestDateFor(int age) {
        return new UtcDate(age);
    }

    public static Calendar getDefaultDateFor(int age) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.add(Calendar.DATE, -age);
        return calendar;
    }

    public static Date getHttpLastModifiedDate(@NonNull String dateStr) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.parse(dateStr);
    }

    public static String getReadingListsLastSyncDateString(@NonNull String dateStr) throws ParseException {
        return getDateStringWithSkeletonPattern(getIso8601DateFormat().parse(dateStr), "d MMM yyyy HH:mm");
    }

    @NonNull public static String yearToStringWithEra(int year) {
        Calendar cal = new GregorianCalendar(year, 1, 1);
        return getDateStringWithSkeletonPattern(cal.getTime(), year < 0 ? "y GG" : "y");
    }

    @NonNull public static String getYearDifferenceString(int year) {
        int diffInYears = Calendar.getInstance().get(Calendar.YEAR) - year;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return diffInYears == 0 ? RelativeDateTimeFormatter.getInstance()
                    .format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                    : RelativeDateTimeFormatter.getInstance().format(diffInYears,
                    RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.YEARS);
        } else {
            Context context = WikipediaApp.getInstance().getApplicationContext();
            return diffInYears == 0 ? context.getString(R.string.this_year)
                    : context.getResources().getQuantityString(R.plurals.diff_years, diffInYears, diffInYears);
        }
    }

    @NonNull public static String getDaysAgoString(int daysAgo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return daysAgo == 0 ? RelativeDateTimeFormatter.getInstance()
                    .format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.DAY)
                    : RelativeDateTimeFormatter.getInstance().format(daysAgo,
                    RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.DAYS);
        } else {
            Context context = WikipediaApp.getInstance().getApplicationContext();
            // TODO: rename this string/plurals resource:
            return daysAgo == 0 ? context.getResources().getString(R.string.view_continue_reading_card_subtitle_today)
                    : context.getResources().getQuantityString(R.plurals.view_continue_reading_card_subtitle, daysAgo, daysAgo);
        }
    }

    private DateUtil() {
    }
}
