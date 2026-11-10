package org.wikipedia.util

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.text.format.DateFormat
import org.wikipedia.WikipediaApp
import org.wikipedia.extensions.getResources
import org.wikipedia.feed.model.UtcDate
import java.text.SimpleDateFormat
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
        return getDateStringWithSkeletonPattern(date.time, "MMM d")
    }

    fun getMDYDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MM/dd/yyyy")
    }

    fun getMonthOnlyDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMMM d")
    }

    fun getMonthWithYearString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MM/yyyy")
    }

    fun getMonthOnlyDateStringFromTimeString(dateStr: String): String {
        return getMonthOnlyDateString(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)!!)
    }

    fun getMonthOnlyDateString(date: LocalDate): String {
        return getDateStringWithSkeletonPattern(date, "MMMM d")
    }

    fun getMonthOnlyWithoutDayDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMMM")
    }

    fun getYearOnlyDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "yyyy")
    }

    fun getYMDDateString(date: LocalDate): String {
        return getCachedDateTimeFormatter("yyyyMMdd", Locale.getDefault(), utc = false, skeleton = false)
            .format(date)
    }

    fun getMMMMdYYYY(date: Date, utc: Boolean = true): String {
        return getCachedDateFormat("MMMM d, yyyy", Locale.getDefault(), utc).format(date)
    }

    fun getTimeString(context: Context, date: Date): String {
        val datePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
        return getDateStringWithSkeletonPattern(date, datePattern)
    }

    fun getTimeString(context: Context, localDateTime: LocalDateTime): String {
        val datePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
        return getDateStringWithSkeletonPattern(localDateTime, datePattern)
    }

    fun getTimeString(context: Context, localDateTime: LocalDateTime, locale: Locale): String {
        val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm a"
        return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, timePattern), locale).format(localDateTime)
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
        return getCachedDateFormat(pattern, Locale.getDefault(), utc = false, skeleton = true)
            .format(date)
    }

    private fun getDateStringWithSkeletonPattern(temporalAccessor: TemporalAccessor, pattern: String): String {
        return getCachedDateTimeFormatter(pattern, Locale.getDefault(), utc = false, skeleton = true)
            .format(temporalAccessor)
    }

    private fun getCachedDateFormat(pattern: String, locale: Locale, utc: Boolean, skeleton: Boolean = false): SimpleDateFormat {
        return DATE_FORMATS.getOrPut(pattern) {
            val df = SimpleDateFormat(if (skeleton) DateFormat.getBestDateTimePattern(locale, pattern) else pattern, locale)
            if (utc) {
                df.timeZone = TimeZone.getTimeZone("UTC")
            }
            df
        }
    }

    private fun getCachedDateTimeFormatter(pattern: String, locale: Locale, utc: Boolean, skeleton: Boolean = false): DateTimeFormatter {
        val dtf = DateTimeFormatter.ofPattern(if (skeleton) DateFormat.getBestDateTimePattern(locale, pattern) else pattern, locale)
        return if (utc) dtf.withZone(ZoneOffset.UTC) else dtf
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
        val cal: Calendar = GregorianCalendar(if (year < 0) year + 1 else year, 1, 1)
        return getDateStringWithSkeletonPattern(cal.time, if (year < 0) "y GG" else "y")
    }

    fun getYearDifferenceString(context: Context, year: Int, languageCode: String): String {
        val diffInYears = Calendar.getInstance()[Calendar.YEAR] - year
        val targetResource = context.getResources(languageCode)
        val firstMatchLocaleInstance = RelativeDateTimeFormatter.getInstance(targetResource.configuration.locales.getFirstMatch(arrayOf(languageCode)))
        return when (diffInYears) {
            0 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
            1 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
            -1 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.NEXT, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
            else -> firstMatchLocaleInstance.format(diffInYears.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.YEARS)
        }
    }

    fun getWeekdayMonthDayDateString(date: LocalDate, locale: Locale): String {
        return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEE, MMMM d"), locale).format(date)
    }
}
