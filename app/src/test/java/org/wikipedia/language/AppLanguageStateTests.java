package org.wikipedia.language;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppLanguageStateTests {

    @Test public void testInitAppLanguages() {
        Assert.assertTrue(WikipediaApp.instance.languageState.getAppLanguageCode().equals("en"));
    }

    @Test public void testAddAppLanguage() {
        WikipediaApp.instance.languageState.addAppLanguageCode("ja");
        Assert.assertTrue(WikipediaApp.instance.languageState.getAppLanguageCode().equals("en"));
        Assert.assertTrue(WikipediaApp.instance.languageState.getAppLanguageCodes().size() == 2);
        Assert.assertTrue(WikipediaApp.instance.languageState.getAppLanguageCodes().get(1).equals("ja"));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test public void testAddMultipleAppLanguages() {
        List<String> list = new ArrayList<>();
        list.add("en");
        list.add("ja");
        list.add("es");
        list.add("zh-hant");
        WikipediaApp.instance.languageState.setAppLanguageCodes(list);
        Assert.assertTrue(WikipediaApp.instance.languageState.getAppLanguageCode().equals("en")
                && WikipediaApp.instance.languageState.getAppLanguageCodes().size() == 4);
    }

    @Test public void testRemoveAppLanguages() {
        List<String> list = new ArrayList<>();
        list.add("en");
        list.add("ja");
        list.add("es");
        list.add("zh-hant");
        WikipediaApp.instance.languageState.setAppLanguageCodes(list);

        List<String> listToRemove = new ArrayList<>();
        listToRemove.add("en");
        listToRemove.add("zh-hant");
        WikipediaApp.instance.languageState.removeAppLanguageCodes(listToRemove);
        Assert.assertTrue(WikipediaApp.instance.languageState.getAppLanguageCode().equals("ja"));
        Assert.assertTrue(WikipediaApp.instance.languageState.getAppLanguageCodes().size() == 2);
    }
}
