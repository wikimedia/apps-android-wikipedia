package org.wikipedia.analytics;

import android.text.format.DateUtils;

import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.RbSwitch;
import org.wikipedia.util.StringUtil;

public class SessionFunnel extends Funnel {
    /**
     * Definition of a "session timeout", as agreed upon by the Apps and Analytics teams.
     * (currently 30 minutes)
     */
    public static final int DEFAULT_SESSION_TIMEOUT = 30;
    public static final int MIN_SESSION_TIMEOUT = 1;

    private static final String SCHEMA_NAME = "MobileWikiAppSessions";
    private static final int REVISION = 18948969;

    private SessionData sessionData;
    private long leadSectionStartTime;
    private long restSectionsStartTime;

    public SessionFunnel(WikipediaApp app) {
        super(app, SCHEMA_NAME, REVISION);
        sessionData = Prefs.getSessionData();
        if (sessionData.getStartTime() == 0 || sessionData.getLastTouchTime() == 0) {
            // session was serialized/deserialized incorrectly, so reset it.
            sessionData = new SessionData();
            persistSession();
        }
        touchSession();
    }

    /**
     * Save the state of the current session. To be called when the main Activity is stopped,
     * so that we don't have to save its state every time a single parameter is modified.
     */
    public void persistSession() {
        Prefs.setSessionData(sessionData);
    }

    /**
     * Update the timestamp for the current session. If the last-updated time is older than the
     * defined timeout period, then consider the current session as over, and send the event!
     */
    public void touchSession() {
        long now = System.currentTimeMillis();
        if (hasTimedOut()) {
            logSessionData();
            // start a new session by clearing everything.
            sessionData = new SessionData();
            persistSession();
        }
        sessionData.setLastTouchTime(now);
    }

    public void pageViewed(HistoryEntry entry) {
        touchSession();
        sessionData.addPageViewed(entry);
    }

    public void backPressed() {
        touchSession();
        sessionData.addPageFromBack();
    }

    public void noDescription() {
        touchSession();
        sessionData.addPageWithNoDescription();
    }

    public void leadSectionFetchStart() {
        leadSectionStartTime = System.currentTimeMillis();
    }

    public void leadSectionFetchEnd() {
        sessionData.addLeadLatency(System.currentTimeMillis() - leadSectionStartTime);
    }

    public void restSectionsFetchStart() {
        restSectionsStartTime = System.currentTimeMillis();
    }

    public void restSectionsFetchEnd() {
        sessionData.addRestLatency(System.currentTimeMillis() - restSectionsStartTime);
    }

    private boolean hasTimedOut() {
        return System.currentTimeMillis() - sessionData.getLastTouchTime()
                > Prefs.getSessionTimeout() * DateUtils.MINUTE_IN_MILLIS;
    }

    private void logSessionData() {
        long sessionLengthSeconds = (sessionData.getLastTouchTime() - sessionData.getStartTime()) / DateUtils.SECOND_IN_MILLIS;
        log(
                "length", sessionLengthSeconds,
                "fromSearch", sessionData.getPagesFromSearch(),
                "fromRandom", sessionData.getPagesFromRandom(),
                "fromLanglink", sessionData.getPagesFromLangLink(),
                "fromInternal", sessionData.getPagesFromInternal(),
                "fromExternal", sessionData.getPagesFromExternal(),
                "fromHistory", sessionData.getPagesFromHistory(),
                "fromReadingList", sessionData.getPagesFromReadingList(),
                "fromNearby", sessionData.getPagesFromNearby(),
                "fromDisambig", sessionData.getPagesFromDisambig(),
                "fromBack", sessionData.getPagesFromBack(),
                "noDescription", sessionData.getPagesWithNoDescription(),
                "fromSuggestedEdits", sessionData.getPagesFromSuggestedEdits(),
                "totalPages", sessionData.getTotalPages(),
                "leadLatency", sessionData.getLeadLatency(),
                "restLatency", sessionData.getRestLatency(),
                "languages", StringUtil.listToJsonArrayString(getApp().language().getAppLanguageCodes()),
                "apiMode", RbSwitch.INSTANCE.isRestBaseEnabled() ? 1 : 0
        );
    }
}
