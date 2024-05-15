package org.wikipedia.language

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.jsoup.Jsoup
import org.junit.Test
import java.io.File
import java.util.function.Consumer
import java.util.regex.Pattern

class TranslationTests {
    @Test
    @Throws(Throwable::class)
    fun testAllTranslations() {
        val mismatches = StringBuilder()

        // Step 1: collect counts of parameters in en/strings.xml
        val baseMap = findMatchedParamsInXML(
            baseFile, POSSIBLE_PARAMS, true
        )

        // Step 2: finding parameters in other languages
        for (dir in allFiles) {
            val lang =
                if (dir.name.contains("-")) dir.name.substring(dir.name.indexOf("-") + 1) else "en"
            val targetStringsXml = File(dir, STRINGS_XML_NAME)
            val targetMap = findMatchedParamsInXML(targetStringsXml, POSSIBLE_PARAMS, true)

            // compare the counts inside the maps
            targetMap.forEach { (targetKey, targetList) ->
                val baseList = baseMap[targetKey]
                if (baseList != null && baseList != targetList) {
                    mismatches.append("Parameters mismatched in ")
                        .append(lang)
                        .append("/")
                        .append(STRINGS_XML_NAME).append(": ")
                        .append(targetKey).append(" \n")
                }
            }
        }

        // Step 3: check the result
        MatcherAssert.assertThat("\n" + mismatches.toString(), mismatches.length, Matchers.`is`(0))
    }

    @Test
    @Throws(Throwable::class)
    fun testTranslateWikiQQ() {
        val mismatches = StringBuilder()

        // Step 1: collect all items in en/strings.xml
        val baseList = findStringItemInXML(baseFile, "string", "plurals")

        // Step 2: collect all items in qq/strings.xml
        val qqList = findStringItemInXML(qQFile, "string", "plurals")

        // Step 3: check if item exists in qq/strings.xml
        for (item in baseList) {
            if (!qqList.contains(item)) {
                mismatches.append("Missing item in qq/strings.xml ")
                    .append(item).append(" \n")
            }
        }

        // Step 4: check the result
        MatcherAssert.assertThat("\n" + mismatches.toString(), mismatches.length, Matchers.`is`(0))
    }

    @Test
    @Throws(Throwable::class)
    fun testPluralsDeclaration() {
        val mismatches = StringBuilder()

        val baseList = findStringItemInXML(baseFile, "plurals")

        for (dir in allFiles) {
            val lang =
                if (dir.name.contains("-")) dir.name.substring(dir.name.indexOf("-") + 1) else "en"
            val targetStringsXml = File(dir, STRINGS_XML_NAME)
            val targetList = findStringItemInXML(targetStringsXml, "plurals")

            targetList.forEach(Consumer { targetKey ->
                if (!baseList.contains(targetKey)) {
                    mismatches.append("Plurals item has no declaration in the base values folder in ")
                        .append(lang)
                        .append("/")
                        .append(STRINGS_XML_NAME).append(": ")
                        .append(targetKey).append(" \n")
                }
            })
        }

        MatcherAssert.assertThat("\n" + mismatches.toString(), mismatches.length, Matchers.`is`(0))
    }

    @Test
    @Throws(Throwable::class)
    fun testUnsupportedTexts() {
        val mismatches = StringBuilder()

        // Step 1: collect counts of parameters in en/strings.xml
        val baseMap = findMatchedParamsInXML(baseFile, UNSUPPORTED_TEXTS_REGEX, false)

        // Step 2: finding parameters in other languages
        for (dir in allFiles) {
            val lang =
                if (dir.name.contains("-")) dir.name.substring(dir.name.indexOf("-") + 1) else "en"
            // Skip "qq" since it contains a lot of {{Identical}} tags
            if (lang != "qq") {
                val targetStringsXml = File(dir, STRINGS_XML_NAME)
                val targetMap =
                    findMatchedParamsInXML(targetStringsXml, UNSUPPORTED_TEXTS_REGEX, false)

                // compare the counts inside the maps
                targetMap.forEach { (targetKey: String, targetList: List<Int>) ->
                    val baseList = baseMap[targetKey]
                    if (baseList != null && baseList != targetList) {
                        mismatches.append("Unsupported Wikitext/Markdown in ")
                            .append(lang)
                            .append("/")
                            .append(STRINGS_XML_NAME).append(": ")
                            .append(targetKey).append(" \n")
                    }
                }
            }
        }

        // Step 3: check the result
        MatcherAssert.assertThat("\n" + mismatches.toString(), mismatches.length, Matchers.`is`(0))
    }

    private val baseFile: File
        get() {
            if (BASE_FILE == null) {
                BASE_FILE = File("$RES_BASE/$STRINGS_DIRECTORY", STRINGS_XML_NAME)
            }
            return BASE_FILE!!
        }

    private val qQFile: File
        get() {
            if (QQ_FILE == null) {
                QQ_FILE = File("$RES_BASE/$STRINGS_DIRECTORY-qq", STRINGS_XML_NAME)
            }
            return QQ_FILE!!
        }

    private val allFiles: Array<File>
        get() {
            if (ALL_FILES == null) {
                ALL_FILES = RES_BASE.listFiles { pathName ->
                    pathName.isDirectory && pathName.name.startsWith(STRINGS_DIRECTORY) && !hasBadName(pathName)
                }
                ALL_FILES?.sort()
            }
            return ALL_FILES!!
        }

    private fun hasBadName(pathname: File): Boolean {
        return BAD_NAMES.any { pathname.name.startsWith("$STRINGS_DIRECTORY-$it") }
    }

    @Throws(Throwable::class)
    private fun findStringItemInXML(xmlPath: File, vararg strings: String): List<String> {
        val list = mutableListOf<String>()
        val document = Jsoup.parse(xmlPath, "UTF-8")

        for (string in strings) {
            val elements = document.select(string)
            for (element in elements) {
                val name = element.attr("name")
                list.add(name)
            }
        }
        return list
    }

    @Throws(Throwable::class)
    private fun findMatchedParamsInXML(xmlPath: File, params: List<String>, quote: Boolean): Map<String, List<Int>> {
        val map = mutableMapOf<String, List<Int>>()
        val document = Jsoup.parse(xmlPath, "UTF-8")

        // For string items
        // <string name="app_name_prod">Wikipedia</string>
        val stringElements = document.select("string")
        for (element in stringElements) {
            val name = element.attr("name")
            val value = element.text()

            // Exclude pre-packaged messages for use with the patrolling feature, since they are
            // intended to contain wikitext.
            // TODO: test these messages separately and more thoroughly.
            if (name.startsWith("patroller_saved_message_body")) {
                continue
            }

            val countList = mutableListOf<Int>()
            for (param in params) {
                var count = 0
                val pattern = Pattern.compile(if (quote) Pattern.quote(param) else param)
                val matcher = pattern.matcher(value)
                while (matcher.find()) {
                    count++
                }
                countList.add(count)
            }
            map[name] = countList
        }

        // For plural items
        // <plurals name="diff_years">
        //     <item quantity="one">Last year</item>
        //     <item quantity="other">%d years ago</item>
        // </plurals>
        val pluralsElements = document.select("plurals")
        for (element in pluralsElements) {
            val name = element.attr("name")
            val pluralElements = element.select("item")
            for (subElement in pluralElements) {
                val subName = subElement.attr("quantity")
                val subValue = subElement.text()
                if (subName == "one") {
                    continue
                }

                val countList = mutableListOf<Int>()
                for (param in params) {
                    var count = 0
                    val pattern = Pattern.compile(if (quote) Pattern.quote(param) else param)
                    val matcher = pattern.matcher(subValue)
                    while (matcher.find()) {
                        count++
                    }
                    countList.add(count)
                }
                map["$name[$subName]"] = countList
            }
        }

        return map
    }

    companion object {
        private val RES_BASE = File("src/main/res/")
        private const val STRINGS_DIRECTORY = "values"
        private const val STRINGS_XML_NAME = "strings.xml"

        /** Add more if needed, but then also add some tests.  */
        private val POSSIBLE_PARAMS = listOf(
            "%s", "%1\$s", "%2\$s", "%3\$s",
            "%d", "%1\$d", "%2\$d", "%3\$d",
            "%.2f", "%1$.2f", "%2$.2f", "%3$.2f",
            "^1"
        )
        private val UNSUPPORTED_TEXTS_REGEX = listOf(
            "\\{\\{.*?\\}\\}",
            "\\[\\[.*?\\]\\]",
            "\\*\\*.*?\\*\\*",
            "''.*?''"
        )
        private val BAD_NAMES = listOf("ldrtl", "sw360dp", "sw600dp", "sw720dp", "v19", "v21", "v23", "land", "night")

        private var BASE_FILE: File? = null
        private var QQ_FILE: File? = null
        private var ALL_FILES: Array<File>? = null
    }
}
