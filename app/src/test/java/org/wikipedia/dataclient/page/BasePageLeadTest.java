package org.wikipedia.dataclient.page;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.dataclient.mwapi.page.MwMobileViewPageLead;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Common test code for the two PageLead variants.
 */
public abstract class BasePageLeadTest extends BasePageClientTest {
    protected static final int ID = 15580374;
    protected static final long REVISION = 664887982L;
    protected static final int LANGUAGE_COUNT = 45;
    protected static final String LAST_MODIFIED_DATE = "2015-05-31T17:32:11Z";
    protected static final String MAIN_PAGE = "Main Page";

    @NonNull
    public static String getEnglishMainPageJson() {
        return "{"
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
                + "\"editable\":false"
                + "}";
    }

    protected void verifyEnglishMainPage(PageLeadProperties props) {
        assertThat(props.getId(), is(ID));
        assertThat(props.getRevision(), is(REVISION));
        assertThat(props.getLastModified(), is(LAST_MODIFIED_DATE));
        assertThat(props.getDisplayTitle(), is(MAIN_PAGE));
        assertThat(props.getLanguageCount(), is(LANGUAGE_COUNT));
        assertThat(props.getLeadImageUrl(0), equalTo(null));
        assertThat(props.getLeadImageFileName(), equalTo(null));
        assertThat(props.getSections().size(), is(1));
        assertThat(props.getSections().get(0).getId(), is(0));
        assertThat(props.getSections().get(0).getContent(), is("My lead section text"));
        assertThat(props.getSections().get(0).getLevel(), is(1));
        assertThat(props.getSections().get(0).getAnchor(), equalTo(""));
        assertThat(props.getSections().get(0).getHeading(), equalTo(""));
        assertThat(props.getFirstAllowedEditorRole(), is("made_up_role1"));
        assertThat(props.isEditable(), is(false));
        assertThat(props.isMainPage(), is(true));
        assertThat(props.isDisambiguation(), is(false));
    }

    @NonNull
    protected String getUnprotectedDisambiguationPageJson() {
        return "{"
                + "\"disambiguation\":true,"
                + "\"protection\":{},"
                + "\"editable\":true"
                + "}";
    }

    protected void verifyUnprotectedDisambiguationPage(PageLeadProperties core) {
        assertThat(core.getFirstAllowedEditorRole(), equalTo(null));
        assertThat(core.isEditable(), is(true));
        assertThat(core.isMainPage(), is(false));
        assertThat(core.isDisambiguation(), is(true));
    }

    @NonNull
    protected String getProtectedButNoEditProtectionPageJson() {
        return "{"
                + "\"protection\":{\"move\":[\"sysop\"]}"
                + "}";
    }

    protected void verifyProtectedNoEditProtectionPage(PageLeadProperties core) {
        assertThat(core.getFirstAllowedEditorRole(), equalTo(null));
    }

    @NonNull
    protected String getErrorJson() {
        return "{\"error\":{"
                + "\"code\":\"nopage\","
                + "\"info\":\"The page parameter must be set\","
                + "\"docref\":\"See https://en.wikipedia.org/w/api.php for API usage\""
                + "}}";
    }

    protected void verifyError(MwMobileViewPageLead pageLead, MwMobileViewPageLead.Mobileview mv) {
        assertThat(mv, equalTo(null));
        MwServiceError error = pageLead.getError();
        assertThat(pageLead.hasError(), is(true));
        assert error != null;
        assertThat(error.getTitle(), is("nopage"));
        assertThat(error.getDetails(), is("The page parameter must be set"));
        assertThat(error.getDocRef(), is("See https://en.wikipedia.org/w/api.php for API usage"));
    }
}
