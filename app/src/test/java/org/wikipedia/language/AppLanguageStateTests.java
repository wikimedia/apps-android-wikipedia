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
        Assert.assertEquals("en", WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCode());
    }

    @Test public void testAddAppLanguage() {
        WikipediaApp.Companion.getInstance().getLanguageState().addAppLanguageCode("ja");
        Assert.assertEquals("en", WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCode());
        Assert.assertEquals(2, WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCodes().size());
        Assert.assertEquals("ja", WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCodes().get(1));
    }

    @SuppressWarnings("checkstyle:magicnumber")
    @Test public void testAddMultipleAppLanguages() {
        List<String> list = new ArrayList<>();
        list.add("en");
        list.add("ja");
        list.add("es");
        list.add("zh-hant");
        WikipediaApp.Companion.getInstance().getLanguageState().setAppLanguageCodes(list);
        Assert.assertTrue(WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCode().equals("en")
                && WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCodes().size() == 4);
    }

    @Test public void testRemoveAppLanguages() {
        List<String> list = new ArrayList<>();
        list.add("en");
        list.add("ja");
        list.add("es");
        list.add("zh-hant");
        WikipediaApp.Companion.getInstance().getLanguageState().setAppLanguageCodes(list);

        List<String> listToRemove = new ArrayList<>();
        listToRemove.add("en");
        listToRemove.add("zh-hant");
        WikipediaApp.Companion.getInstance().getLanguageState().removeAppLanguageCodes(listToRemove);
        Assert.assertEquals("ja", WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCode());
        Assert.assertEquals(2, WikipediaApp.Companion.getInstance().getLanguageState().getAppLanguageCodes().size());
    }
}
