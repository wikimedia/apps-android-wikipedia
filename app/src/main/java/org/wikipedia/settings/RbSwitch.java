package org.wikipedia.settings;

import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.retrofit.RetrofitException;

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
    private static final int HUNDRED_PERCENT = 100;
    private static final int SUCCESS_THRESHOLD = 100; // page loads
    private static final int FAILED = -1;

    public static final RbSwitch INSTANCE = new RbSwitch();

    /**
     * Returns true if RB is enabled for a particular Site (or PageTitle if you will).
     * This method has a few extra checks over the overloaded #isRestBaseEnabled():
     * It disables RB also when image download is disabled since the noimages parameter is
     * not functional yet. T119161
     * It also disables RB usage if the site is zhwiki since RB endpoints have a harder time
     * dealing with caching of language variants. T118905
     * @param site the Site of the PageTitle to use for the check
     * @return true is RB is enabled for a particular Site
     */
    public boolean isRestBaseEnabled(Site site) {
        return isRestBaseEnabled()
                && Prefs.isImageDownloadEnabled()
                && !site.languageCode().startsWith("zh");
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

        if (WikipediaApp.getInstance().isProdRelease()) {
            return isAdmitted(ticket, "restbaseProdPercent");
        } else {
            return isAdmitted(ticket, "restbaseBetaPercent");
        }
    }

    private static boolean isAdmitted(int ticket, String configKey) {
        int admittedPct = WikipediaApp.getInstance()
                .getRemoteConfig().getConfig().optInt(configKey, 0); // [0, 100]
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
    public void onRbRequestFailed(Throwable error) {
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
    private static boolean isSignificantFailure(Throwable throwable) {
        if (!(throwable instanceof RetrofitException)) {
            return false;
        }

        RetrofitException error = (RetrofitException) throwable;
        if (error.getKind() == RetrofitException.Kind.HTTP) {
            int status = error.getResponse().code();
            return status != HTTP_NOT_FOUND;
        }
        return error.getKind() != RetrofitException.Kind.NETWORK;
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
