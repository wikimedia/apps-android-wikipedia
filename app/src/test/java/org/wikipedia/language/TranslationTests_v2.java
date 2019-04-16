package org.wikipedia.language;

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

import io.reactivex.annotations.NonNull;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SuppressWarnings("checkstyle:magicnumber")
public class TranslationTests_v2 {
    private static File RES_BASE = new File("src/main/res/");
    private static String STRINGS_DIRECTORY = "values";
    private static String STRINGS_XML_NAME = "strings.xml";

    /** Add more if needed, but then also add some tests. */
    private static final String[] POSSIBLE_PARAMS = new String[] {"%s", "%1$s", "%2$s", "%d", "%1$d", "%2$d", "%.2f", "%1$.2f", "%2$.2f", "%3$.2f", "^1"};

    private static final String[] BAD_NAMES = new String[]{"ldrtl", "sw360dp", "sw600dp", "sw720dp", "v19", "v21", "v23", "land"};

    private StringBuilder mismatches = new StringBuilder();

    @Test public void testAllTranslations() throws Throwable {

        File baseStringsXml = new File(RES_BASE + "/" + STRINGS_DIRECTORY, STRINGS_XML_NAME);
        // Step 1: collect counts of parameters in en/strings.xml
        Map<String, List<Integer>> baseMap = xmlReader(baseStringsXml);


        // Step 2: finding parameters in other languages
        File[] resDirs = RES_BASE.listFiles((File pathname) -> pathname.isDirectory() && pathname.getName().startsWith(STRINGS_DIRECTORY) && !hasBadName(pathname));
        Arrays.sort(resDirs);
        for (File dir : resDirs) {
            String lang = dir.getName().contains("-") ? dir.getName().substring(dir.getName().indexOf("-") + 1) : "en";
            File targetStringsXml = new File(dir, STRINGS_XML_NAME);
            Map<String, List<Integer>> targetMap = xmlReader(targetStringsXml);

            // compare the counts inside the maps
            targetMap.forEach((targetKey, targetList) -> {
                List<Integer> baseList = baseMap.get(targetKey);
                if (baseList != null && !baseList.equals(targetList)) {
                    mismatches.append("Parameters mismatched in " + lang + "/" + STRINGS_XML_NAME + ": " + targetKey + " \n");
                }
            });
        }

        // Step ?
        assertThat("\n" + mismatches.toString(), mismatches.length(), is(0));
    }

    private boolean hasBadName(File pathname) {
        for (String name : BAD_NAMES) {
            if (pathname.getName().startsWith(STRINGS_DIRECTORY + "-" + name)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<Integer>> xmlReader(@NonNull File xmlPath) throws Throwable{
        Map<String, List<Integer>> map = new HashMap<>();
        Document document = Jsoup.parse(xmlPath, "UTF-8");

        // For string items: <string name="app_name_prod">Wikipedia</string>
        Elements stringElements = document.select("string");
        for (Element element : stringElements) {
            String name = element.attr("name");
            String value = element.text();

            List<Integer> countList = new ArrayList<>();
            for (String param : POSSIBLE_PARAMS) {
                int count = 0;
                Pattern pattern = Pattern.compile(Pattern.quote(param));
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

                List<Integer> countList = new ArrayList<>();
                for (String param : POSSIBLE_PARAMS) {
                    int count = 0;
                    Pattern pattern = Pattern.compile(Pattern.quote(param));
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
