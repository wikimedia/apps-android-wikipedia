package org.wikipedia.util

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.core.text.parseAsHtml
import androidx.core.text.toSpanned
import okio.ByteString.Companion.encodeUtf8
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.UserAliasData
import java.text.Collator
import java.text.Normalizer

object StringUtil {
    private const val CSV_DELIMITER = ","

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
        var sourceStr = source ?: return "".toSpanned()
        if ("<" !in sourceStr && "&" !in sourceStr) {
            // If the string doesn't contain any hints of HTML entities, then skip the expensive
            // processing that fromHtml() performs.
            return sourceStr.toSpanned()
        }
        sourceStr = sourceStr.replace("&#8206;", "\u200E")
            .replace("&#8207;", "\u200F")
            .replace("&amp;", "&")

        // HACK: We don't want to display "images" in the html string, because they will just show
        // up as a green square. Therefore, let's just disable the parsing of images by renaming
        // <img> tags to something that the native Html parser doesn't recognize.
        // This automatically covers both <img></img> and <img /> variations.
        sourceStr = sourceStr.replace("<img ", "<figure ").replace("</img>", "</figure>")

        return sourceStr.parseAsHtml()
    }

    fun highlightEditText(editText: EditText, parentText: String, highlightText: String) {
        val words = highlightText.split("\\s+".toRegex()).toTypedArray()
        var pos = 0
        for (word in words) {
            pos = parentText.indexOf(word, pos)
            if (pos == -1) {
                break
            }
        }
        if (pos == -1) {
            pos = parentText.indexOf(words.last())
        }
        if (pos >= 0) {
            // TODO: Programmatic selection doesn't seem to work with RTL content...
            editText.setSelection(pos, pos + words.last().length)
            editText.performLongClick()
        }
    }

    fun boldenKeywordText(textView: TextView, parentText: String, searchQuery: String?) {
        var parentTextStr = parentText
        val startIndex = indexOf(parentTextStr, searchQuery)
        if (startIndex >= 0) {
            parentTextStr = (parentTextStr.substring(0, startIndex) + "<strong>" +
                    parentTextStr.substring(startIndex, startIndex + searchQuery!!.length) + "</strong>" +
                    parentTextStr.substring(startIndex + searchQuery.length))
            textView.text = fromHtml(parentTextStr)
        } else {
            textView.text = parentTextStr
        }
    }

    fun highlightAndBoldenText(textView: TextView, input: String?, shouldBolden: Boolean, highlightColor: Int) {
        if (!input.isNullOrEmpty()) {
            val spannableString = SpannableString(textView.text)
            val caseInsensitiveSpannableString = SpannableString(textView.text.toString().lowercase())
            var indexOfKeyword = caseInsensitiveSpannableString.toString().lowercase().indexOf(input.lowercase())
            while (indexOfKeyword >= 0) {
                spannableString.setSpan(BackgroundColorSpan(highlightColor), indexOfKeyword, indexOfKeyword + input.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannableString.setSpan(ForegroundColorSpan(Color.BLACK), indexOfKeyword, indexOfKeyword + input.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (shouldBolden) {
                    spannableString.setSpan(StyleSpan(Typeface.BOLD), indexOfKeyword, indexOfKeyword + input.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                indexOfKeyword = caseInsensitiveSpannableString.indexOf(input.lowercase(), indexOfKeyword + input.length)
            }
            textView.text = spannableString
        }
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

    fun userPageTitleFromName(userName: String, wiki: WikiSite): PageTitle {
        return PageTitle(UserAliasData.valueFor(wiki.languageCode), userName, wiki)
    }
}
