
package org.wikipedia.test;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import org.wikimedia.wikipedia.test.R;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.editing.EditTokenStorage;
import org.wikipedia.login.LoginTask;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LoginTaskTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public LoginTaskTest() {
        super(TestDummyActivity.class);
    }

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
                    public void onFinish(String result) {
                        assertNotNull(result);
                        assertEquals(result, "Success");
                        app.getEditTokenStorage().get(testWiki, new EditTokenStorage.TokenRetreivedCallback() {
                            @Override
                            public void onTokenRetreived(String token) {
                                assertNotNull(token);
                                assertFalse(token.equals("+\\"));
                                completionLatch.countDown();
                            }
                        });
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}

