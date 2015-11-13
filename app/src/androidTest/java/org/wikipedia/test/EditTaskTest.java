package org.wikipedia.test;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.editing.AbuseFilterEditResult;
import org.wikipedia.editing.CaptchaResult;
import org.wikipedia.editing.EditTask;
import org.wikipedia.editing.SuccessEditResult;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.editing.EditingResult;
import org.wikipedia.editing.FetchSectionWikitextTask;
import org.wikipedia.testlib.TestLatch;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class EditTaskTest {
    private static final int SECTION_ID = 3;
    private static final Site TEST_WIKI_SITE = new Site("test.wikipedia.org");
    private static final String TEST_WIKI_DOMAIN = TEST_WIKI_SITE.getDomain();

    private static final String EDIT_TASK_PAGE_TITLE = "Test_page_for_app_testing/Section1";
    private static final String EDIT_TASK_WIKITEXT = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at ";
    private static final String SUCCESS = "Success";

    private static final String CAPTCHA_TEST_PAGE_TITLE = "Test_page_for_app_testing/Captcha";
    private static final String CAPTCHA_TEST_WIKITEXT = "== Section 2 ==\n\nEditing by inserting an external link https://";
    private static final String CAPTCHA_URL = getNetworkProtocol() + "://test.wikipedia.org/w/index.php?title=Special:Captcha/image";

    private static final String ABUSE_FILTER_WARNING_PAGE_TITLE = "User:Yuvipandaaaaaaaa";
    private static final String ABUSE_FILTER_WARNING_WIKITEXT = "Testing Abusefilter by simply editing this page. Triggering rule 94 at ";
    private static final String ABUSE_FILTER_ERROR_PAGE_TITLE = "Test_page_for_app_testing/AbuseFilter";
    private static final String ABUSE_FILTER_ERROR_WIKITEXT = "== Section 2 ==\n\nTriggering AbuseFilter number 2 by saying poop many times at ";
    private static final String ARBITRARY_ERROR_CODE_WIKITEXT = "== Section 2 ==\n\nTriggering AbuseFilter number 152 by saying appcrashtest many times at ";

    private WikipediaApp app = WikipediaApp.getInstance();
    private Context context = getInstrumentation().getTargetContext();

    @Before
    public void setUp() {
        clearSession();
    }

    @Test
    public void testEdit() throws Throwable {
        final TestLatch completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                save(completionLatch);
            }
        });
        completionLatch.await();
    }

    @Test
    public void testCaptcha() throws Throwable {
        final PageTitle title = new PageTitle(null, CAPTCHA_TEST_PAGE_TITLE, TEST_WIKI_SITE);
        final String wikitext = CAPTCHA_TEST_WIKITEXT + System.currentTimeMillis();

        final TestLatch completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new EditTask(context, title, wikitext, 0, "+\\", "", false) {
                    @Override
                    public void onFinish(EditingResult result) {
                        if (captchaShown(result)) {
                            validateCaptcha(result);
                        }
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        completionLatch.await();
    }

    /**
     * Test handling of abuse filter warnings which warn users about making edits of a certain kind.
     *
     * Type:   warn
     * Action: editing any userspace page while logged out
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/94
     *
     * @throws Throwable
     */
    @Test
    public void testAbuseFilterTriggerWarn() throws Throwable {
        final PageTitle title = new PageTitle(null, ABUSE_FILTER_WARNING_PAGE_TITLE, TEST_WIKI_SITE);
        final String wikitext = ABUSE_FILTER_WARNING_WIKITEXT + System.currentTimeMillis();
        final TestLatch completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new EditTask(context, title, wikitext, 0, "+\\", "", false) {
                    @Override
                    public void onFinish(EditingResult result) {
                        assertThat(result, instanceOf(AbuseFilterEditResult.class));
                        assertThat(((AbuseFilterEditResult) result).getType(), is(AbuseFilterEditResult.TYPE_WARNING));
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        completionLatch.await();
    }

    /**
     * Test handling of abuse filter errors which completely disallow edits of a certain kind.
     *
     * Type:   disallow
     * Action: adding string "poop" to page text
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/2
     *
     * @throws Throwable
     */
    @Test
    public void testAbuseFilterTriggerStop() throws Throwable {
        final PageTitle title = new PageTitle(null, ABUSE_FILTER_ERROR_PAGE_TITLE, TEST_WIKI_SITE);
        final String wikitext = ABUSE_FILTER_ERROR_WIKITEXT + System.currentTimeMillis();
        final TestLatch completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new EditTask(context, title, wikitext, 0, "+\\", "", false) {
                    @Override
                    public void onFinish(EditingResult result) {
                        assertThat(result, instanceOf(AbuseFilterEditResult.class));
                        assertThat(((AbuseFilterEditResult) result).getType(), is(AbuseFilterEditResult.TYPE_ERROR));
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        completionLatch.await();
    }

    /**
     * Test the app's handling of the abuse filter emitting arbitrary error codes.
     *
     * Type:   warn
     * Action: adding string "appcrashtest" to page text
     * Filter: https://test.wikipedia.org/wiki/Special:AbuseFilter/152
     *
     * @throws Throwable
     */
    @Test
    public void testAbuseFilterTriggerStopOnArbitraryErrorCode() throws Throwable {
        final PageTitle title = new PageTitle(null, ABUSE_FILTER_ERROR_PAGE_TITLE, TEST_WIKI_SITE);
        final String wikitext = ARBITRARY_ERROR_CODE_WIKITEXT + System.currentTimeMillis();
        final TestLatch completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                new EditTask(context, title, wikitext, 0, "+\\", "", false) {
                    @Override
                    public void onFinish(EditingResult result) {
                        assertThat(result, instanceOf(AbuseFilterEditResult.class));
                        // For now we handle arbitrary error codes as TYPE_ERROR. This may change.
                        assertThat(((AbuseFilterEditResult) result).getType(), is(AbuseFilterEditResult.TYPE_ERROR));
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        completionLatch.await();
    }

    private void save(final TestLatch completionLatch) {
        final PageTitle title = new PageTitle(null, EDIT_TASK_PAGE_TITLE, TEST_WIKI_SITE);
        final String addedText = EDIT_TASK_WIKITEXT + System.currentTimeMillis();

        app.getEditTokenStorage().get(title.getSite(), new EditTokenStorage.TokenRetrievedCallback() {
            @Override
            public void onTokenRetrieved(String token) {
                edit(title, addedText, token, completionLatch);
            }

            @Override
            public void onTokenFailed(Throwable caught) {
                throw new RuntimeException(caught);
            }
        });
    }

    private void edit(final PageTitle title, final String addedText, String token, final TestLatch completionLatch) {
        new EditTask(app, title, addedText, SECTION_ID, token, "", false) {
            @Override
            public void onFinish(EditingResult result) {
                verifyEditResultCode(result);
                verifyNewContent(title, addedText, completionLatch);
            }

            @Override
            public void onCatch(Throwable caught) {
                throw new RuntimeException(caught);
            }
        }.execute();
    }

    private void verifyEditResultCode(EditingResult result) {
        assertThat(result.getResult(), is(SUCCESS));
    }

    private void verifyNewContent(PageTitle title, final String addedText, final TestLatch completionLatch) {
        new FetchSectionWikitextTask(app, title, SECTION_ID) {
            @Override
            public void onFinish(String result) {
                assertThat(addedText, is(result));
                completionLatch.countDown();
            }
        }.execute();
    }

    /**
     * We don't always get a Captcha when running this test.  If the edit operation returns
     * a result of type SuccessEditResult, no Captcha was shown, so we'll skip validation.
     * @param result the editing result object
     * @return true if result is an instance of SuccessEditResult
     */
    private boolean captchaShown(EditingResult result) {
        return !(result instanceof SuccessEditResult);
    }

    private void validateCaptcha(EditingResult result) {
        assertThat(result, instanceOf(CaptchaResult.class));
        CaptchaResult captchaResult = (CaptchaResult) result;
        String url = captchaResult.getCaptchaUrl(TEST_WIKI_SITE);
        assertThat(isValidCaptchaUrl(url), is(true));
    }

    private boolean isValidCaptchaUrl(String url) {
        return url.startsWith(CAPTCHA_URL);
    }

    private static String getNetworkProtocol() {
        return WikipediaApp.getInstance().getNetworkProtocol();
    }

    private void clearSession() {
        app.getEditTokenStorage().clearEditTokenForDomain(TEST_WIKI_DOMAIN);
        app.getCookieManager().clearCookiesForDomain(TEST_WIKI_DOMAIN);
    }

    private void runOnMainSync(Runnable r) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(r);
    }
}

