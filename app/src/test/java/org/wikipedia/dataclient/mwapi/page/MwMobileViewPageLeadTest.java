package org.wikipedia.dataclient.mwapi.page;

import org.junit.Before;
import org.junit.Test;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.page.BasePageLeadTest;
import org.wikipedia.dataclient.page.PageClient;

import androidx.annotation.NonNull;
import io.reactivex.observers.TestObserver;
import okhttp3.CacheControl;
import retrofit2.Response;

import static org.wikipedia.json.GsonUnmarshaller.unmarshal;

public class MwMobileViewPageLeadTest extends BasePageLeadTest {
    private PageClient subject;

    @Before public void setUp() throws Throwable {
        super.setUp();
        subject = new MwPageClient();
    }

    @Test public void testEnglishMainPage() throws Exception {
        MwMobileViewPageLead pageLead = unmarshal(MwMobileViewPageLead.class, wrapInMobileview(getEnglishMainPageJson()));
        MwMobileViewPageLead.Mobileview props = pageLead.getMobileview();
        verifyEnglishMainPage(props);
    }


    @Test public void testUnprotectedDisambiguationPage() throws Exception {
        MwMobileViewPageLead pageLead = unmarshal(MwMobileViewPageLead.class,
                wrapInMobileview(getUnprotectedDisambiguationPageJson()));
        MwMobileViewPageLead.Mobileview props = pageLead.getMobileview();
        verifyUnprotectedDisambiguationPage(props);
    }

    /**
     * Custom deserializer; um, yeah /o\.
     * An earlier version had issues with protection settings that don't include "edit" protection.
     */
    @Test public void testProtectedButNoEditProtectionPage() throws Exception {
        MwMobileViewPageLead pageLead = unmarshal(MwMobileViewPageLead.class,
                wrapInMobileview(getProtectedButNoEditProtectionPageJson()));
        MwMobileViewPageLead.Mobileview props = pageLead.getMobileview();
        verifyProtectedNoEditProtectionPage(props);
    }

    @Test @SuppressWarnings("checkstyle:magicnumber") public void testThumbUrls() throws Throwable {
        enqueueFromFile("page_lead_mw.json");
        TestObserver<Response<MwMobileViewPageLead>> observer = new TestObserver<>();
        getApiService().getLeadSection(CacheControl.FORCE_NETWORK.toString(), null, null, "foo", 640, "en").subscribe(observer);
        observer.assertComplete().assertNoErrors()
                .assertValue(result -> result.body().getLeadImageUrl(640).contains("640px")
                    && result.body().getThumbUrl().contains(preferredThumbSizeString())
                    && result.body().getDescription().contains("Mexican boxer"));
    }

    @Test public void testError() throws Exception {
        try {
            MwMobileViewPageLead pageLead = unmarshal(MwMobileViewPageLead.class, getErrorJson());
        } catch (MwException e) {
            verifyError(e);
        }
    }

    @NonNull @Override protected PageClient subject() {
        return subject;
    }

    private String wrapInMobileview(String json) {
        return "{\"mobileview\":" + json + "}";
    }
}
