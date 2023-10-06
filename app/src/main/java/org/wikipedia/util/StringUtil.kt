package org.wikipedia.util

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.icu.text.CompactDecimalFormat
import android.os.Build
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.core.text.buildSpannedString
import androidx.core.text.set
import okio.ByteString.Companion.encodeUtf8
import org.wikipedia.R
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.CustomHtmlParser
import org.wikipedia.staticdata.UserAliasData
import java.nio.charset.StandardCharsets
import java.text.Collator
import java.text.Normalizer
import java.util.EnumSet
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

object StringUtil {
    private const val CSV_DELIMITER = ","
    private val HIGHLIGHT_REGEX_OPTIONS = EnumSet.of(RegexOption.LITERAL, RegexOption.IGNORE_CASE)

    fun listToCsv(list: List<String?>): String {
        return list.joinToString(CSV_DELIMITER)
    }

    fun csvToList(csv: String): List<String> {
        return delimiterStringToList(csv, CSV_DELIMITER)
    }

    fun delimiterStringToList(delimitedString: String,
                              delimiter: String): List<String> {
        return delimitedString.split(delimiter).filter { it.isNotBlank() }
    }

    fun md5string(s: String): String {
        return s.encodeUtf8().md5().hex()
    }

    fun strip(str: CharSequence?): CharSequence {
        // TODO: remove this function once Kotlin conversion of consumers is complete.
        return if (str.isNullOrEmpty()) "" else str.trim()
    }

    fun intToHexStr(i: Int): String {
        return String.format("x%08x", i)
    }

    fun addUnderscores(text: String?): String {
        return text.orEmpty().replace(" ", "_")
    }

    fun removeUnderscores(text: String?): String {
        return text.orEmpty().replace("_", " ")
    }

    fun dbNameToLangCode(wikiDbName: String): String {
        return (if (wikiDbName.endsWith("wiki")) wikiDbName.substring(0, wikiDbName.length - "wiki".length) else wikiDbName)
                .replace("_", "-")
    }

    fun removeSectionAnchor(text: String?): String {
        text.orEmpty().let {
            return if (it.contains("#")) it.substring(0, it.indexOf("#")) else it
        }
    }

    fun removeNamespace(text: String): String {
        return if (text.length > text.indexOf(":")) {
            text.substring(text.indexOf(":") + 1)
        } else {
            text
        }
    }

    fun removeHTMLTags(text: String?): String {
        return fromHtml(text).toString()
    }

    fun removeStyleTags(text: String): String {
        return text.replace("<style.*?</style>".toRegex(), "")
    }

    fun removeCiteMarkup(text: String): String {
        return text.replace("<cite.*?>".toRegex(), "").replace("</cite>".toRegex(), "")
    }

    fun sanitizeAbuseFilterCode(code: String): String {
        return code.replace("[⧼⧽]".toRegex(), "")
    }

    fun normalizedEquals(str1: String?, str2: String?): Boolean {
        return if (str1 == null || str2 == null) {
            str1 == null && str2 == null
        } else (Normalizer.normalize(str1, Normalizer.Form.NFC)
                == Normalizer.normalize(str2, Normalizer.Form.NFC))
    }

    fun fromHtml(source: String?): Spanned {
        return CustomHtmlParser.fromHtml(source)
    }

    fun highlightEditText(editText: EditText, parentText: String, highlightText: String) {
        val words = highlightText.split("\\s".toRegex()).filter { it.isNotBlank() }
        var pos = 0
        var firstPos = 0
        for (word in words) {
            pos = parentText.indexOf(word, pos)
            if (pos == -1) {
                break
            } else if (firstPos == 0) {
                firstPos = pos
            }
        }
        if (pos == -1) {
            pos = parentText.indexOf(words.last())
            firstPos = pos
        }
        if (pos >= 0) {
            editText.setSelection(firstPos, pos + words.last().length)
        }
    }

    fun boldenKeywordText(textView: TextView, parentText: String, searchQuery: String?) {
        var parentTextStr = parentText
        val startIndex = indexOf(parentTextStr, searchQuery)
        if (startIndex >= 0 && !isIndexInsideHtmlTag(parentTextStr, startIndex)) {
            parentTextStr = (parentTextStr.substring(0, startIndex) + "<strong>" +
                    parentTextStr.substring(startIndex, startIndex + searchQuery!!.length) + "</strong>" +
                    parentTextStr.substring(startIndex + searchQuery.length))
        }
        textView.text = fromHtml(parentTextStr)
    }

    fun setHighlightedAndBoldenedText(textView: TextView, parentText: CharSequence, query: String?) {
        textView.text = if (query.isNullOrEmpty()) parentText else buildSpannedString {
            append(parentText)

            query.toRegex(HIGHLIGHT_REGEX_OPTIONS).findAll(parentText)
                .forEach {
                    val range = it.range
                    val (start, end) = range.first to range.last + 1
                    this[start, end] = BackgroundColorSpan(Color.YELLOW)
                    this[start, end] = ForegroundColorSpan(Color.BLACK)
                    this[start, end] = StyleSpan(Typeface.BOLD)
                }
        }
    }

    private fun isIndexInsideHtmlTag(text: String, index: Int): Boolean {
        var tagStack = 0
        for (i in text.indices) {
            if (text[i] == '<') { tagStack++ } else if (text[i] == '>') { tagStack-- }
            if (i == index) { break }
        }
        return tagStack > 0
    }

    // case insensitive indexOf, also more lenient with similar chars, like chars with accents
    private fun indexOf(original: String, search: String?): Int {
        if (!search.isNullOrEmpty()) {
            val collator = Collator.getInstance()
            collator.strength = Collator.PRIMARY
            for (i in 0..original.length - search.length) {
                if (collator.equals(search, original.substring(i, i + search.length))) {
                    return i
                }
            }
        }
        return -1
    }

    fun getBase26String(@IntRange(from = 1) number: Int): String {
        var num = number
        val base = 26
        var str = ""
        while (--num >= 0) {
            str = ('A' + num % base) + str
            num /= base
        }
        return str
    }

    fun utf8Indices(s: String): IntArray {
        val indices = IntArray(s.toByteArray(StandardCharsets.UTF_8).size)
        var ptr = 0
        var count = 0
        for (i in s.indices) {
            val c = s.codePointAt(i)
            when {
                c <= 0x7F -> count = 1
                c <= 0x7FF -> count = 2
                c <= 0xFFFF -> count = 3
                c <= 0x1FFFFF -> count = 4
            }
            for (j in 0 until count) {
                if (ptr < indices.size) {
                    indices[ptr++] = i
                }
            }
        }
        return indices
    }

    fun userPageTitleFromName(userName: String, wiki: WikiSite): PageTitle {
        return PageTitle(UserAliasData.valueFor(wiki.languageCode), userName, wiki)
    }

    fun getPageViewText(context: Context, pageViews: Long): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val primaryLocale = context.resources.configuration.locales[0]
            val decimalFormat = CompactDecimalFormat.getInstance(primaryLocale, CompactDecimalFormat.CompactStyle.SHORT)
            return decimalFormat.format(pageViews)
        }
        return when {
            pageViews < 1000 -> pageViews.toString()
            pageViews < 1000000 -> {
                context.getString(
                    R.string.view_top_read_card_pageviews_k_suffix,
                    (pageViews / 1000f).roundToInt()
                )
            }
            else -> {
                context.getString(
                    R.string.view_top_read_card_pageviews_m_suffix,
                    (pageViews / 1000000f).roundToInt()
                )
            }
        }
    }

    fun getDiffBytesText(context: Context, diffSize: Int): String {
        return context.resources.getQuantityString(R.plurals.edit_diff_bytes, diffSize.absoluteValue, if (diffSize > 0) "+$diffSize" else diffSize.toString())
    }

    fun capitalize(str: String?): String? {
        return str?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}
