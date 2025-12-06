package org.wikipedia.util

import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateFormat
import org.wikipedia.R
import org.wikipedia.extensions.getResources
import org.wikipedia.extensions.getString
import org.wikipedia.feed.model.UtcDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtil {
    private val DB_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    private val YMD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

    // TODO: Switch to DateTimeFormatter when minSdk = 26.
    fun iso8601DateParse(date: String): Date {
        return Date.from(Instant.parse(date))
    }

    fun iso8601LocalDateTimeParse(timestamp: String): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.parse(timestamp), ZoneId.systemDefault())
    }

    fun dbDateFormat(date: Date): String {
        return DB_FORMATTER.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()))
    }

    fun dbDateParse(date: String): Date {
        val dateTime = LocalDateTime.parse(date, DB_FORMATTER)
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
    }

    fun getFeedCardDateString(age: Int): String {
        return getMediumDateString(LocalDate.now().minusDays(age.toLong()))
    }

    fun getFeedCardShortDateString(calendar: Calendar): String {
        return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM d"))
            .format(LocalDate.ofInstant(calendar.toInstant(), ZoneId.systemDefault()))
    }

    fun getMonthOnlyDateString(localDate: LocalDate): String {
        return localDate.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
    }

    fun getMonthOnlyDateString(calendar: Calendar): String {
        return getMonthOnlyDateString(LocalDate.ofInstant(calendar.toInstant(), ZoneId.systemDefault()))
    }

    fun getMonthOnlyDateStringFromTimeString(dateStr: String): String {
        return getMonthOnlyDateString(LocalDate.parse(dateStr))
    }

    fun getYearOnlyDateString(date: Date): String {
        return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()).year.toString()
    }

    fun getYMDDateString(date: LocalDate): String {
        return YMD_FORMATTER.format(date)
    }

    fun getTimeString(dateTime: LocalDateTime): String {
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(dateTime)
    }

    fun getDateAndTimeString(dateStr: String): String {
        return getDateAndTimeString(iso8601LocalDateTimeParse(dateStr))
    }

    fun getDateAndTimeString(dateTime: LocalDateTime): String {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .format(dateTime)
    }

    fun getDateAndTimeString(date: Date): String {
        return getDateAndTimeString(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()))
    }

    fun getMediumDateString(localDate: LocalDate): String {
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(localDate)
    }

    fun getMediumDateString(date: Date): String {
        return getMediumDateString(LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault()))
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
        val skeleton = if (year < 0) "y GG" else "y"
        return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton))
            .format(Year.of(year))
    }

    fun getYearDifferenceString(context: Context, year: Int, languageCode: String): String {
        val diffInYears = Calendar.getInstance()[Calendar.YEAR] - year
        val targetResource = context.getResources(languageCode)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val firstMatchLocaleInstance = RelativeDateTimeFormatter.getInstance(targetResource.configuration.locales.getFirstMatch(arrayOf(languageCode)))
            when (diffInYears) {
                0 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.THIS, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                1 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                -1 -> firstMatchLocaleInstance.format(RelativeDateTimeFormatter.Direction.NEXT, RelativeDateTimeFormatter.AbsoluteUnit.YEAR)
                else -> firstMatchLocaleInstance.format(diffInYears.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.YEARS)
            }
        } else {
            return if (diffInYears == 0) context.getString(languageCode, R.string.this_year)
            else targetResource.getQuantityString(R.plurals.diff_years, diffInYears, diffInYears)
        }
    }
}
