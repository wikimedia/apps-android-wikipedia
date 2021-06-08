package org.wikipedia.util

import android.text.Spanned
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.core.text.parseAsHtml
import androidx.core.text.toSpanned
import com.google.gson.Gson
import okio.ByteString.Companion.encodeUtf8
import org.json.JSONArray
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.staticdata.UserAliasData
import java.text.Collator
import java.text.Normalizer

object StringUtil {
    private const val CSV_DELIMITER = ","

    @JvmStatic
    fun listToCsv(list: List<String?>): String {
        return list.joinToString(CSV_DELIMITER)
    }

    @JvmStatic
    fun csvToList(csv: String): List<String> {
        return delimiterStringToList(csv, CSV_DELIMITER)
    }

    @JvmStatic
    fun delimiterStringToList(delimitedString: String,
                              delimiter: String): List<String> {
        return delimitedString.split(delimiter).filter { it.isNotBlank() }
    }

    @JvmStatic
    fun md5string(s: String): String {
        return s.encodeUtf8().md5().hex()
    }

    @JvmStatic
    fun strip(str: CharSequence?): CharSequence {
        // TODO: remove this function once Kotlin conversion of consumers is complete.
        return if (str.isNullOrEmpty()) "" else str.trim()
    }

    @JvmStatic
    fun intToHexStr(i: Int): String {
        return String.format("x%08x", i)
    }

    @JvmStatic
    fun addUnderscores(text: String?): String {
        return text.orEmpty().replace(" ", "_")
    }

    @JvmStatic
    fun removeUnderscores(text: String?): String {
        return text.orEmpty().replace("_", " ")
    }

    @JvmStatic
    fun removeSectionAnchor(text: String?): String {
        text.orEmpty().let {
            return if (it.contains("#")) it.substring(0, it.indexOf("#")) else it
        }
    }

    @JvmStatic
    fun removeNamespace(text: String): String {
        return if (text.length > text.indexOf(":")) {
            text.substring(text.indexOf(":") + 1)
        } else {
            text
        }
    }

    @JvmStatic
    fun removeHTMLTags(text: String?): String {
        return fromHtml(text).toString()
    }

    @JvmStatic
    fun removeStyleTags(text: String): String {
        return text.replace("<style.*?</style>".toRegex(), "")
    }

    @JvmStatic
    fun removeCiteMarkup(text: String): String {
        return text.replace("<cite.*?>".toRegex(), "").replace("</cite>".toRegex(), "")
    }

    @JvmStatic
    fun sanitizeAbuseFilterCode(code: String): String {
        return code.replace("[⧼⧽]".toRegex(), "")
    }

    @JvmStatic
    fun normalizedEquals(str1: String?, str2: String?): Boolean {
        return if (str1 == null || str2 == null) {
            str1 == null && str2 == null
        } else (Normalizer.normalize(str1, Normalizer.Form.NFC)
                == Normalizer.normalize(str2, Normalizer.Form.NFC))
    }

    @JvmStatic
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

    @JvmStatic
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
            pos = parentText.indexOf(words[words.size - 1])
        }
        if (pos >= 0) {
            // TODO: Programmatic selection doesn't seem to work with RTL content...
            editText.setSelection(pos, pos + words[words.size - 1].length)
            editText.performLongClick()
        }
    }

    @JvmStatic
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

    @JvmStatic
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

    @JvmStatic
    fun listToJsonArrayString(list: List<String>): String {
        return JSONArray(list).toString()
    }

    @JvmStatic
    fun stringToListMapToJSONString(map: Map<String, List<Int>>): String {
        return Gson().toJson(map)
    }

    @JvmStatic
    fun listToJSONString(list: List<Int>): String {
        return Gson().toJson(list)
    }

    fun userPageTitleFromName(userName: String, wiki: WikiSite): PageTitle {
        return PageTitle(UserAliasData.valueFor(wiki.languageCode()), userName, wiki)
    }
}
