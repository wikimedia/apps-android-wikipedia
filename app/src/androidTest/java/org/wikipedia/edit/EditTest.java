package org.wikipedia.edit;

import android.support.annotation.NonNull;
import android.support.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.WikipediaApp;
import org.wikipedia.captcha.CaptchaResult;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import retrofit2.Call;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@LargeTest
public class EditTest {
    private static final WikiSite TEST_WIKI_SITE = WikiSite.forLanguageCode("test");
    private static final String ABUSE_FILTER_ERROR_PAGE_TITLE = "Test_page_for_app_testing/AbuseFilter";
    private static final int DEFAULT_SECTION_ID = 0;
    // https://www.mediawiki.org/wiki/Manual:Edit_token#The_edit_token_suffix
    private static final String ANONYMOUS_TOKEN = "+\\";
    private static final String DEFAULT_SUMMARY = "";

    private EditClient client = new EditClient();

    @Before
    public void setUp() {
        // Cookies for a logged in session cannot be used with the anonymous edit token.
        app().getCookieManager().clearAllCookies();
    }

    @Test
    public void testEdit() {
        PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Section1", TEST_WIKI_SITE);
        String wikitext = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at "
                + System.currentTimeMillis();
        final int sectionId = 3;
        final TestLatch latch = new TestLatch();

        client.request(TEST_WIKI_SITE, title, sectionId, wikitext, ANONYMOUS_TOKEN, DEFAULT_SUMMARY,
                false, null, null, new EditClient.Callback() {
                    @Override
                    public void success(@NonNull Call<Edit> call, @NonNull EditResult result) {
                        assertThat(result, instanceOf(EditSuccessResult.class));
                        latch.countDown();
                    }

                    @Override
                    public void failure(@NonNull Call<Edit> call, @NonNull Throwable caught) {
                        throw new RuntimeException(caught);
                    }
                });
        latch.await();
    }

    @Test
    public void testCaptcha() {
        PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Captcha", TEST_WIKI_SITE);
        String wikitext = "== Section 2 ==\n\nEditing by inserting an external link https://"
                + System.currentTimeMillis();
        final TestLatch latch = new TestLatch();

        client.request(TEST_WIKI_SITE, title, DEFAULT_SECTION_ID, wikitext, ANONYMOUS_TOKEN,
                DEFAULT_SUMMARY, false, null, null, new EditClient.Callback() {
                    @Override
                    public void success(@NonNull Call<Edit> call, @NonNull EditResult result) {
                        validateCaptcha(result);
                        latch.countDown();
                    }

                    @Override
                    public void failure(@NonNull Call<Edit> call, @NonNull Throwable caught) {
                        throw new RuntimeException(caught);
                    }
                });
        latch.await();
    }

    /**
     * Test handling of abuse filter warnings which warn users about making edits of a certain kind.
     *
     * Type:   warn
     * Action: editing any userspace page while logged out
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/94
     */
    @Test
    public void testAbuseFilterTriggerWarn() {
        PageTitle title = new PageTitle(null, "User:Yuvipandaaaaaaaa", TEST_WIKI_SITE);

        // Rule 94 is only a warning so the initial attempt may be successful. The second is
        // guaranteed to be a warning if different content is used. @FlakyTest(tolerance = 2)
        // doesn't work with JUnit 4.
        for (int i = 0; i < 2; ++i) {
            String wikitext = "Testing Abusefilter by simply editing this page. Triggering rule 94"
                    + "at " + System.nanoTime();
            final TestLatch latch = new TestLatch();

            client.request(TEST_WIKI_SITE, title, DEFAULT_SECTION_ID, wikitext, ANONYMOUS_TOKEN,
                    DEFAULT_SUMMARY, false, null, null, new EditClient.Callback() {
                        @Override
                        public void success(@NonNull Call<Edit> call, @NonNull EditResult result) {
                            if (!(result instanceof EditSuccessResult)) {
                                assertThat(result, instanceOf(EditAbuseFilterResult.class));
                                //noinspection ConstantConditions
                                assertThat(((EditAbuseFilterResult) result).getType(),
                                        is(EditAbuseFilterResult.TYPE_ERROR));
                            }
                            latch.countDown();
                        }

                        @Override
                        public void failure(@NonNull Call<Edit> call, @NonNull Throwable caught) {
                            throw new RuntimeException(caught);
                        }
                    });
            latch.await();
        }
    }

    /**
     * Test handling of abuse filter errors which completely disallow edits of a certain kind.
     *
     * Type:   disallow
     * Action: adding string "poop" to page text
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/2
     */
    @Test
    public void testAbuseFilterTriggerStop() {
        PageTitle title = new PageTitle(null, ABUSE_FILTER_ERROR_PAGE_TITLE, TEST_WIKI_SITE);
        String wikitext = "== Section 2 ==\n\nTriggering AbuseFilter number 2 by saying poop many"
                + "times at " + System.currentTimeMillis();
        final TestLatch latch = new TestLatch();

        client.request(TEST_WIKI_SITE, title, DEFAULT_SECTION_ID, wikitext, ANONYMOUS_TOKEN,
                DEFAULT_SUMMARY, false, null, null, new EditClient.Callback() {
                    @Override
                    public void success(@NonNull Call<Edit> call, @NonNull EditResult result) {
                        assertThat(result, instanceOf(EditAbuseFilterResult.class));
                        assertThat(((EditAbuseFilterResult) result).getType(),
                                is(EditAbuseFilterResult.TYPE_ERROR));
                        latch.countDown();
                    }

                    @Override
                    public void failure(@NonNull Call<Edit> call, @NonNull Throwable caught) {
                        throw new RuntimeException(caught);
                    }
                });
        latch.await();
    }

    /**
     * Test the app's handling of the abuse filter emitting arbitrary error codes.
     *
     * Type:   warn
     * Action: adding string "appcrashtest" to page text
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/152
     */
    @Test
    public void testAbuseFilterTriggerStopOnArbitraryErrorCode() {
        PageTitle title = new PageTitle(null, ABUSE_FILTER_ERROR_PAGE_TITLE, TEST_WIKI_SITE);
        String wikitext = "== Section 2 ==\n\nTriggering AbuseFilter number 152 by saying"
                + "appcrashtest many times at " + System.currentTimeMillis();
        final TestLatch latch = new TestLatch();

        client.request(TEST_WIKI_SITE, title, DEFAULT_SECTION_ID, wikitext, ANONYMOUS_TOKEN,
                DEFAULT_SUMMARY, false, null, null, new EditClient.Callback() {
                    @Override
                    public void success(@NonNull Call<Edit> call, @NonNull EditResult result) {
                        assertThat(result, instanceOf(EditAbuseFilterResult.class));
                        assertThat(((EditAbuseFilterResult) result).getType(),
                                is(EditAbuseFilterResult.TYPE_ERROR));
                        latch.countDown();
                    }

                    @Override
                    public void failure(@NonNull Call<Edit> call, @NonNull Throwable caught) {
                        throw new RuntimeException(caught);
                    }
                });
        latch.await();
    }

    // Don't crash.
    @Test
    public void testErrorResponse() {
        WikiSite enwiki = WikiSite.forLanguageCode("en");
        PageTitle title = new PageTitle(Namespace.USER.toLegacyString(), "Mhollo/sandbox", enwiki);
        String badToken = "BAD_TOKEN";
        String wikitext = "foo";
        final TestLatch latch = new TestLatch();

        client.request(enwiki, title, DEFAULT_SECTION_ID, wikitext, badToken,
                DEFAULT_SUMMARY, false, null, null, new EditClient.Callback() {
                    @Override
                    public void success(@NonNull Call<Edit> call, @NonNull EditResult result) {
                        throw new RuntimeException("Token was bad, this should fail!");
                    }

                    @Override
                    public void failure(@NonNull Call<Edit> call, @NonNull Throwable caught) {
                        assertThat(caught.getMessage(), is("Invalid token"));
                        latch.countDown();
                    }
                });
        latch.await();
    }

    private void validateCaptcha(EditResult result) {
        assertThat(result, instanceOf(CaptchaResult.class));
        CaptchaResult captchaResult = (CaptchaResult) result;
        String url = captchaResult.getCaptchaUrl(TEST_WIKI_SITE);
        assertThat(isValidCaptchaUrl(url), is(true));
    }

    private boolean isValidCaptchaUrl(String url) {
        return url.startsWith(getNetworkProtocol()
                + "://test.wikipedia.org/w/index.php?title=Special:Captcha/image");
    }

    private String getNetworkProtocol() {
        return app().getWikiSite().scheme();
    }

    private WikipediaApp app() {
        return WikipediaApp.getInstance();
    }
}
