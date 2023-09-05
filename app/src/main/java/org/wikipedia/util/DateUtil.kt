package org.wikipedia.util

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateFormat
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.model.UtcDate
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAccessor
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

object DateUtil {
    private val DATE_FORMATS = ConcurrentHashMap<String, SimpleDateFormat>()
    private val DATE_TIME_FORMATTERS = ConcurrentHashMap<String, DateTimeFormatter>()

    // TODO: Switch to DateTimeFormatter when minSdk = 26.
    fun iso8601DateParse(date: String): Date {
        return Date.from(Instant.parse(date))
    }

    fun iso8601LocalDateTimeParse(timestamp: String): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.parse(timestamp), ZoneId.systemDefault())
    }

    fun dbDateFormat(date: Date): String {
        return getCachedDateFormat("yyyyMMddHHmmss", Locale.ROOT, true).format(date)
    }

    fun dbDateParse(date: String): Date {
        return getCachedDateFormat("yyyyMMddHHmmss", Locale.ROOT, true).parse(date)!!
    }

    fun getFeedCardDateString(age: Int): String {
        return getShortDateString(UtcDate(age).baseCalendar.time)
    }

    fun getFeedCardShortDateString(date: Calendar): String {
        return getExtraShortDateString(date.time)
    }

    fun getMDYDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MM/dd/yyyy")
    }

    fun getMonthOnlyDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMMM d")
    }

    fun getMonthOnlyWithoutDayDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMMM")
    }

    fun getYearOnlyDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "yyyy")
    }

    fun getYMDDateString(date: Date): String {
        return getCachedDateFormat("yyyyMMdd", Locale.ROOT, true).format(date)
    }

    private fun getExtraShortDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMM d")
    }

    fun getTimeString(context: Context, date: Date): String {
        val datePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
        return getDateStringWithSkeletonPattern(date, datePattern)
    }

    fun getTimeString(context: Context, localDateTime: LocalDateTime): String {
        val datePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
        return getDateStringWithSkeletonPattern(localDateTime, datePattern)
    }

    fun getShortDayWithTimeString(dateStr: String): String {
        return getDateStringWithSkeletonPattern(iso8601DateParse(dateStr), "MMM d HH:mm")
    }

    fun getShortDayWithTimeString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MM/dd/yyyy HH:mm")
    }

    fun getTimeAndDateString(context: Context, date: Date): String {
        val datePattern = if (DateFormat.is24HourFormat(context)) "HH:mm, MMM d, yyyy" else "hh:mm a, MMM d, yyyy"
        return getDateStringWithSkeletonPattern(date, datePattern)
    }

    fun getTimeAndDateString(context: Context, dateStr: String): String {
        val datePattern = if (DateFormat.is24HourFormat(context)) "HH:mm, MMM d, yyyy" else "hh:mm a, MMM d, yyyy"
        return getDateStringWithSkeletonPattern(iso8601DateParse(dateStr), datePattern)
    }

    fun getDateAndTime(context: Context, date: Date): String {
        val datePattern = if (DateFormat.is24HourFormat(context)) "MMM d, yyyy, HH:mm" else "MMM d, yyyy, hh:mm a"
        return getCachedDateFormat(datePattern, Locale.getDefault(), false).format(date)
    }

    private fun getDateStringWithSkeletonPattern(date: Date, pattern: String): String {
        return getCachedDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault(), false).format(date)
    }

    private fun getDateStringWithSkeletonPattern(temporalAccessor: TemporalAccessor, pattern: String): String {
        return getCachedDateTimeFormatter(DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault(), false)
            .format(temporalAccessor)
    }

    private fun getCachedDateFormat(pattern: String, locale: Locale, utc: Boolean): SimpleDateFormat {
        return DATE_FORMATS.getOrPut(pattern) {
            val df = SimpleDateFormat(pattern, locale)
            if (utc) {
                df.timeZone = TimeZone.getTimeZone("UTC")
            }
            df
        }
    }

    private fun getCachedDateTimeFormatter(pattern: String, locale: Locale, utc: Boolean): DateTimeFormatter {
        return DATE_TIME_FORMATTERS.getOrPut(pattern) {
            val dtf = DateTimeFormatter.ofPattern(pattern, locale)
            return if (utc) dtf.withZone(ZoneOffset.UTC) else dtf
        }
    }

    fun getShortDateString(date: Date): String {
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        val dateFormat = DateFormat.getMediumDateFormat(WikipediaApp.instance)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(date)
    }

    fun getShortDateString(localDate: LocalDate): String {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(localDate)
    }

    fun getUtcRequestDateFor(age: Int): UtcDate {
        return UtcDate(age)
    }

    fun getDefaultDateFor(age: Int): Calendar {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.add(Calendar.DATE, -age)
        return calendar
    }

    fun yearToStringWithEra(year: Int): String {
        val cal: Calendar = GregorianCalendar(year, 1, 1)
        return getDateStringWithSkeletonPattern(cal.time, if (year < 0) "y GG" else "y")
    }

    fun getYearDifferenceString(year: Int, languageCode: String): String {
        val diffInYears = Calendar.getInstance()[Calendar.YEAR] - year
        val targetResource = L10nUtil.getResourcesForWikiLang(languageCode) ?: WikipediaApp.instance.resources
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val firstMatchLocaleInstance = RelativeDateTimeFormatter.getInstance(targetResource.configuration.locales.getFirstMatch(arrayOf(languageCode)))
            when (diffInYears) {
                0 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                1 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                -1 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.NEXT, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                else -> firstMatchLocaleInstance.format(diffInYears.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.YEARS)
            }
        } else {
            return if (diffInYears == 0) L10nUtil.getStringForArticleLanguage(languageCode, R.string.this_year)
            else targetResource.getQuantityString(R.plurals.diff_years, diffInYears, diffInYears)
        }
    }

    fun getDateDiffString(context: Context, date: Date): String {
        val nowDate = Calendar.getInstance().toInstant()
        val beginDate = date.toInstant()
        val diffDays = Duration.between(beginDate, nowDate).toDays().toInt()
        if (diffDays <= 31) {
            val diffString = String.format("%,d", diffDays)
            return context.resources.getQuantityString(R.plurals.date_diff_days, diffDays, diffString)
        }
        val diffMonths = diffDays.floorDiv(31)
        if (diffMonths <= 12) {
            val diffString = String.format("%,d", diffMonths)
            return context.resources.getQuantityString(R.plurals.date_diff_months, diffMonths, diffString)
        }
        val diffYear = diffMonths.floorDiv(12)
        val diffString = String.format("%,d", diffYear)
        return context.resources.getQuantityString(R.plurals.date_diff_years, diffYear, diffString)
    }
}
