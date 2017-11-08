package org.wikipedia.settings;

import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.okhttp.HttpStatusException;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.util.ReleaseUtil;

import java.util.Random;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * This class encapsulates logic to turn on or off usage of our RESTBase service for a certain
 * percentage of beta (=non-production) app installs. The percentage is remote controlled,
 * so we can turn it off if necessary and can better control the roll-out in the unlikely case
 * that the new content service cannot handle the load from the app.
 * It also has an automatic fallback to using the MW API after the first significant error.
 * So, 404s and network errors are ignored.
 */
public final class RbSwitch {
    public static final int FAILED = -1;
    private static final int HUNDRED_PERCENT = 100;
    private static final int SUCCESS_THRESHOLD = 5; // page loads
    private static final String ENABLE_RESTBASE_PERCENT_CONFIG_KEY = ReleaseUtil.isProdRelease()
            ? "restbaseProdPercent" : "restbaseBetaPercent";

    public static final RbSwitch INSTANCE = new RbSwitch();

    /**
     * Returns true if RB is enabled for a particular WikiSite (or PageTitle if you will).
     * This method has a few extra checks over the overloaded #isRestBaseEnabled():
     * It also disables RB usage if the wiki is zhwiki since RB endpoints have a harder time
     * dealing with caching of language variants. T118905
     * @param wiki the WikiSite of the PageTitle to use for the check
     * @return true is RB is enabled for a particular WikiSite
     */
    public boolean isRestBaseEnabled(WikiSite wiki) {
        return isRestBaseEnabled()
                && !wiki.languageCode().startsWith("zh");
    }

    public boolean isRestBaseEnabled() {
        return Prefs.useRestBase();
    }

    public void update() {
        if (!Prefs.useRestBaseSetManually()) {
            Prefs.setUseRestBase(shouldUseRestBase());
        }
    }

    private static boolean shouldUseRestBase() {
        return isSlatedForRestBase() && hasNotRecentlyFailed();
    }

    private static boolean isSlatedForRestBase() {
        int ticket = Prefs.getRbTicket(0);
        if (ticket == 0) {
            ticket = new Random().nextInt(HUNDRED_PERCENT) + 1; // [1, 100]
            Prefs.setRbTicket(ticket);
        }

        return isAdmitted(ticket, ENABLE_RESTBASE_PERCENT_CONFIG_KEY);
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private static boolean isAdmitted(@IntRange(from = 1, to = 100) int ticket, String configKey) {
        @IntRange(from = 0, to = 100) int admittedPct = WikipediaApp.getInstance()
                .getRemoteConfig().getConfig().optInt(configKey, 100); // 0 = disable
        return ticket <= admittedPct;
    }

    private static boolean hasNotRecentlyFailed() {
        return Prefs.getRequestSuccessCounter(0) >= 0;
    }

    /**
     * For automatically bouncing back from MW API to RB API after SUCCESS_THRESHOLD number of
     * successful requests happened after #markRbFailed.
     */
    public void onMwSuccess() {
        if (isSlatedForRestBase()) {
            int successes = Prefs.getRequestSuccessCounter(0);
            successes++;
            Prefs.setRequestSuccessCounter(successes);
            resetFailed();
            if (successes >= SUCCESS_THRESHOLD && !Prefs.useRestBaseSetManually()) {
                Prefs.setUseRestBase(true);
            }
        }
    }

    /**
     * Call this method when a RESTBase call fails.
     */
    public void onRbRequestFailed(@Nullable Throwable error) {
        if (isSignificantFailure(error)) {
            markRbFailed();
            if (!Prefs.useRestBaseSetManually()) {
                Prefs.setUseRestBase(false);
            }
        }
    }

    /**
     * Determines if an error is significant enough to warrant a fallback to MwApi.
     * We don't want to fallback just because of a user error (404)
     * or a network issue on the client side (RetrofitError.Kind.NETWORK).
     */
    private static boolean isSignificantFailure(@Nullable Throwable throwable) {
        if (throwable instanceof RetrofitException) {
            RetrofitException error = (RetrofitException) throwable;
            if (error.getKind() == RetrofitException.Kind.HTTP) {
                return error.getCode() != null && error.getCode() != HTTP_NOT_FOUND;
            }
            return error.getKind() != RetrofitException.Kind.NETWORK;
        }

        if (throwable instanceof HttpStatusException) {
            HttpStatusException e = (HttpStatusException) throwable;
            return e.code() != HTTP_NOT_FOUND;
        }

        return false;
    }

    private static void markRbFailed() {
        Prefs.setRequestSuccessCounter(FAILED);
    }

    private static void resetFailed() {
        Prefs.setRequestSuccessCounter(0);
    }

    private RbSwitch() {
        update();
    }
}
