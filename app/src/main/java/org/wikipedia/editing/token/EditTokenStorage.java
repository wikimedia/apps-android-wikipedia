package org.wikipedia.editing.token;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

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
        for (String wiki : wikis) {
            tokenJar.put(wiki, Prefs.getEditTokenForWiki(wiki));
        }
    }

    @Nullable public String token(@NonNull WikiSite wiki) {
        return tokenJar.get(wiki.authority());
    }

    public void token(@NonNull WikiSite wiki, String token) {
        updatePrefs(wiki.authority(), token);
    }

    public void get(@NonNull final WikiSite wiki, final TokenRetrievedCallback callback) {
        ensureMainThread();

        String curToken = token(wiki);
        if (curToken != null) {
            callback.onTokenRetrieved(curToken);
            return;
        }

        new EditTokenClient().request(wiki, new EditTokenClient.Callback() {
            @Override
            public void success(@NonNull Call<EditToken> call, @NonNull String token) {
                token(wiki, token);
                callback.onTokenRetrieved(token);
            }

            @Override
            public void failure(@NonNull Call<EditToken> call, @NonNull Throwable caught) {
                callback.onTokenFailed(caught);
            }
        });
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
