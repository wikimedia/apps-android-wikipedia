package org.wikipedia.descriptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.json.JSONArray;
import org.wikipedia.WikipediaApp;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwPostResponse;
import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ReleaseUtil;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Data Client to submit a new or updated description to wikidata.org.
 */
public class DescriptionEditClient {
    private static final String ABUSEFILTER_DISALLOWED = "abusefilter-disallowed";
    private static final String ABUSEFILTER_WARNING = "abusefilter-warning";
    private static final String DESCRIPTION_SOURCE_LOCAL = "local";
    private static final String DESCRIPTION_SOURCE_WIKIDATA = "central";

    public interface Callback {
        void success(@NonNull Call<MwPostResponse> call);
        void abusefilter(@NonNull Call<MwPostResponse> call, @Nullable String code, @Nullable String info);
        void invalidLogin(@NonNull Call<MwPostResponse> call, @NonNull Throwable caught);
        void failure(@NonNull Call<MwPostResponse> call, @NonNull Throwable caught);
    }

    public static boolean isEditAllowed(@NonNull Page page) {
        PageProperties props = page.getPageProperties();
        return !TextUtils.isEmpty(props.getWikiBaseItem())
                && !DESCRIPTION_SOURCE_LOCAL.equals(props.getDescriptionSource())
                && (!isLanguageBlacklisted(page.getTitle().getWikiSite().languageCode())
                || ReleaseUtil.isPreBetaRelease());
    }

    private static boolean isLanguageBlacklisted(@NonNull String lang) {
        JSONArray blacklist = WikipediaApp.getInstance().getRemoteConfig().getConfig()
                .optJSONArray("descriptionEditLangBlacklist");
        if (blacklist != null) {
            for (int i = 0; i < blacklist.length(); i++) {
                if (lang.equals(blacklist.optString(i))) {
                    return true;
                }
            }
            return false;
        } else {
            return Arrays.asList("en")
                    .contains(lang);
        }
    }

    /**
     * Submit a new value for the Wikidata description associated with the given Wikipedia page.
     *
     * @param wiki             the Wiki site to use this on. Should be "www.wikidata.org"
     * @param pageTitle        specifies the Wikipedia page the Wikidata item is linked to
     * @param description      the new value for the Wikidata description
     * @param editToken        a token from Wikidata
     * @param cb               called when this is done successfully or failed
     * @return Call object which can be used to cancel the request
     */
    public Call<MwPostResponse> request(@NonNull WikiSite wiki,
                                        @NonNull PageTitle pageTitle,
                                        @NonNull String description,
                                        @NonNull String editToken,
                                        @NonNull Callback cb) {
        return request(ServiceFactory.get(wiki), pageTitle, description, editToken,
                AccountUtil.isLoggedIn(), cb);
    }

    @SuppressWarnings("WeakerAccess") @VisibleForTesting
    Call<MwPostResponse> request(@NonNull Service service,
                                 @NonNull PageTitle pageTitle,
                                 @NonNull String description,
                                 @NonNull String editToken,
                                 boolean loggedIn,
                                 @NonNull final Callback cb) {

        Call<MwPostResponse> call = service.postDescriptionEdit(pageTitle.getWikiSite().languageCode(),
                pageTitle.getWikiSite().languageCode(), pageTitle.getWikiSite().dbName(),
                pageTitle.getPrefixedText(), description, editToken,
                loggedIn ? "user" : null);
        call.enqueue(new retrofit2.Callback<MwPostResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwPostResponse> call, @NonNull Response<MwPostResponse> response) {
                final MwPostResponse body = response.body();
                if (body.getSuccessVal() > 0) {
                    cb.success(call);
                } else {
                    cb.failure(call, RetrofitException.unexpectedError(new RuntimeException(
                                    "Received unrecognized description edit response")));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwPostResponse> call, @NonNull Throwable t) {
                if (call.isCanceled()) {
                    return;
                }
                if (t instanceof MwException) {
                    handleError(call, (MwException) t, cb);
                } else {
                    cb.failure(call, t);
                }
            }
        });
        return call;
    }

    private void handleError(@NonNull Call<MwPostResponse> call, @NonNull MwException e, @NonNull Callback cb) {
        MwServiceError error = e.getError();

        if (error.badLoginState() || error.badToken()) {
            cb.invalidLogin(call, e);
        } else if (error != null && error.hasMessageName(ABUSEFILTER_DISALLOWED)) {
            cb.abusefilter(call, ABUSEFILTER_DISALLOWED, error.getMessageHtml(ABUSEFILTER_DISALLOWED));
        } else if (error != null && error.hasMessageName(ABUSEFILTER_WARNING)) {
            cb.abusefilter(call, ABUSEFILTER_WARNING, error.getMessageHtml(ABUSEFILTER_WARNING));
        } else {
            // noinspection ConstantConditions
            cb.failure(call, e);
        }
    }
}
