
package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.Suppress;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.login.LoginResult;
import org.wikipedia.login.LoginTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LoginTaskTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public LoginTaskTest() {
        super(TestDummyActivity.class);
    }

    /**
     * Don't run automatically since this requires some setup, see method body.
     * Remove the @Suppress annotation when we've implemented a way to configure the user
     * name/password without having to store a file inside the Git repo.
     * This makes this test a hassle.
     */
    @Suppress
    public void testLogin() throws Throwable {
        startActivity(new Intent(), null, null);
        final Site testWiki = new Site("test.wikipedia.org");
        final String username = getInstrumentation().getContext().getString(R.string.test_username);
        final String password = getInstrumentation().getContext().getString(R.string.test_password);
        final WikipediaApp app = (WikipediaApp)getInstrumentation().getTargetContext().getApplicationContext();

        if (username.equals("Insert-your-test-username-here")) {
            throw new RuntimeException("Use a custom username and password in wikipedia-it/res/values/credentials.xml");
        }

        final CountDownLatch completionLatch = new CountDownLatch(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new LoginTask(getInstrumentation().getTargetContext(), testWiki, username, password) {
                    @Override
                    public void onFinish(LoginResult result) {
                        super.onFinish(result);
                        assertNotNull(result);
                        assertEquals(result.getCode(), "Success");
                        app.getEditTokenStorage().get(testWiki, new EditTokenStorage.TokenRetrievedCallback() {
                            @Override
                            public void onTokenRetrieved(String token) {
                                assertNotNull(token);
                                assertFalse(token.equals("+\\"));
                                completionLatch.countDown();
                            }

                            @Override
                            public void onTokenFailed(Throwable caught) {
                                fail("onTokenFailed called: " + caught);
                            }
                        });
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

