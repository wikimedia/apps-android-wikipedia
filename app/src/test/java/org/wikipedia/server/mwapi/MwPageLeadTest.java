package org.wikipedia.server.mwapi;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.server.BasePageLeadTest;
import org.wikipedia.test.TestRunner;

import static org.wikipedia.json.GsonUnmarshaller.unmarshal;

/**
 * Tests serialization via Gson.
 * Note the ApiService uses formatversion=2 for requests which return booleans in the responses.
 */
@RunWith(TestRunner.class)
public class MwPageLeadTest extends BasePageLeadTest {
    private String wrapInMobileview(String json) {
        return "{\"mobileview\":" + json + "}";
    }

    @Test
    public void testEnglishMainPage() throws Exception {
        MwPageLead pageLead = unmarshal(MwPageLead.class, wrapInMobileview(getEnglishMainPageJson()));
        MwPageLead.Mobileview props = pageLead.getMobileview();
        verifyEnglishMainPage(props);
    }


    @Test
    public void testUnprotectedDisambiguationPage() throws Exception {
        MwPageLead pageLead = unmarshal(MwPageLead.class,
                wrapInMobileview(getUnprotectedDisambiguationPageJson()));
        MwPageLead.Mobileview props = pageLead.getMobileview();
        verifyUnprotectedDisambiguationPage(props);
    }

    /**
     * Custom deserializer; um, yeah /o\.
     * An earlier version had issues with protection settings that don't include "edit" protection.
     */
    @Test
    public void testProtectedButNoEditProtectionPage() throws Exception {
        MwPageLead pageLead = unmarshal(MwPageLead.class,
                wrapInMobileview(getProtectedButNoEditProtectionPageJson()));
        MwPageLead.Mobileview props = pageLead.getMobileview();
        verifyProtectedNoEditProtectionPage(props);
    }

    /**
     * Test an error case
     */
    @Test
    public void testError() throws Exception {
        MwPageLead pageLead = unmarshal(MwPageLead.class, getErrorJson());
        MwPageLead.Mobileview props = pageLead.getMobileview();
        verifyError(pageLead, props);
    }
}