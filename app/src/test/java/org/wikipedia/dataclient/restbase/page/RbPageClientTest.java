package org.wikipedia.dataclient.restbase.page;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.wikipedia.dataclient.page.BasePageClientTest;
import org.wikipedia.dataclient.page.PageClient;

public class RbPageClientTest extends BasePageClientTest {
    private PageClient subject;

    @Before public void setUp() throws Throwable {
        super.setUp();
        subject = new RbPageClient(service(RbPageService.class));
    }

    @NonNull @Override protected PageClient subject() {
        return subject;
    }
}
