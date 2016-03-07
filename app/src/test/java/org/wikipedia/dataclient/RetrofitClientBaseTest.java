package org.wikipedia.dataclient;

import org.wikipedia.readinglist.api.ReadingListDataClient;
import org.wikipedia.test.TestWebServer;

import org.junit.After;
import org.junit.Before;

import android.support.annotation.NonNull;

/**
 * A base class for test cases of Retrofit clients using the MockWebServer.
 */
public abstract class RetrofitClientBaseTest {
    private TestWebServer server = new TestWebServer();

    @Before
    public void setUp() throws Exception {
        server.setUp();
    }

    @After
    public void tearDown() throws Exception {
        server.tearDown();
    }

    protected void runTest(String responseBody, BaseTestSubject subject)
            throws InterruptedException {

        server.enqueue(responseBody);
        subject.execute();
        server.takeRequest();
    }

    protected abstract class BaseTestSubject {
        @NonNull
        private final ReadingListDataClient client;

        @NonNull
        public ReadingListDataClient getClient() {
            return client;
        }

        public abstract void execute();

        protected BaseTestSubject() {
            client = new ReadingListDataClient(server.getUrl("/").toString());
        }
    }
}
