package org.wikipedia.util;

import android.content.Context;
import android.icu.text.RelativeDateTimeFormatter;
import android.os.Build;
import android.util.ArrayMap;

import androidx.annotation.NonNull;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.UtcDate;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class DateUtil {
    private static final Map<String, SimpleDateFormat> DATE_FORMATS = new HashMap<>();
    private static final Map<String, DateTimeFormatter> DATE_TIME_FORMATTERS = new ArrayMap<>();

    public static synchronized String iso8601DateFormat(Instant instant) {
        return getCachedDateTimeFormatter("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).format(instant);
    }

    public static synchronized Instant iso8601InstantParse(String date) {
        return Instant.from(getCachedDateTimeFormatter("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).parse(date));
    }

    public static synchronized Date iso8601DateParse(String date) throws ParseException {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).parse(date);
    }

    public static synchronized String iso8601LocalDateFormat(Date date) {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT, false).format(date);
    }

    public static synchronized String dbDateFormat(Date date) {
        return getCachedDateFormat("yyyyMMddHHmmss", Locale.ROOT, true).format(date);
    }

    public static synchronized Date dbDateParse(String date) throws ParseException {
        return getCachedDateFormat("yyyyMMddHHmmss", Locale.ROOT, true).parse(date);
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

    public static String getFeedCardDateString(@NonNull Instant instant) {
        return getShortDateString(instant);
    }

    public static String getFeedCardDateString(@NonNull Date date) {
        return getShortDateString(date);
    }

    public static String getFeedCardShortDateString(@NonNull Calendar date) {
        return getExtraShortDateString(date.getTime());
    }

    public static String getMDYDateString(@NonNull Date date) {
        return getDateStringWithSkeletonPattern(date, "MM/dd/yyyy");
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

    private static synchronized String getDateStringWithSkeletonPattern(@NonNull Instant instant, @NonNull String pattern) {
        return getCachedDateTimeFormatter(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault(), false).format(instant);
    }

    private static synchronized String getDateStringWithSkeletonPattern(@NonNull Date date, @NonNull String pattern) {
        return getCachedDateFormat(android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault(), false).format(date);
    }

    private static DateTimeFormatter getCachedDateTimeFormatter(String pattern, Locale locale, boolean utc) {
        if (!DATE_TIME_FORMATTERS.containsKey(pattern)) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withLocale(locale)
                    .withZone(utc ? ZoneOffset.UTC : ZoneId.systemDefault());
            DATE_TIME_FORMATTERS.put(pattern, formatter);
        }
        return DATE_TIME_FORMATTERS.get(pattern);
    }

    private static SimpleDateFormat getCachedDateFormat(String pattern, Locale locale, boolean utc) {
        if (!DATE_FORMATS.containsKey(pattern)) {
            SimpleDateFormat df = new SimpleDateFormat(pattern, locale);
            if (utc) {
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
            }
            DATE_FORMATS.put(pattern, df);
        }
        return DATE_FORMATS.get(pattern);
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

    public static String getShortDateString(@NonNull Instant instant) {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneOffset.UTC)
                .format(instant);
    }

    public static UtcDate getUtcRequestDateFor(int age) {
        return new UtcDate(age);
    }

    public static Calendar getDefaultDateFor(int age) {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.add(Calendar.DATE, -age);
        return calendar;
    }

    public static synchronized Date getHttpLastModifiedDate(@NonNull String dateStr) throws ParseException {
        return getCachedDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH, true).parse(dateStr);
    }

    public static synchronized Instant getHttpLastModifiedInstant(@NonNull String dateStr) {
        return Instant.from(getCachedDateTimeFormatter("EEE, dd MMM yyyy HH:mm:ss zzz",
                Locale.ENGLISH, true).parse(dateStr));
    }

    public static synchronized String getHttpLastModifiedDate(@NonNull Date date) {
        return getCachedDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH, true).format(date);
    }

    public static synchronized String getHttpLastModifiedInstant(@NonNull Instant instant) {
        return getCachedDateTimeFormatter("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH, true).format(instant);
    }

    public static String getReadingListsLastSyncDateString(@NonNull String dateStr) throws ParseException {
        return getDateStringWithSkeletonPattern(iso8601DateParse(dateStr), "d MMM yyyy HH:mm");
    }

    @NonNull public static String yearToStringWithEra(int year) {
        Calendar cal = new GregorianCalendar(year, 1, 1);
        return getDateStringWithSkeletonPattern(cal.getTime(), year < 0 ? "y GG" : "y");
    }

    @NonNull public static String getYearDifferenceString(int year) {
        int diffInYears = Calendar.getInstance().get(Calendar.YEAR) - year;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (diffInYears) {
                case 0:
                    return RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.YEAR);
                case 1:
                    return RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.YEAR);
                case -1:
                    return RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.NEXT, RelativeDateTimeFormatter.AbsoluteUnit.YEAR);
                default:
                    return RelativeDateTimeFormatter.getInstance().format(diffInYears, RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.YEARS);
            }
        } else {
            Context context = WikipediaApp.getInstance().getApplicationContext();
            return diffInYears == 0 ? context.getString(R.string.this_year)
                    : context.getResources().getQuantityString(R.plurals.diff_years, diffInYears, diffInYears);
        }
    }

    @NonNull public static String getDaysAgoString(int daysAgo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            switch (daysAgo) {
                case 0:
                    return RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.DAY);
                case 1:
                    return RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.DAY);
                case -1:
                    return RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.NEXT, RelativeDateTimeFormatter.AbsoluteUnit.DAY);
                default:
                    return RelativeDateTimeFormatter.getInstance().format(daysAgo, RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.DAYS);
            }
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
