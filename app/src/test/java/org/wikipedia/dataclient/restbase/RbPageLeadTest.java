package org.wikipedia.dataclient.restbase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.wikipedia.dataclient.mwapi.page.MwMobileViewPageLead;
import org.wikipedia.dataclient.restbase.page.RbPageLead;
import org.wikipedia.server.BasePageLeadTest;
import org.wikipedia.test.TestRunner;

import static org.wikipedia.json.GsonUnmarshaller.unmarshal;

/**
 * Tests serialization via Gson.
 * Note the ApiService uses formatversion=2 for requests which return booleans in the responses.
 */
@RunWith(TestRunner.class)
public class RbPageLeadTest extends BasePageLeadTest {
    @Test
    public void testEnglishMainPage() throws Exception {
        RbPageLead props = unmarshal(RbPageLead.class, getEnglishMainPageJson());
        verifyEnglishMainPage(props);
    }

    @Test
    public void testUnprotectedDisambiguationPage() throws Exception {
        RbPageLead props = unmarshal(RbPageLead.class, getUnprotectedDisambiguationPageJson());
        verifyUnprotectedDisambiguationPage(props);
    }

    /**
     * Custom deserializer; um, yeah /o\.
     * An earlier version had issues with protection settings that don't include "edit" protection.
     */
    @Test
    public void testProtectedButNoEditProtectionPage() throws Exception {
        RbPageLead props = unmarshal(RbPageLead.class, getProtectedButNoEditProtectionPageJson());
        verifyProtectedNoEditProtectionPage(props);
    }

    /**
     * Test an error case
     */
    @Test
    public void testError() throws Exception {
        MwMobileViewPageLead pageLead = unmarshal(MwMobileViewPageLead.class, getErrorJson());
        MwMobileViewPageLead.Mobileview props = pageLead.getMobileview();
        verifyError(pageLead, props);
    }
}
