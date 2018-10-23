package org.wikipedia.dataclient.restbase.page;

import android.support.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.page.BasePageLeadTest;
import org.wikipedia.dataclient.page.PageClient;

import io.reactivex.observers.TestObserver;
import okhttp3.CacheControl;
import retrofit2.Response;

import static org.wikipedia.json.GsonUnmarshaller.unmarshal;

public class RbPageLeadTest extends BasePageLeadTest {
    private PageClient subject;

    @Before @Override public void setUp() throws Throwable {
        super.setUp();
        subject = new RbPageClient();
    }

    @Test public void testEnglishMainPage() throws Exception {
        RbPageLead props = unmarshal(RbPageLead.class, getEnglishMainPageJson());
        verifyEnglishMainPage(props);
    }

    @Test public void testUnprotectedDisambiguationPage() throws Exception {
        RbPageLead props = unmarshal(RbPageLead.class, getUnprotectedDisambiguationPageJson());
        verifyUnprotectedDisambiguationPage(props);
    }

    /**
     * Custom deserializer; um, yeah /o\.
     * An earlier version had issues with protection settings that don't include "edit" protection.
     */
    @Test public void testProtectedButNoEditProtectionPage() throws Exception {
        RbPageLead props = unmarshal(RbPageLead.class, getProtectedButNoEditProtectionPageJson());
        verifyProtectedNoEditProtectionPage(props);
    }

    @Test @SuppressWarnings("checkstyle:magicnumber") public void testThumbUrls() throws Throwable {
        enqueueFromFile("page_lead_rb.json");
        TestObserver<Response<RbPageLead>> observer = new TestObserver<>();
        getRestService().getLeadSection(CacheControl.FORCE_NETWORK.toString(), null, null, "foo").subscribe(observer);
        observer.assertComplete()
                .assertValue(result -> result.body().getLeadImageUrl(640).contains("640px")
                            && result.body().getThumbUrl().contains(preferredThumbSizeString())
                            && result.body().getDescription().contains("Mexican boxer")
                            && result.body().getDescriptionSource().contains("central"));
    }

    @NonNull @Override protected PageClient subject() {
        return subject;
    }
}
