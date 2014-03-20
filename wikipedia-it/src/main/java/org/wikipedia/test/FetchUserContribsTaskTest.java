package org.wikipedia.test;

import android.content.*;
import android.test.*;
import org.wikipedia.*;
import org.wikipedia.pagehistory.usercontributions.*;

import java.util.concurrent.*;

public class FetchUserContribsTaskTest extends ActivityUnitTestCase<TestDummyActivity> {
    private static final int TASK_COMPLETION_TIMEOUT = 20000;

    public FetchUserContribsTaskTest() {
        super(TestDummyActivity.class);
    }

    public void testUserContributionsFetch() throws Throwable {
        final CountDownLatch completionLatch = new CountDownLatch(1);
        startActivity(new Intent(), null, null);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                new FetchUserContribsTask(getInstrumentation().getTargetContext(),  new Site("test.wikipedia.org"), "yuvipanda", 10, null) {
                    @Override
                    public void onFinish(FetchUserContribsTask.UserContributionsList result) {
                        assertNotNull(result);
                        assertNotNull(result.getQueryContinue());
                        assertFalse(result.getContribs().size() < 10);
                        completionLatch.countDown();
                    }
                }.execute();
            }
        });
        assertTrue(completionLatch.await(TASK_COMPLETION_TIMEOUT, TimeUnit.MILLISECONDS));
    }
}
