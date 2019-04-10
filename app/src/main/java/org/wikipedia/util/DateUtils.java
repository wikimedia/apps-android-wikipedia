package org.wikipedia.util;

import android.content.Context;
import android.icu.text.RelativeDateTimeFormatter;
import android.os.Build;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.UtcDate;

import java.util.Calendar;
import java.util.Date;

import androidx.annotation.NonNull;

public final class DateUtils {
    public static String getFeedCardDayHeaderDate(int age) {
        return DateUtil.getDateStringWithSkeletonPattern(new UtcDate(age).baseCalendar().getTime(), "EEEE MMM d");
    }

    public static String getFeedCardDateString(int age) {
        return getFeedCardDateString(new UtcDate(age).baseCalendar());
    }

    public static String getFeedCardDateString(@NonNull Calendar date) {
        return DateUtil.getShortDateString(WikipediaApp.getInstance(), date.getTime());
    }

    public static String getFeedCardDateString(@NonNull Date date) {
        return DateUtil.getShortDateString(WikipediaApp.getInstance(), date);
    }

    public static String getFeedCardShortDateString(@NonNull Calendar date) {
        return DateUtil.getExtraShortDateString(date.getTime());
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

    private DateUtils() {
    }
}
