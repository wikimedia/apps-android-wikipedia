package org.wikipedia.editing;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.settings.PrefKeys;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class EditTokenStorage {
    private final HashMap<String, String> tokenJar = new HashMap<String, String>();
    private final SharedPreferences prefs;
    private final Context context;

    public interface TokenRetrievedCallback {
        void onTokenRetrieved(String token);
        void onTokenFailed(Throwable caught);
    }

    public EditTokenStorage(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
        List<String> wikis = makeList(prefs.getString(PrefKeys.getEditTokenWikis(), ""));
        for (String wiki: wikis) {
            String key = String.format(PrefKeys.getEditTokenForWiki(), wiki);
            tokenJar.put(wiki, prefs.getString(key, null));
        }
    }

    public void get(final Site site, final TokenRetrievedCallback callback) {
        // This might run an AsyncTask, and hence must be called from main thread
        Utils.ensureMainThread();

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

    public void clearAllTokens() {
        SharedPreferences.Editor editor = prefs.edit();
        for (String domain: tokenJar.keySet()) {
            String key = String.format(PrefKeys.getEditTokenForWiki(), domain);
            editor.remove(key);
        }
        editor.remove(PrefKeys.getEditTokenWikis());
        editor.apply();
        tokenJar.clear();
    }

    private void updatePrefs(String wiki, String token) {
        tokenJar.put(wiki, token);
        String wikisList = makeString(tokenJar.keySet());
        String wikiKey = String.format(PrefKeys.getEditTokenForWiki(), wiki);
        prefs.edit()
                .putString(PrefKeys.getEditTokenWikis(), wikisList)
                .putString(wikiKey, token)
                .apply();
    }

    private String makeString(Iterable<String> list) {
        return TextUtils.join(";", list);
    }

    private List<String> makeList(String str) {
        return Arrays.asList(TextUtils.split(str, ";"));
    }
}
