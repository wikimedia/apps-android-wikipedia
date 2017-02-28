package org.wikipedia.descriptions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwServiceError;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.RetrofitException;
import org.wikipedia.login.User;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ReleaseUtil;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * Data Client to submit a new or updated description to wikidata.org.
 */
public class DescriptionEditClient {
    private static List<String> ENABLED_LANGUAGES = Arrays.asList("ru", "he", "ca");
    private static final String ABUSEFILTER_DISALLOWED = "abusefilter-disallowed";
    private static final String ABUSEFILTER_WARNING = "abusefilter-warning";

    @NonNull private final MwCachedService<Service> cachedService
            = new MwCachedService<>(Service.class);

    public interface Callback {
        void success(@NonNull Call<DescriptionEdit> call);
        void abusefilter(@NonNull Call<DescriptionEdit> call, @Nullable String code, @Nullable String info);
        void invalidLogin(@NonNull Call<DescriptionEdit> call, @NonNull Throwable caught);
        void failure(@NonNull Call<DescriptionEdit> call, @NonNull Throwable caught);
    }

    public static boolean isEditAllowed(@NonNull Page page) {
        PageProperties props = page.getPageProperties();
        return !TextUtils.isEmpty(props.getWikiBaseItem())
                && (ENABLED_LANGUAGES.contains(page.getTitle().getWikiSite().languageCode()) || ReleaseUtil.isPreBetaRelease());
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
    public Call<DescriptionEdit> request(@NonNull WikiSite wiki,
                                         @NonNull PageTitle pageTitle,
                                         @NonNull String description,
                                         @NonNull String editToken,
                                         @NonNull Callback cb) {
        return request(cachedService.service(wiki), pageTitle, description,
                pageTitle.getWikiSite().languageCode(), editToken, User.isLoggedIn(), cb);
    }

    @SuppressWarnings("WeakerAccess") @VisibleForTesting
    Call<DescriptionEdit> request(@NonNull Service service,
                                  @NonNull PageTitle pageTitle,
                                  @NonNull String description,
                                  @NonNull String languageCode,
                                  @NonNull String editToken,
                                  boolean loggedIn,
                                  @NonNull final Callback cb) {

        Call<DescriptionEdit> call = service.edit(languageCode, languageCode, languageCode + "wiki",
                pageTitle.getPrefixedText(), description, editToken,
                loggedIn ? "user" : null);
        call.enqueue(new retrofit2.Callback<DescriptionEdit>() {
            @Override
            public void onResponse(Call<DescriptionEdit> call,
                                   Response<DescriptionEdit> response) {
                if (response.isSuccessful()) {
                    final DescriptionEdit body = response.body();
                    if (body.editWasSuccessful()) {
                        cb.success(call);
                    } else if (body.hasError()) {
                        handleError(call, body, cb);
                    } else {
                        cb.failure(call,
                                RetrofitException.unexpectedError(new RuntimeException(
                                "Received unrecognized description edit response")));
                    }
                } else {
                    cb.failure(call,
                            RetrofitException.httpError(response));
                }
            }

            @Override
            public void onFailure(Call<DescriptionEdit> call, Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    private void handleError(@NonNull Call<DescriptionEdit> call, @NonNull DescriptionEdit body,
                             @NonNull Callback cb) {
        MwServiceError error = body.getError();
        String info = body.info();
        RuntimeException exception = new RuntimeException(info != null
                ? info : "An unknown error occurred");

        if (body.badLoginState() || body.badToken()) {
            cb.invalidLogin(call, exception);
        } else if (error != null && error.hasMessageName(ABUSEFILTER_DISALLOWED)) {
            cb.abusefilter(call, ABUSEFILTER_DISALLOWED, error.getMessageHtml(ABUSEFILTER_DISALLOWED));
        } else if (error != null && error.hasMessageName(ABUSEFILTER_WARNING)) {
            cb.abusefilter(call, ABUSEFILTER_WARNING, error.getMessageHtml(ABUSEFILTER_WARNING));
        } else {
            // noinspection ConstantConditions
            cb.failure(call, new MwException(error));
        }
    }

    @VisibleForTesting interface Service {
        @POST("w/api.php?action=wbsetdescription&format=json") @FormUrlEncoded
        Call<DescriptionEdit> edit(@NonNull @Field("language") String language,
                                   @NonNull @Field("uselang") String useLang,
                                   @NonNull @Field("site") String site,
                                   @NonNull @Field("title") String title,
                                   @NonNull @Field("value") String newDescription,
                                   @NonNull @Field("token") String token,
                                   @Nullable @Field("assert") String user);
    }
}
