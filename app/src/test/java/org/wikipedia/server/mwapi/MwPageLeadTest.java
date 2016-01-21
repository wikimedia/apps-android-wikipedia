package org.wikipedia.server.mwapi;

import org.wikipedia.server.BasePageLeadTest;
import org.wikipedia.server.Protection;
import org.wikipedia.test.TestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Tests serialization via Gson.
 * Note the ApiService uses formatversion=2 for requests which return booleans in the responses.
 */
@RunWith(TestRunner.class)
public class MwPageLeadTest extends BasePageLeadTest {

    private Gson gson;

    @Before
    public void setUp() throws Exception {
        gson = new GsonBuilder()
                .registerTypeAdapter(Protection.class, new Protection.Deserializer())
                .create();
    }

    private String wrapInMobileview(String json) {
        return "{\"mobileview\":" + json + "}";
    }

    @Test
    public void testEnglishMainPage() throws Exception {
        MwPageLead pageLead
                = gson.fromJson(wrapInMobileview(getEnglishMainPageJson()), MwPageLead.class);
        MwPageLead.Mobileview props = pageLead.getMobileview();
        verifyEnglishMainPage(props);
    }

    @Test
    public void testUnprotectedDisambiguationPage() throws Exception {
        MwPageLead pageLead
                = gson.fromJson(wrapInMobileview(getUnprotectedDisambiguationPageJson()),
                MwPageLead.class);
        MwPageLead.Mobileview props = pageLead.getMobileview();
        verifyUnprotectedDisambiguationPage(props);
    }

    /**
     * Custom deserializer; um, yeah /o\.
     * An earlier version had issues with protection settings that don't include "edit" protection.
     */
    @Test
    public void testProtectedButNoEditProtectionPage() throws Exception {
        MwPageLead pageLead
                = gson.fromJson(wrapInMobileview(getProtectedButNoEditProtectionPageJson()),
                MwPageLead.class);
        MwPageLead.Mobileview props = pageLead.getMobileview();
        verifyProtectedNoEditProtectionPage(props);
    }

    /**
     * Test an error case
     */
    @Test
    public void testError() throws Exception {
        MwPageLead pageLead = gson.fromJson(getErrorJson(), MwPageLead.class);
        MwPageLead.Mobileview props = pageLead.getMobileview();
        verifyError(pageLead, props);
    }
}
