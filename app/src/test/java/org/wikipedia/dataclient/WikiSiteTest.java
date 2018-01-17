package org.wikipedia.dataclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageTitle;
import org.wikipedia.test.TestParcelUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(RobolectricTestRunner.class) public class WikiSiteTest {
    @Test public void testSupportedAuthority() {
        assertThat(WikiSite.supportedAuthority("fr.wikipedia.org"), is(true));
        assertThat(WikiSite.supportedAuthority("fr.m.wikipedia.org"), is(true));
        assertThat(WikiSite.supportedAuthority("roa-rup.wikipedia.org"), is(true));

        assertThat(WikiSite.supportedAuthority("google.com"), is(false));
    }

    @Test public void testForLanguageCodeScheme() {
        WikiSite subject = WikiSite.forLanguageCode("test");
        assertThat(subject.scheme(), is("https"));
    }

    @Test public void testForLanguageCodeAuthority() {
        WikiSite subject = WikiSite.forLanguageCode("test");
        assertThat(subject.authority(), is("test.wikipedia.org"));
    }

    @Test public void testForLanguageCodeLanguage() {
        WikiSite subject = WikiSite.forLanguageCode("test");
        assertThat(subject.languageCode(), is("test"));
    }

    @Test public void testForLanguageCodeNoLanguage() {
        WikiSite subject = WikiSite.forLanguageCode("");
        assertThat(subject.languageCode(), is(""));
    }

    @Test public void testForLanguageCodeNoLanguageAuthority() {
        WikiSite subject = WikiSite.forLanguageCode("");
        assertThat(subject.authority(), is("wikipedia.org"));
    }

    @Test public void testForLanguageCodeLanguageAuthority() {
        WikiSite subject = WikiSite.forLanguageCode("zh-hans");
        assertThat(subject.authority(), is("zh.wikipedia.org"));
        assertThat(subject.languageCode(), is("zh-hans"));
    }

    @Test public void testCtorScheme() {
        WikiSite subject = new WikiSite("http://wikipedia.org");
        assertThat(subject.scheme(), is("http"));
    }

    @Test public void testCtorDefaultScheme() {
        WikiSite subject = new WikiSite("wikipedia.org");
        assertThat(subject.scheme(), is("https"));
    }

    @Test public void testCtorAuthority() {
        WikiSite subject = new WikiSite("test.wikipedia.org");
        assertThat(subject.authority(), is("test.wikipedia.org"));
    }

    @Test public void testCtorAuthorityLanguage() {
        WikiSite subject = new WikiSite("test.wikipedia.org");
        assertThat(subject.languageCode(), is("test"));
    }

    @Test public void testCtorAuthorityNoLanguage() {
        WikiSite subject = new WikiSite("wikipedia.org");
        assertThat(subject.languageCode(), is(""));
    }

    @Test public void testCtorMobileAuthorityLanguage() {
        WikiSite subject = new WikiSite("test.m.wikipedia.org");
        assertThat(subject.languageCode(), is("test"));
    }

    @Test public void testCtorMobileAuthorityNoLanguage() {
        WikiSite subject = new WikiSite("m.wikipedia.org");
        assertThat(subject.languageCode(), is(""));
    }

    @Test public void testCtorUriLangVariant() {
        WikiSite subject = new WikiSite("zh.wikipedia.org/zh-hant/Foo");
        assertThat(subject.authority(), is("zh.wikipedia.org"));
        assertThat(subject.mobileAuthority(), is("zh.m.wikipedia.org"));
        assertThat(subject.subdomain(), is("zh"));
        assertThat(subject.languageCode(), is("zh-hant"));
        assertThat(subject.scheme(), is("https"));
        assertThat(subject.dbName(), is("zhwiki"));
        assertThat(subject.url(), is("https://zh.wikipedia.org"));
    }

    @Test public void testCtorMobileUriLangVariant() {
        WikiSite subject = new WikiSite("zh.m.wikipedia.org/zh-hant/Foo");
        assertThat(subject.authority(), is("zh.m.wikipedia.org"));
        assertThat(subject.mobileAuthority(), is("zh.m.wikipedia.org"));
        assertThat(subject.subdomain(), is("zh"));
        assertThat(subject.languageCode(), is("zh-hant"));
        assertThat(subject.scheme(), is("https"));
        assertThat(subject.url(), is("https://zh.m.wikipedia.org"));
    }

    @Test public void testCtorUriNoLangVariant() {
        WikiSite subject = new WikiSite("http://zh.wikipedia.org/wiki/Foo");
        assertThat(subject.authority(), is("zh.wikipedia.org"));
        assertThat(subject.mobileAuthority(), is("zh.m.wikipedia.org"));
        assertThat(subject.subdomain(), is("zh"));
        assertThat(subject.languageCode(), is("zh"));
        assertThat(subject.scheme(), is("http"));
        assertThat(subject.url(), is("http://zh.wikipedia.org"));
    }

    @Test public void testCtorParcel() throws Throwable {
        WikiSite subject = WikiSite.forLanguageCode("test");
        TestParcelUtil.test(subject);
    }

    @Test public void testAuthority() {
        WikiSite subject = new WikiSite("test.wikipedia.org", "test");
        assertThat(subject.authority(), is("test.wikipedia.org"));
    }

    @Test public void testMobileAuthorityLanguage() {
        WikiSite subject = WikiSite.forLanguageCode("fiu-vro");
        assertThat(subject.mobileAuthority(), is("fiu-vro.m.wikipedia.org"));
    }

    @Test public void testMobileAuthorityNoLanguage() {
        WikiSite subject = new WikiSite("wikipedia.org");
        assertThat(subject.mobileAuthority(), is("m.wikipedia.org"));
    }

    @Test public void testMobileAuthorityLanguageAuthority() {
        WikiSite subject = new WikiSite("no.wikipedia.org", "nb");
        assertThat(subject.mobileAuthority(), is("no.m.wikipedia.org"));
    }

    @Test public void testMobileAuthorityMobileAuthority() {
        WikiSite subject = new WikiSite("ru.m.wikipedia.org");
        assertThat(subject.mobileAuthority(), is("ru.m.wikipedia.org"));
    }

    @Test public void testMobileAuthorityMobileAuthorityNoLanguage() {
        WikiSite subject = new WikiSite("m.wikipedia.org");
        assertThat(subject.mobileAuthority(), is("m.wikipedia.org"));
    }

    @Test public void testDbNameLanguage() {
        WikiSite subject = new WikiSite("en.wikipedia.org", "en");
        assertThat(subject.dbName(), is("enwiki"));
    }

    @Test public void testDbNameSpecialLanguage() {
        WikiSite subject = new WikiSite("no.wikipedia.org", "nb");
        assertThat(subject.dbName(), is("nowiki"));
    }

    @Test public void testPath() {
        WikiSite subject = WikiSite.forLanguageCode("test");
        assertThat(subject.path("Segment"), is("/w/Segment"));
    }

    @Test public void testPathEmpty() {
        WikiSite subject = WikiSite.forLanguageCode("test");
        assertThat(subject.path(""), is("/w/"));
    }

    @Test public void testUrl() {
        WikiSite subject = new WikiSite("test.192.168.1.11:8080", "test");
        assertThat(subject.url(), is("https://test.192.168.1.11:8080"));
    }

    @Test public void testUrlPath() {
        WikiSite subject = WikiSite.forLanguageCode("test");
        assertThat(subject.url("Segment"), is("https://test.wikipedia.org/w/Segment"));
    }

    @Test public void testLanguageCode() {
        WikiSite subject = WikiSite.forLanguageCode("lang");
        assertThat(subject.languageCode(), is("lang"));
    }

    @Test public void testUnmarshal() {
        WikiSite wiki = WikiSite.forLanguageCode("test");
        assertThat(GsonUnmarshaller.unmarshal(WikiSite.class, GsonMarshaller.marshal(wiki)), is(wiki));
    }

    @Test public void testUnmarshalScheme() {
        WikiSite wiki = new WikiSite("wikipedia.org", "");
        assertThat(GsonUnmarshaller.unmarshal(WikiSite.class, GsonMarshaller.marshal(wiki)), is(wiki));
    }

    @Test public void testTitleForInternalLink() {
        WikiSite wiki = WikiSite.forLanguageCode("en");
        assertThat(new PageTitle("Main Page", wiki), is(wiki.titleForInternalLink("")));
        assertThat(new PageTitle("Main Page", wiki), is(wiki.titleForInternalLink("/wiki/")));
        assertThat(new PageTitle("wiki", wiki), is(wiki.titleForInternalLink("wiki")));
        assertThat(new PageTitle("wiki", wiki), is(wiki.titleForInternalLink("/wiki/wiki")));
        assertThat(new PageTitle("wiki/wiki", wiki), is(wiki.titleForInternalLink("/wiki/wiki/wiki")));
    }

    @Test public void testEquals() {
        assertThat(WikiSite.forLanguageCode("en"), is(WikiSite.forLanguageCode("en")));

        assertThat(WikiSite.forLanguageCode("ta"), not(WikiSite.forLanguageCode("en")));
        assertThat(WikiSite.forLanguageCode("ta").equals("ta.wikipedia.org"), is(false));
    }

    @Test public void testNormalization() {
        assertThat("bm.wikipedia.org", is(WikiSite.forLanguageCode("bm").authority()));
    }
}
