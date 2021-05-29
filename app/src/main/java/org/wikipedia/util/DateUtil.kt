package org.wikipedia.util

import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateFormat
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.model.UtcDate
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object DateUtil {
    private val DATE_TIME_FORMATTERS = ConcurrentHashMap<String, DateTimeFormatter>()

    @JvmStatic
    fun iso8601LocalDateFormat(date: ZonedDateTime): String {
        return getCachedDateTimeFormatter("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT, false).format(date)
    }

    @JvmStatic
    fun dbDateTimeFormat(date: LocalDateTime): String {
        return getCachedDateTimeFormatter("yyyyMMddHHmmss", Locale.ROOT, true).format(date)
    }

    @JvmStatic
    fun dbDateTimeParse(date: String): LocalDateTime {
        return LocalDateTime.parse(date, getCachedDateTimeFormatter("yyyyMMddHHmmss", Locale.ROOT, true))
    }

    @JvmStatic
    fun getFeedCardDayHeaderDate(age: Int): String {
        return getDateStringWithSkeletonPattern(LocalDate.now(ZoneOffset.UTC).minusDays(age.toLong()),
            "MMMM d")
    }

    @JvmStatic
    fun getFeedCardDateString(age: Int): String {
        return getShortDateString(LocalDate.now(ZoneOffset.UTC).minusDays(age.toLong()))
    }

    @JvmStatic
    fun getFeedCardDateString(date: LocalDate): String {
        return getShortDateString(date)
    }

    @JvmStatic
    fun getFeedCardShortDateString(date: LocalDate): String {
        return getDateStringWithSkeletonPattern(date, "MMM d")
    }

    @JvmStatic
    fun getMDYDateString(date: LocalDate): String {
        return getDateStringWithSkeletonPattern(date, "MM/dd/yyyy")
    }

    @JvmStatic
    fun getMonthOnlyDateString(date: LocalDate): String {
        return getDateStringWithSkeletonPattern(date, "MMMM d")
    }

    @JvmStatic
    fun getMonthOnlyWithoutDayDateString(date: LocalDate): String {
        return getDateStringWithSkeletonPattern(date, "MMMM")
    }

    @JvmStatic
    fun getTimeString(time: LocalTime): String {
        return getDateStringWithSkeletonPattern(time, "HH:mm")
    }

    @JvmStatic
    fun getDateAndTimeWithPipe(dateTime: LocalDateTime): String {
        return getCachedDateTimeFormatter("MMM d, yyyy | HH:mm", Locale.getDefault(), false)
            .format(dateTime)
    }

    private fun getDateStringWithSkeletonPattern(temporalAccessor: TemporalAccessor, pattern: String): String {
        return getCachedDateTimeFormatter(DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern),
            Locale.getDefault(), false).format(temporalAccessor)
    }

    private fun getCachedDateTimeFormatter(pattern: String, locale: Locale, utc: Boolean): DateTimeFormatter {
        return DATE_TIME_FORMATTERS.getOrPut(pattern) {
            if (utc)
                DateTimeFormatter.ofPattern(pattern, locale).withZone(ZoneOffset.UTC)
            else
                DateTimeFormatter.ofPattern(pattern, locale)
        }
    }

    @JvmStatic
    fun getShortDateString(date: LocalDate): String {
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
    }

    @JvmStatic
    fun getUtcRequestDateFor(age: Int): UtcDate {
        return UtcDate(age)
    }

    @JvmStatic
    fun getLastSyncDateString(dateStr: String): String {
        return getDateStringWithSkeletonPattern(Instant.parse(dateStr).atZone(ZoneOffset.UTC),
            "d MMM yyyy HH:mm")
    }

    @JvmStatic
    fun get24HrFormatTimeOnlyString(time: LocalTime): String {
        return getDateStringWithSkeletonPattern(time, "kk:mm")
    }

    @JvmStatic
    fun yearToStringWithEra(year: Int): String {
        return getDateStringWithSkeletonPattern(LocalDate.of(year, 1, 1),
            if (year < 0) "y GG" else "y")
    }

    @JvmStatic
    fun getYearDifferenceString(year: Int): String {
        val diffInYears = LocalDate.now().year - year
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when (diffInYears) {
                0 -> RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                1 -> RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                -1 -> RelativeDateTimeFormatter.getInstance().format(RelativeDateTimeFormatter.Direction.NEXT, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                else -> RelativeDateTimeFormatter.getInstance().format(diffInYears.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.YEARS)
            }
        } else {
            val context = WikipediaApp.getInstance().applicationContext
            if (diffInYears == 0) context.getString(R.string.this_year) else context.resources.getQuantityString(R.plurals.diff_years, diffInYears, diffInYears)
        }
    }
}
