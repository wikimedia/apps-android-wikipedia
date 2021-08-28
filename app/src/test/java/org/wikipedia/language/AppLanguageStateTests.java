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
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageState().getAppLanguageCode().equals("en"));
    }

    @Test public void testAddAppLanguage() {
        WikipediaApp.getInstance().getAppLanguageState().addAppLanguageCode("ja");
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageState().getAppLanguageCode().equals("en"));
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageState().getAppLanguageCodes().size() == 2);
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageState().getAppLanguageCodes().get(1).equals("ja"));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test public void testAddMultipleAppLanguages() {
        List<String> list = new ArrayList<>();
        list.add("en");
        list.add("ja");
        list.add("es");
        list.add("zh-hant");
        WikipediaApp.getInstance().getAppLanguageState().setAppLanguageCodes(list);
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageState().getAppLanguageCode().equals("en")
                && WikipediaApp.getInstance().getAppLanguageState().getAppLanguageCodes().size() == 4);
    }

    @Test public void testRemoveAppLanguages() {
        List<String> list = new ArrayList<>();
        list.add("en");
        list.add("ja");
        list.add("es");
        list.add("zh-hant");
        WikipediaApp.getInstance().getAppLanguageState().setAppLanguageCodes(list);

        List<String> listToRemove = new ArrayList<>();
        listToRemove.add("en");
        listToRemove.add("zh-hant");
        WikipediaApp.getInstance().getAppLanguageState().removeAppLanguageCodes(listToRemove);
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageState().getAppLanguageCode().equals("ja"));
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageState().getAppLanguageCodes().size() == 2);
    }
}
