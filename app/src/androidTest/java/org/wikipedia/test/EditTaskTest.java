package org.wikipedia.test;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.AbuseFilterEditResult;
import org.wikipedia.editing.CaptchaResult;
import org.wikipedia.editing.EditTask;
import org.wikipedia.editing.EditingResult;
import org.wikipedia.editing.SuccessEditResult;
import org.wikipedia.page.PageTitle;
import org.wikipedia.testlib.TestLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@RunWith(AndroidJUnit4.class)
public class EditTaskTest {
    private static final Site TEST_WIKI_SITE = new Site("test.wikipedia.org");

    private static final String ABUSE_FILTER_ERROR_PAGE_TITLE = "Test_page_for_app_testing/AbuseFilter";

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

        EditingResult result = Subject.execute(title, wikitext, sectionId);
        assertThat(result, instanceOf(SuccessEditResult.class));
    }

    @Test
    public void testCaptcha() {
        PageTitle title = new PageTitle(null, "Test_page_for_app_testing/Captcha", TEST_WIKI_SITE);
        String wikitext = "== Section 2 ==\n\nEditing by inserting an external link https://"
                + System.currentTimeMillis();

        EditingResult result = Subject.execute(title, wikitext);
        validateCaptcha(result);
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
        EditingResult result = null;
        for (int i = 0; !(result instanceof AbuseFilterEditResult) && i < 2; ++i) {
            String wikitext = "Testing Abusefilter by simply editing this page. Triggering rule 94 at "
                    + System.nanoTime();
            result = Subject.execute(title, wikitext);
        }

        assertThat(result, instanceOf(AbuseFilterEditResult.class));
        //noinspection ConstantConditions
        assertThat(((AbuseFilterEditResult) result).getType(), is(AbuseFilterEditResult.TYPE_WARNING));
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
        String wikitext = "== Section 2 ==\n\nTriggering AbuseFilter number 2 by saying poop many times at "
                + System.currentTimeMillis();

        EditingResult result = Subject.execute(title, wikitext);
        assertThat(result, instanceOf(AbuseFilterEditResult.class));
        assertThat(((AbuseFilterEditResult) result).getType(), is(AbuseFilterEditResult.TYPE_ERROR));
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
        String wikitext = "== Section 2 ==\n\nTriggering AbuseFilter number 152 by saying appcrashtest many times at "
                + System.currentTimeMillis();

        EditingResult result = Subject.execute(title, wikitext);
        assertThat(result, instanceOf(AbuseFilterEditResult.class));
        // For now we handle arbitrary error codes as TYPE_ERROR. This may change.
        assertThat(((AbuseFilterEditResult) result).getType(), is(AbuseFilterEditResult.TYPE_ERROR));
    }

    private void validateCaptcha(EditingResult result) {
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
        return app().getSite().scheme();
    }

    private WikipediaApp app() {
        return WikipediaApp.getInstance();
    }

    private static class Subject extends EditTask {
        private static final int DEFAULT_SECTION_ID = 0;

        // https://www.mediawiki.org/wiki/Manual:Edit_token#The_edit_token_suffix
        private static final String ANONYMOUS_TOKEN = "+\\";

        private static final String DEFAULT_SUMMARY = "";

        public static EditingResult execute(PageTitle title, String sectionWikitext) {
            return execute(title, sectionWikitext, DEFAULT_SECTION_ID);
        }

        public static EditingResult execute(PageTitle title, String sectionWikitext, int sectionId) {
            return execute(title, sectionWikitext, sectionId, ANONYMOUS_TOKEN);
        }

        public static EditingResult execute(PageTitle title, String sectionWikitext, int sectionId,
                                            String token) {
            Subject subject = new Subject(title, sectionWikitext, sectionId, token);
            subject.execute();
            return subject.await();
        }

        @NonNull private final TestLatch latch = new TestLatch();
        private EditingResult result;

        Subject(PageTitle title, String sectionWikitext, int sectionId, String token) {
            super(getTargetContext(), title, sectionWikitext, sectionId, token, DEFAULT_SUMMARY,
                    false);
        }

        @Override
        public void onFinish(EditingResult result) {
            super.onFinish(result);
            this.result = result;
            latch.countDown();
        }

        public EditingResult await() {
            latch.await();
            return result;
        }
    }
}
