package org.wikipedia.util

import android.icu.text.RelativeDateTimeFormatter
import android.os.Build
import android.text.format.DateFormat
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.model.UtcDate
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object DateUtil {
    private val DATE_FORMATS = HashMap<String, SimpleDateFormat>()

    // TODO: Switch to DateTimeFormatter when minSdk = 26.
    @JvmStatic
    @Synchronized
    fun iso8601DateFormat(date: Date): String {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).format(date)
    }

    @Synchronized
    fun iso8601DateParse(date: String): Date {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT, true).parse(date)!!
    }

    @Synchronized
    fun iso8601ShortDateParse(date: String): Date {
        return getCachedDateFormat("yyyy-MM-dd'Z'", Locale.ROOT, true).parse(date)!!
    }

    @Synchronized
    fun iso8601LocalDateFormat(date: Date): String {
        return getCachedDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT, false).format(date)
    }

    @Synchronized
    fun dbDateFormat(date: Date?): String {
        return getCachedDateFormat("yyyyMMddHHmmss", Locale.ROOT, true).format(date!!)
    }

    @Synchronized
    fun dbDateParse(date: String): Date {
        return getCachedDateFormat("yyyyMMddHHmmss", Locale.ROOT, true).parse(date)!!
    }

    fun getFeedCardDayHeaderDate(age: Int): String {
        return getDateStringWithSkeletonPattern(UtcDate(age).baseCalendar.time, "MMMM d")
    }

    fun getFeedCardDateString(age: Int): String {
        return getFeedCardDateString(UtcDate(age).baseCalendar)
    }

    private fun getFeedCardDateString(date: Calendar): String {
        return getShortDateString(date.time)
    }

    fun getFeedCardDateString(date: Date): String {
        return getShortDateString(date)
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

    private fun getExtraShortDateString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "MMM d")
    }

    fun getTimeString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "HH:mm")
    }

    fun getDateAndTimeWithPipe(date: Date): String {
        return getCachedDateFormat("MMM d, yyyy | HH:mm", Locale.getDefault(), false).format(date)
    }

    @Synchronized
    private fun getDateStringWithSkeletonPattern(date: Date, pattern: String): String {
        return getCachedDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern), Locale.getDefault(), false).format(date)
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

    fun getShortDateString(date: Date): String {
        // todo: consider allowing TWN date formats. It would be useful to have but might be
        //       difficult for translators to write correct format specifiers without being able to
        //       test them. We should investigate localization support in date libraries such as
        //       Joda-Time and how TWN solves this classic problem.
        val dateFormat = DateFormat.getMediumDateFormat(WikipediaApp.getInstance())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        return dateFormat.format(date)
    }

    fun getUtcRequestDateFor(age: Int): UtcDate {
        return UtcDate(age)
    }

    fun getDefaultDateFor(age: Int): Calendar {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.add(Calendar.DATE, -age)
        return calendar
    }

    @Synchronized
    @Throws(ParseException::class)
    fun getHttpLastModifiedDate(dateStr: String): Date {
        return getCachedDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH, true).parse(dateStr)!!
    }

    @Throws(ParseException::class)
    fun getLastSyncDateString(dateStr: String): String {
        return getDateStringWithSkeletonPattern(iso8601DateParse(dateStr), "d MMM yyyy HH:mm")
    }

    fun get24HrFormatTimeOnlyString(date: Date): String {
        return getDateStringWithSkeletonPattern(date, "kk:mm")
    }

    fun yearToStringWithEra(year: Int): String {
        val cal: Calendar = GregorianCalendar(year, 1, 1)
        return getDateStringWithSkeletonPattern(cal.time, if (year < 0) "y GG" else "y")
    }

    fun getYearDifferenceString(year: Int, languageCode: String): String {
        val diffInYears = Calendar.getInstance()[Calendar.YEAR] - year
        val targetResource = L10nUtil.getResourcesForWikiLang(languageCode) ?: WikipediaApp.getInstance().resources
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
}
