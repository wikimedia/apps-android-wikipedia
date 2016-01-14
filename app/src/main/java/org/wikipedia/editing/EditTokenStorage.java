package org.wikipedia.editing;

import android.content.Context;
import android.os.Looper;

import org.wikipedia.Site;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditTokenStorage {
    private static final String DELIMITER = ";";

    private final Map<String, String> tokenJar = new HashMap<>();
    private final Context context;

    public interface TokenRetrievedCallback {
        void onTokenRetrieved(String token);
        void onTokenFailed(Throwable caught);
    }

    public EditTokenStorage(Context context) {
        this.context = context;
        List<String> wikis = makeList(Prefs.getEditTokenWikis());
        for (String wiki: wikis) {
            tokenJar.put(wiki, Prefs.getEditTokenForWiki(wiki));
        }
    }

    public void get(final Site site, final TokenRetrievedCallback callback) {
        // This might run an AsyncTask, and hence must be called from main thread
        ensureMainThread();

        String curToken = tokenJar.get(site.getDomain());
        if (curToken != null) {
            callback.onTokenRetrieved(curToken);
            return;
        }

        new FetchEditTokenTask(context, site) {
            @Override
            public void onFinish(String result) {
                updatePrefs(site.getDomain(), result);
                callback.onTokenRetrieved(result);
            }

            @Override
            public void onCatch(Throwable caught) {
                callback.onTokenFailed(caught);
            }
        }.execute();
    }

    public void clearEditTokenForDomain(String wiki) {
        Prefs.removeEditTokenForWiki(wiki);
    }

    public void clearAllTokens() {
        for (String wiki : tokenJar.keySet()) {
            Prefs.removeEditTokenForWiki(wiki);
        }
        Prefs.setEditTokenWikis(null);
        tokenJar.clear();
    }

    private void updatePrefs(String wiki, String token) {
        tokenJar.put(wiki, token);
        String wikisList = makeString(tokenJar.keySet());
        Prefs.setEditTokenWikis(wikisList);
        Prefs.setEditTokenForWiki(wiki, token);
    }

    private String makeString(Iterable<String> list) {
        return StringUtil.listToDelimitedString(list, DELIMITER);
    }

    private List<String> makeList(String str) {
        return StringUtil.delimiterStringToList(str, DELIMITER);
    }

    /**
     * Ensures that the calling method is on the main thread.
     */
    private void ensureMainThread() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new IllegalStateException("Method must be called from the main thread");
        }
    }
}
