package org.wikipedia.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class StringUtilTest {
    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void testGetBase26String() {
        assertThat(StringUtil.getBase26String(1), is("A"));
        assertThat(StringUtil.getBase26String(26), is("Z"));
        assertThat(StringUtil.getBase26String(277), is("JQ"));
        assertThat(StringUtil.getBase26String(2777), is("DBU"));
        assertThat(StringUtil.getBase26String(27000), is("AMXL"));
        assertThat(StringUtil.getBase26String(52), is("AZ"));
        assertThat(StringUtil.getBase26String(53), is("BA"));
    }

    @Test
    public void testListToCsv() {
        List<String> stringList = new ArrayList<>();
        assertThat(StringUtil.listToCsv(stringList), is(""));
        stringList.add("one");
        assertThat(StringUtil.listToCsv(stringList), is("one"));
        stringList.add("two");
        assertThat(StringUtil.listToCsv(stringList), is("one,two"));
    }

    @Test
    public void testCsvToList() {
        List<String> stringList = new ArrayList<>();
        stringList.add("one");
        stringList.add("two");
        assertThat(StringUtil.csvToList("one,two"), is(stringList));
        assertThat(StringUtil.csvToList("one").size(), is(1));
        assertThat(StringUtil.csvToList("").size(), is(0));
    }

    @Test
    public void testDelimiterStringToList() {
        List<String> stringList = new ArrayList<>();
        stringList.add("one");
        stringList.add("two");
        assertThat(StringUtil.delimiterStringToList("one,two", ","), is(stringList));
    }

    @Test
    public void testMd5string() {
        assertThat(StringUtil.md5string("test"), is("098f6bcd4621d373cade4e832627b4f6"));
        assertThat(StringUtil.md5string("https://en.wikipedia.org/api/rest_v1/page/mobile-html/Earth"),
                is("0f28e0cfe175f17806979dff54cc7ea6"));
    }

    @Test
    public void testStrip() {
        assertThat(StringUtil.strip("test"), is("test"));
        assertThat(StringUtil.strip(" test  "), is("test"));
    }

    @Test
    public void testIntToHexStr() {
        assertThat(StringUtil.intToHexStr(1), is("x00000001"));
    }

    @Test
    public void testAddUnderscores() {
        assertThat(StringUtil.addUnderscores("te st"), is("te_st"));
    }

    @Test
    public void testRemoveUnderscores() {
        assertThat(StringUtil.removeUnderscores("te_st"), is("te st"));
    }

    @Test
    public void testRemoveSectionAnchor() {
        assertThat(StringUtil.removeSectionAnchor("#te_st"), is(""));
        assertThat(StringUtil.removeSectionAnchor("sec#te_st"), is("sec"));
    }

    @Test
    public void testRemoveHTMLTags() {
        assertThat(StringUtil.removeHTMLTags("<tag>te_st</tag>"), is("te_st"));
    }

    @Test
    public void testRemoveStyleTags() {
        assertThat(StringUtil.removeStyleTags("Lorem <style data=\"123\">test</style> <i>ipsum</i>"), is("Lorem  <i>ipsum</i>"));
    }

    @Test
    public void testRemoveCiteMarkup() {
        assertThat(StringUtil.removeCiteMarkup("Lorem <cite data=\"123\">test</cite> <i>ipsum</i>"), is("Lorem test <i>ipsum</i>"));
    }

    @Test
    public void testSanitizeAbuseFilterCode() {
        assertThat(StringUtil.sanitizeAbuseFilterCode("⧼abusefilter-warning-selfpublished⧽"), is("abusefilter-warning-selfpublished"));
    }
}
