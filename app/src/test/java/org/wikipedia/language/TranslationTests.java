package org.wikipedia.language;

import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.wikipedia.util.log.L;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

/**
 * Tests to make sure that the string resources don't cause any issues. Mainly the goal is to test
 * all translations, but even the default strings are tested.
 *
 * TODO: We could make this cleaner by leveraging regular expressions.  However, this could be
 * challenging because how to detect the boundaries of the substring to test against our patterns
 * may vary by language.  We can't split the string by spaces, for example, because some languages
 * don't use them.
 *
 * TODO: check content_license_html is valid HTML
 */
@SuppressWarnings("checkstyle:magicnumber")
public class TranslationTests {
    private static File RES_BASE = new File("src/main/res/");

    /** Add more if needed, but then also add some tests. */
    private static final String[] POSSIBLE_PARAMS = new String[] {"%s", "%1$s", "%2$s", "%d",
            "%1$d", "%2$d", "%.2f", "%1$.2f", "%2$.2f", "%3$.2f", "^1"};

    private static final String[] BAD_NAMES = new String[] {"ldrtl", "sw600dp", "sw720dp", "v19", "v21", "v23"};

    private StringBuilder mismatches = new StringBuilder();

    @Test public void testAllTranslations() throws Throwable {
        // todo: flag usage of templates {{}}.
        File[] resDirs = RES_BASE.listFiles((File pathname) -> pathname.isDirectory() && pathname.getName().startsWith("values") && !hasBadName(pathname));
        for (File dir : resDirs) {
            String lang = dir.getName().contains("-") ? dir.getName().substring(dir.getName().indexOf("-") + 1) : "en";
            Locale locale = new Locale(lang);
            File strings = new File(dir, "strings.xml");
            Document doc = Jsoup.parse(strings, "UTF-8");

            checkAllStrings(dir.getName(), doc);

            // commented out during the transition from 1 param to 0
//            checkTranslationHasNoParameter(R.string.saved_pages_search_empty_message);
//            checkTranslationHasNoParameter(R.string.history_search_empty_message);

            Map<String, String> hasTags = new StringCollector("<", "&lt;").collect(dir.getName(), doc);
            Map<String, String> hasNoTags = new StringCollector("<", "&lt;").not().collect(dir.getName(), doc);
            Map<String, String> hasStringParams = new StringCollector("%s").collect(dir.getName(), doc);
            Map<String, String> hasDecimalParams = new StringCollector("%d").collect(dir.getName(), doc);
            Map<String, String> hasFloatParams = new StringCollector("%.2f").collect(dir.getName(), doc);
            Map<String, String> hasFloatFirstParams = new StringCollector("%1$.2f").collect(dir.getName(), doc);
            Map<String, String> hasFloatThirdParams = new StringCollector("%3$.2f").collect(dir.getName(), doc);
            Map<String, String> hasTextUtilTemplateParams = new StringCollector("^1").collect(dir.getName(), doc);
            List<Element> hasPlurals = collectPluralResources(doc);

            if (!lang.equals("qq")) {
                for (Map.Entry<String, String> entry : hasTags.entrySet()) {
                    if (entry.getKey().equals("wp_stylized")
                        && (lang.equals("iw") || lang.equals("he") || lang.equals("ckb"))
                            || entry.getKey().equals("notification_talk")
                            || entry.getKey().equals("notification_reverted")
                            || entry.getKey().equals("notification_thanks")) {
                        continue;
                    }
                    expectContains(lang, entry, "<", "&lt;");
                }
                for (Map.Entry<String, String> entry : hasNoTags.entrySet()) {
                    expectNotContains(lang, entry, "<", "&lt;");
                }
                for (Map.Entry<String, String> entry : hasStringParams.entrySet()) {
                    checkTranslationHasParameter(lang, entry, "%s", "[stringParam]", null);
                }
                for (Map.Entry<String, String> entry : hasDecimalParams.entrySet()) {
                    checkTranslationHasParameter(lang, entry, "%d", 42, null);
                }
                for (Map.Entry<String, String> entry : hasFloatParams.entrySet()) {
                    checkTranslationHasParameter(lang, entry, "%.2f", .27f, "0,27");
                }
                for (Map.Entry<String, String> entry : hasFloatFirstParams.entrySet()) {
                    float input = 1.23f;
                    String expected = NumberFormat.getInstance(locale).format(input);
                    if (entry.getValue().contains("%2$.2f")) {
                        testTranslation(lang, entry, expected, input, input);
                    } else {
                        testTranslation(lang, entry, expected, input);
                    }
                }
                for (Map.Entry<String, String> entry : hasFloatThirdParams.entrySet()) {
                    float input = 1.23f;
                    String expected = NumberFormat.getInstance(locale).format(input);
                    testTranslation(lang, entry, expected, null, null, input);
                }
                for (Map.Entry<String, String> entry : hasTextUtilTemplateParams.entrySet()) {
                    checkTranslationHasParameter(lang, entry, "^1", "[templateParam]", null);
                }
                for (Element elem : hasPlurals) {
                    checkPluralHasOther(lang, elem);
                }
            }
        }
        assertThat("\n" + mismatches.toString(), mismatches.length(), is(0));
    }

    private void checkAllStrings(String name, Document doc) throws Throwable {
        new StringCollector().collect(name, doc);
    }

    private void expectNotContains(String lang, Map.Entry<String, String> entry, String... examples) {
        for (String example : examples) {
            if (entry.getValue().contains(example)) {
                final String msg = lang + ":" + entry.getKey() + " = " + entry.getValue() + "' contains " + example;
                L.e(msg);
                mismatches.append(msg).append("\n");
                break;
            }
        }
    }

    private void expectContains(String lang, Map.Entry<String, String> entry, Object... examples) {
        boolean found = false;
        for (Object example : examples) {
            if (entry.getValue().contains(example.toString())) {
                found = true;
                break;
            }
        }
        if (!found) {
            String msg = lang + ":" + entry.getKey() + " = " + entry.getValue() + "' does not contain " + Arrays.toString(examples);
            L.e(msg);
            mismatches.append(msg).append("\n");
        }
    }

    private void checkTranslationHasParameter(String lang, Map.Entry<String, String> entry, String paramName, Object val1, String alternateFormat) {
        String subject = String.format(new Locale(lang), entry.getValue(), val1);
        if (!subject.contains(String.format(paramName, val1))
            && (alternateFormat == null || !subject.contains(alternateFormat))) {
            final String msg = lang + ":" + entry.getKey() + " = " + subject + "' is missing " + val1;
            L.e(msg);
            mismatches.append(msg).append("\n");
        }
    }

    private <T> void testTranslation(String lang, Map.Entry<String, String> entry,
                                     @NonNull CharSequence expected, @NonNull T... input) {
        testTranslation(lang, entry, new CharSequence[] {expected}, input);
    }

    private <T> void testTranslation(String lang, Map.Entry<String, String> entry,
                                     CharSequence[] expectedAny, @NonNull T... input) {
        String subject = String.format(new Locale(lang), entry.getValue(), input);
        String msg = lang + ":" + entry.getKey() + " = \"" + subject + "\"";
        if (StringUtils.indexOfAny(subject, (CharSequence[]) expectedAny) < 0) {
            msg += " is missing any of \"" + Arrays.toString(expectedAny) + "\"";
            L.e(msg);
            mismatches.append(msg).append("\n");
        }
    }

    private void checkPluralHasOther(String lang, Element elem) {
        if (elem.getElementsByAttributeValue("quantity", "other").size() <= 0) {
            final String msg = lang + ":" + elem.attr("name") + " plural is missing 'other'";
            L.e(msg);
            mismatches.append(msg).append("\n");
        }
    }

    private boolean hasBadName(File pathname) {
        for (String name : BAD_NAMES) {
            if (pathname.getName().startsWith("values-" + name)) {
                return true;
            }
        }
        return false;
    }

    private class StringCollector {
        private boolean negate;
        private final String[] paramExamples;

        StringCollector(String... paramExamples) {
            this.paramExamples = paramExamples;
        }

        private StringCollector not() {
            negate = true;
            return this;
        }

        private Map<String, String> collect(String lang, Document doc) throws Throwable {
            Map<String, String> result = new HashMap<>();

            Elements stringElems = doc.select("string");
            for (Element elem : stringElems) {
                String name = elem.attr("name");
                String value = elem.text();

                if (name.startsWith("abc_")
                        || name.startsWith("preference_")
                        // Required after upgrading Support Libraries from v23.0.1 to v23.1.0.
                        || name.equals("character_counter_pattern")
                        || name.startsWith("hockeyapp_")
                        || name.equals("find_in_page_result")) {
                    continue;
                }

                // don't care about appcompat string; and preference string resources don't get translated
                assertParameterFormats(lang, name, value);

                boolean found = findParameter(value);
                if ((!negate && found) || (negate && !found)) {
                    result.put(name, value);
                }

            }
            return result;
        }

        /**
         * If it has a parameter it should be one of POSSIBLE_PARAMS.
         * If not then flag this so we can improve the tests.
         */
        private void assertParameterFormats(String lang, String name, String value) {
            if (value.contains("%")) {
                boolean ok = false;
                int start = value.indexOf('%');
                for (String possible : POSSIBLE_PARAMS) {
                    int end = value.indexOf(getLastChar(possible), start);
                    if (end != -1 && end < value.length()) {
                        String candidate = value.substring(start, end + 1);
                        if (possible.equals(candidate)) {
                            ok = true;
                            break;
                        }
                    }
                }
                if (!ok) {
                    fail("Unexpected format in " +  name + " (" + lang + "): '" + value + "'. Update tests!");
                }
            }
        }

        private String getLastChar(String str) {
            return str.substring(str.length() - 1);
        }

        private boolean findParameter(String value) {
            boolean found = false;
            for (String paramExample : paramExamples) {
                if (value.contains(paramExample)) {
                    found = true;
                    if (!negate) {
                        break;
                    }
                }
            }
            return found;
        }
    }

    private List<Element> collectPluralResources(Document doc) {
        List<Element> result = new ArrayList<>();
        result.addAll(doc.select("plurals"));
        return result;
    }
}
