package org.wikipedia.util

import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateFormat
import androidx.core.os.ConfigurationCompat
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

object DateUtil {
    private val DATE_FORMATS = HashMap<String, DateTimeFormatter>()

    @JvmStatic
    @Synchronized
    fun iso8601DateFormat(date: Date): String {
        return DateTimeFormatter.ISO_INSTANT.format(date.toInstant())
    }

    @JvmStatic
    @Synchronized
    fun iso8601DateParse(date: String): Date {
        return Date.from(Instant.parse(date))
    }

    @JvmStatic
    @Synchronized
    fun iso8601LocalDateFormat(date: ZonedDateTime): String {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT, false).format(date)
    }

    @JvmStatic
    @Synchronized
    fun dbDateFormat(date: LocalDateTime): String {
        return getCachedDateFormat("yyyyMMddHHmmss", Locale.ROOT, true).format(date)
    }

    @JvmStatic
    fun dbDateParse(date: String): LocalDateTime {
        return LocalDateTime.parse(date, getCachedDateFormat("yyyyMMddHHmmss", Locale.ROOT, true))
    }

    @JvmStatic
    fun getFeedCardDayHeaderDate(age: Int): String {
        return getLocalDateStringWithSkeletonPattern(LocalDate.now(ZoneOffset.UTC).minusDays(age.toLong()),
                "MMMM d")
    }

    @JvmStatic
    fun getFeedCardDateString(age: Int): String {
        return getShortDateString(LocalDate.now(ZoneOffset.UTC).minusDays(age.toLong()))
    }

    @JvmStatic
    fun getFeedCardDateString(date: Date): String {
        return getShortDateString(date.toLocalDateTime().toLocalDate())
    }

    @JvmStatic
    fun getFeedCardShortDateString(date: Calendar): String {
        return getExtraShortDateString(date.time)
    }

    fun getMDYDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MM/dd/yyyy")
    }

    @JvmStatic
    fun getMonthOnlyDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMMM d")
    }

    @JvmStatic
    fun getMonthOnlyWithoutDayDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMMM")
    }

    private fun getExtraShortDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMM d")
    }

    fun getTimeString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "HH:mm")
    }

    fun getDateAndTimeWithPipe(date: Date): String {
        return getCachedDateFormat("MMM d, yyyy | HH:mm", Locale.getDefault(), false)
                .format(date.toLocalDateTime())
    }

    private fun getDateStringWithSkeletonPattern(date: Date, pattern: String): String {
        return getSkeletonPattern(pattern).format(date.toLocalDateTime())
    }

    private fun getLocalDateStringWithSkeletonPattern(localDate: LocalDate, pattern: String): String {
        return getSkeletonPattern(pattern).format(localDate)
    }

    private fun getSkeletonPattern(pattern: String): DateTimeFormatter {
        return getCachedDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern),
                Locale.getDefault(), false)
    }

    private fun getCachedDateFormat(pattern: String, locale: Locale, utc: Boolean): DateTimeFormatter {
        return DATE_FORMATS.getOrPut(pattern) {
            if (utc) {
                DateTimeFormatter.ofPattern(pattern, locale).withZone(ZoneOffset.UTC)
            } else {
                DateTimeFormatter.ofPattern(pattern, locale)
            }
        }
    }

    @JvmStatic
    fun getShortDateString(localDate: LocalDate): String {
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        val locale = ConfigurationCompat.getLocales(WikipediaApp.getInstance().resources.configuration)[0]
        return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).withZone(ZoneOffset.UTC)
                .format(localDate)
    }

    @JvmStatic
    fun getDefaultDateFor(age: Int): Calendar {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.add(Calendar.DATE, -age)
        return calendar
    }

    @JvmStatic
    fun getHttpLastModifiedDate(dateStr: String): ZonedDateTime {
        return ZonedDateTime.parse(dateStr, DateTimeFormatter.RFC_1123_DATE_TIME)
    }

    @JvmStatic
    fun getReadingListsLastSyncDateString(dateStr: String): String {
        return getDateStringWithSkeletonPattern(iso8601DateParse(dateStr), "d MMM yyyy HH:mm")
    }

    fun get24HrFormatTimeOnlyString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "kk:mm")
    }

    @JvmStatic
    fun yearToStringWithEra(year: Int): String {
        val cal: Calendar = GregorianCalendar(year, 1, 1)
        return getDateStringWithSkeletonPattern(cal.time, if (year < 0) "y GG" else "y")
    }

    @JvmStatic
    fun getYearDifferenceString(year: Int): String {
        val diffInYears = Calendar.getInstance()[Calendar.YEAR] - year
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

    private fun ZonedDateTime.toLegacyDate(): Date {
        return Date.from(toInstant())
    }

    private fun Date.toLocalDateTime(): LocalDateTime {
        return toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
}
