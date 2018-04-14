package org.wikipedia.language;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.WikipediaApp;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppLanguageStateTests {

    @Test public void testInitAppLanguages() {
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageCode().equals("en"));
    }

    @Test public void testAddAppLanguage() {
        WikipediaApp.getInstance().setAppLanguageCode("ja");
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageCode().equals("ja")
                && WikipediaApp.getInstance().getAppLanguageCodes().size() == 2);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test public void testAddMultipleAppLanguages() {
        List<String> list = new ArrayList<>();
        list.add("en");
        list.add("ja");
        list.add("es");
        list.add("zh-hant");
        WikipediaApp.getInstance().setAppLanguageCodes(list);
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageCode().equals("en")
                && WikipediaApp.getInstance().getAppLanguageCodes().size() == 4);
    }

    @Test public void testRemoveAppLanguages() {
        List<String> list = new ArrayList<>();
        list.add("en");
        list.add("ja");
        list.add("es");
        list.add("zh-hant");
        WikipediaApp.getInstance().setAppLanguageCodes(list);

        List<String> listToRemove = new ArrayList<>();
        listToRemove.add("en");
        listToRemove.add("zh-hant");
        WikipediaApp.getInstance().removeAppLanguageCodes(listToRemove);
        Assert.assertTrue(WikipediaApp.getInstance().getAppLanguageCode().equals("ja")
                && WikipediaApp.getInstance().getAppLanguageCodes().size() == 2);
    }
}
