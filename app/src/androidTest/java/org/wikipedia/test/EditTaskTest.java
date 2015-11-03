package org.wikipedia.test;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.editing.EditTask;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.editing.EditingResult;
import org.wikipedia.editing.FetchSectionWikitextTask;
import org.wikipedia.testlib.TestLatch;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class EditTaskTest {
    private static final int SECTION_ID = 3;
    private static final Site TEST_WIKI_SITE = new Site("test.wikipedia.org");
    private static final String TEST_WIKI_DOMAIN = TEST_WIKI_SITE.getDomain();
    private static final String EDIT_TASK_TEST_PAGE_TITLE = "Test_page_for_app_testing/Section1";
    private static final String TEXT_TO_ADD = "== Section 2 ==\n\nEditing section INSERT RANDOM & HERE test at ";
    private static final String SUCCESS = "Success";

    private TestLatch completionLatch;
    private WikipediaApp app = WikipediaApp.getInstance();

    @Before
    public void setUp() {
        clearSession();
    }

    @Test
    public void testEdit() throws Throwable {
        completionLatch = new TestLatch();
        runOnMainSync(new Runnable() {
            @Override
            public void run() {
                doSave();
            }
        });
        completionLatch.await();
    }

    private void doSave() {
        final PageTitle title = new PageTitle(null, EDIT_TASK_TEST_PAGE_TITLE, TEST_WIKI_SITE);
        final String addedText = TEXT_TO_ADD + System.currentTimeMillis();

        app.getEditTokenStorage().get(title.getSite(), new EditTokenStorage.TokenRetrievedCallback() {
            @Override
            public void onTokenRetrieved(String token) {
                attemptEdit(title, addedText, token);
            }

            @Override
            public void onTokenFailed(Throwable caught) {
                throw new RuntimeException(caught);
            }
        });
    }

    private void attemptEdit(final PageTitle title, final String addedText, String token) {
        new EditTask(app, title, addedText, SECTION_ID, token, "", false) {
            @Override
            public void onFinish(EditingResult result) {
                verifyEditResultCode(result);
                verifyNewContent(title, addedText);
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

    private void verifyNewContent(PageTitle title, final String addedText) {
        new FetchSectionWikitextTask(app, title, SECTION_ID) {
            @Override
            public void onFinish(String result) {
                assertThat(addedText, is(result));
                completionLatch.countDown();
            }
        }.execute();
    }

    private void clearSession() {
        app.getEditTokenStorage().clearEditTokenForDomain(TEST_WIKI_DOMAIN);
        app.getCookieManager().clearCookiesForDomain(TEST_WIKI_DOMAIN);
    }

    private void runOnMainSync(Runnable r) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(r);
    }
}

