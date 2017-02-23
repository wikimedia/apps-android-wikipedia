package org.wikipedia.dataclient.restbase.page;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.test.MockWebServerTest;

import retrofit2.Call;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

public class RbPageClientTest extends MockWebServerTest {
    private PageClient subject;

    @Before public void setUp() throws Throwable {
        super.setUp();
        subject = new RbPageClient(service(RbPageService.class));
    }

    @Test public void testLeadTitle() throws Throwable {
        Call<?> call = subject.lead("title", 0, false);
        assertThat(call.request().url().toString(), containsString("title"));
    }

    @Test public void testLeadImages() throws Throwable {
        Call<?> call = subject.lead("", 0, false);
        assertThat(call.request().url().queryParameter("noimages"), nullValue());
    }

    @Test public void testLeadNoImages() throws Throwable {
        Call<?> call = subject.lead("", 0, true);
        assertThat(call.request().url().queryParameter("noimages"), is("true"));
    }

    @Test public void testSectionsTitle() throws Throwable {
        Call<?> call = subject.sections("title", false);
        assertThat(call.request().url().toString(), containsString("title"));
    }

    @Test public void testSectionsNoImages() throws Throwable {
        Call<?> call = subject.sections("", true);
        assertThat(call.request().url().queryParameter("noimages"), is("true"));
    }
}
