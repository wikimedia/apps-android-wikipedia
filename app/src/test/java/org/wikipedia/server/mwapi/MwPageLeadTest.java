package org.wikipedia.server.mwapi;

import org.wikipedia.test.TestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests serialization via Gson.
 * Note the ApiService uses formatversion=2 for requests which return booleans in the responses.
 */
@RunWith(TestRunner.class)
public class MwPageLeadTest {
    private static final int ID = 15580374;
    private static final long REVISION = 664887982L;
    private static final int LANGUAGE_COUNT = 45;
    private static final String LAST_MODIFIED_DATE = "2015-05-31T17:32:11Z";
    private static final String MAIN_PAGE = "Main Page";

    private Gson gson;

    @Before
    public void setUp() throws Exception {
        gson = new GsonBuilder()
                .registerTypeAdapter(MwPageLead.Protection.class, new MwPageLead.Protection.Deserializer())
                .create();
    }

    @Test
    public void testEnglishMainPage() throws Exception {
        String json = "{\"mobileview\":{"
                + "\"lastmodified\":\"" + LAST_MODIFIED_DATE + "\","
                + "\"revision\":" + REVISION + ","
                + "\"languagecount\":" + LANGUAGE_COUNT + ","
                + "\"displaytitle\":\"" + MAIN_PAGE + "\","
                + "\"id\":" + ID + ","
                + "\"description\":\"main page of a Wikimedia project\","
                + "\"mainpage\":true,"
                + "\"sections\":["
                + "{\"id\":0,\"text\":\"My lead section text\"}"
                + "],"
                + "\"protection\":{\"edit\":[\"made_up_role1\"],\"move\":[\"made_up_role2\"]},"
                + "\"editable\":false}}";
        MwPageLead pageLead = gson.fromJson(json, MwPageLead.class);
        MwPageLead.Mobileview mv = pageLead.getMobileview();
        assertThat(mv.getId(), is(ID));
        assertThat(mv.getRevision(), is(REVISION));
        assertThat(mv.getLastModified(), is(LAST_MODIFIED_DATE));
        assertThat(mv.getDisplayTitle(), is(MAIN_PAGE));
        assertThat(mv.getLanguageCount(), is(LANGUAGE_COUNT));
        // Note the capitalization of the first character on the next assertion:
        assertThat(mv.getDescription(), is("Main page of a Wikimedia project"));
        assertThat(mv.getImage(), equalTo(null));
        assertThat(mv.getThumb(), equalTo(null));
        assertThat(mv.getSections().size(), is(1));
        assertThat(mv.getSections().get(0).getId(), is(0));
        assertThat(mv.getSections().get(0).getContent(), is("My lead section text"));
        assertThat(mv.getSections().get(0).getLevel(), is(1));
        assertThat(mv.getSections().get(0).getAnchor(), equalTo(null));
        assertThat(mv.getSections().get(0).getHeading(), equalTo(null));
        assertThat(mv.getProtection().getFirstAllowedEditorRole(), is("made_up_role1"));
        assertThat(mv.isEditable(), is(false));
        assertThat(mv.isMainPage(), is(true));
        assertThat(mv.isDisambiguation(), is(false));
    }

    @Test
    public void testUnprotectedDisambiguationPage() throws Exception {
        String json = "{\"mobileview\":{"
                + "\"disambiguation\":true,"
                + "\"protection\":[]," // oh MediaWiki API
                + "\"editable\":true}}";
        MwPageLead pageLead = gson.fromJson(json, MwPageLead.class);
        MwPageLead.Mobileview mv = pageLead.getMobileview();
        assertThat(mv.getProtection().getFirstAllowedEditorRole(), equalTo(null));
        assertThat(mv.isEditable(), is(true));
        assertThat(mv.isMainPage(), is(false));
        assertThat(mv.isDisambiguation(), is(true));
    }

    /**
     * Custom deserializer; um, yeah /o\.
     * An earlier version had issues with protection settings that don't include "edit" protection.
     */
    @Test
    public void testProtectedButNoEditProtectionPage() throws Exception {
        String json = "{\"mobileview\":{"
                + "\"protection\":{\"move\":[\"sysop\"]}"
                + "}}";
        MwPageLead pageLead = gson.fromJson(json, MwPageLead.class);
        MwPageLead.Mobileview mv = pageLead.getMobileview();
        assertThat(mv.getProtection().getFirstAllowedEditorRole(), equalTo(null));
    }

    /**
     * Test an error case
     */
    @Test
    public void testError() throws Exception {
        String json = "{\"error\":{"
                + "\"code\":\"nopage\","
                + "\"info\":\"The page parameter must be set\","
                + "\"docref\":\"See https://en.wikipedia.org/w/api.php for API usage\""
                + "}}";
        MwPageLead pageLead = gson.fromJson(json, MwPageLead.class);
        MwPageLead.Mobileview mv = pageLead.getMobileview();
        assertThat(mv, equalTo(null));
        MwServiceError error = pageLead.getError();
        assertThat(error.getCode(), is("nopage"));
        assertThat(error.getInfo(), is("The page parameter must be set"));
        assertThat(error.getDocRef(), is("See https://en.wikipedia.org/w/api.php for API usage"));
    }
}
