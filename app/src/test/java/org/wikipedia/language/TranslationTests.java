package org.wikipedia.language;

import androidx.annotation.NonNull;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("checkstyle:magicnumber")
public class TranslationTests {
    private static final File RES_BASE = new File("src/main/res/");
    private static final String STRINGS_DIRECTORY = "values";
    private static final String STRINGS_XML_NAME = "strings.xml";

    /** Add more if needed, but then also add some tests. */
    private static final String[] POSSIBLE_PARAMS = new String[] {"%s", "%1$s", "%2$s", "%3$s",
                                                                "%d", "%1$d", "%2$d", "%3$d",
                                                                "%.2f", "%1$.2f", "%2$.2f", "%3$.2f",
                                                                "^1"};
    private static final String[] UNSUPPORTED_TEXTS_REGEX = new String[] {"\\{\\{.*?\\}\\}",
                                                                        "\\[\\[.*?\\]\\]",
                                                                        "\\*\\*.*?\\*\\*",
                                                                        "''.*?''",
                                                                        "\\[.*?\\]"};
    private static final String[] BAD_NAMES = new String[]{"ldrtl", "sw360dp", "sw600dp", "sw720dp", "v19", "v21", "v23", "land"};

    private static File BASE_FILE;
    private static File QQ_FILE;
    private static File[] ALL_FILES;

    @Test
    public void testAllTranslations() throws Throwable {

        StringBuilder mismatches = new StringBuilder();

        // Step 1: collect counts of parameters in en/strings.xml
        Map<String, List<Integer>> baseMap = findMatchedParamsInXML(getBaseFile(), POSSIBLE_PARAMS, true);

        // Step 2: finding parameters in other languages
        for (File dir : getAllFiles()) {
            String lang = dir.getName().contains("-") ? dir.getName().substring(dir.getName().indexOf("-") + 1) : "en";
            File targetStringsXml = new File(dir, STRINGS_XML_NAME);
            Map<String, List<Integer>> targetMap = findMatchedParamsInXML(targetStringsXml, POSSIBLE_PARAMS, true);

            // compare the counts inside the maps
            targetMap.forEach((targetKey, targetList) -> {
                List<Integer> baseList = baseMap.get(targetKey);
                if (baseList != null && !baseList.equals(targetList)) {
                    mismatches.append("Parameters mismatched in ")
                            .append(lang)
                            .append("/")
                            .append(STRINGS_XML_NAME).append(": ")
                            .append(targetKey).append(" \n");
                }
            });
        }

        // Step 3: check the result
        assertThat("\n" + mismatches.toString(), mismatches.length(), is(0));
    }

    @Test
    public void testTranslateWikiQQ() throws Throwable {

        StringBuilder mismatches = new StringBuilder();

        // Step 1: collect all items in en/strings.xml
        List<String> baseList = findStringItemInXML(getBaseFile(), "string", "plurals");

        // Step 2: collect all items in qq/strings.xml
        List<String> qqList = findStringItemInXML(getQQFile(), "string", "plurals");

        // Step 3: check if item exists in qq/strings.xml
        for (String item : baseList) {
            if (!qqList.contains(item)) {
                mismatches.append("Missing item in qq/strings.xml ")
                        .append(item).append(" \n");
            }
        }

        // Step 4: check the result
        assertThat("\n" + mismatches.toString(), mismatches.length(), is(0));
    }

    @Test
    public void testPluralsDeclaration() throws Throwable {

        StringBuilder mismatches = new StringBuilder();

        List<String> baseList = findStringItemInXML(getBaseFile(), "plurals");

        for (File dir : getAllFiles()) {
            String lang = dir.getName().contains("-") ? dir.getName().substring(dir.getName().indexOf("-") + 1) : "en";
            File targetStringsXml = new File(dir, STRINGS_XML_NAME);
            List<String> targetList = findStringItemInXML(targetStringsXml, "plurals");

            targetList.forEach(targetKey -> {
                if (!baseList.contains(targetKey)) {
                    mismatches.append("Plurals item has no declaration in the base values folder in ")
                            .append(lang)
                            .append("/")
                            .append(STRINGS_XML_NAME).append(": ")
                            .append(targetKey).append(" \n");
                }
            });
        }

        assertThat("\n" + mismatches.toString(), mismatches.length(), is(0));
    }

    @Test
    public void testUnsupportedTexts() throws Throwable {

        StringBuilder mismatches = new StringBuilder();

        // Step 1: collect counts of parameters in en/strings.xml
        Map<String, List<Integer>> baseMap = findMatchedParamsInXML(getBaseFile(), UNSUPPORTED_TEXTS_REGEX, false);

        // Step 2: finding parameters in other languages
        for (File dir : getAllFiles()) {
            String lang = dir.getName().contains("-") ? dir.getName().substring(dir.getName().indexOf("-") + 1) : "en";
            // Skip "qq" since it contains a lot of {{Identical}} tags
            if (!lang.equals("qq")) {
                File targetStringsXml = new File(dir, STRINGS_XML_NAME);
                Map<String, List<Integer>> targetMap = findMatchedParamsInXML(targetStringsXml, UNSUPPORTED_TEXTS_REGEX, false);

                // compare the counts inside the maps
                targetMap.forEach((targetKey, targetList) -> {
                    List<Integer> baseList = baseMap.get(targetKey);
                    if (baseList != null && !baseList.equals(targetList)) {
                        mismatches.append("Unsupported Wikitext/Markdown in ")
                                .append(lang)
                                .append("/")
                                .append(STRINGS_XML_NAME).append(": ")
                                .append(targetKey).append(" \n");
                    }
                });
            }
        }

        // Step 3: check the result
        assertThat("\n" + mismatches.toString(), mismatches.length(), is(0));
    }

    private File getBaseFile() {
        if (BASE_FILE == null) {
            BASE_FILE = new File(RES_BASE + "/" + STRINGS_DIRECTORY, STRINGS_XML_NAME);
        }
        return BASE_FILE;
    }

    private File getQQFile() {
        if (QQ_FILE == null) {
            QQ_FILE = new File(RES_BASE + "/" + STRINGS_DIRECTORY + "-qq", STRINGS_XML_NAME);
        }
        return QQ_FILE;
    }

    private File[] getAllFiles() {
        if (ALL_FILES == null) {
            ALL_FILES = RES_BASE.listFiles((File pathname) -> pathname.isDirectory() && pathname.getName().startsWith(STRINGS_DIRECTORY) && !hasBadName(pathname));
            if (ALL_FILES != null) {
                Arrays.sort(ALL_FILES);
            }
        }
        return ALL_FILES;
    }

    private boolean hasBadName(File pathname) {
        for (String name : BAD_NAMES) {
            if (pathname.getName().startsWith(STRINGS_DIRECTORY + "-" + name)) {
                return true;
            }
        }
        return false;
    }

    private List<String> findStringItemInXML(@NonNull File xmlPath, @NonNull String ...strings) throws Throwable {
        List<String> list = new ArrayList<>();
        Document document = Jsoup.parse(xmlPath, "UTF-8");

        for (String string : strings) {
            Elements elements = document.select(string);
            for (Element element : elements) {
                String name = element.attr("name");
                list.add(name);
            }

        }
        return list;
    }

    private Map<String, List<Integer>> findMatchedParamsInXML(@NonNull File xmlPath, @NonNull String[] params, boolean quote) throws Throwable {
        Map<String, List<Integer>> map = new HashMap<>();
        Document document = Jsoup.parse(xmlPath, "UTF-8");

        // For string items
        // <string name="app_name_prod">Wikipedia</string>
        Elements stringElements = document.select("string");
        for (Element element : stringElements) {
            String name = element.attr("name");
            String value = element.text();

            List<Integer> countList = new ArrayList<>();
            for (String param : params) {
                int count = 0;
                Pattern pattern = Pattern.compile(quote ? Pattern.quote(param) : param);
                Matcher matcher = pattern.matcher(value);
                while (matcher.find()) {
                    count++;
                }
                countList.add(count);
            }
            map.put(name, countList);
        }

        // For plural items
        // <plurals name="diff_years">
        //     <item quantity="one">Last year</item>
        //     <item quantity="other">%d years ago</item>
        // </plurals>
        Elements pluralsElements = document.select("plurals");
        for (Element element : pluralsElements) {
            String name = element.attr("name");
            Elements pluralElements = element.select("item");
            for (Element subElement : pluralElements) {
                String subName = subElement.attr("quantity");
                String subValue = subElement.text();
                if (subName.equals("one")) {
                    continue;
                }

                List<Integer> countList = new ArrayList<>();
                for (String param : params) {
                    int count = 0;
                    Pattern pattern = Pattern.compile(quote ? Pattern.quote(param) : param);
                    Matcher matcher = pattern.matcher(subValue);
                    while (matcher.find()) {
                        count++;
                    }
                    countList.add(count);
                }
                map.put(name + "[" + subName + "]", countList);
            }
        }

        return map;
    }
}
