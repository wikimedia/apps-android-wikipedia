package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit DataClient to retrieve user info and group membership information for a specific user.
 */
class UserExtendedInfoClient {
    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @Nullable private Call<MwQueryResponse> groupCall;

    interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, int id, @NonNull Set<String> groups);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki, @NonNull String userName,
                                         @NonNull Callback cb) {
        return request(cachedService.service(wiki), userName, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull Service service,
                                                     @NonNull final String userName,
                                                     @NonNull final Callback cb) {
        cancel();

        groupCall = service.request(userName);
        // noinspection ConstantConditions
        groupCall.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(Call<MwQueryResponse> call, Response<MwQueryResponse> response) {
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    cb.success(call, response.body().query().userInfo().id(),
                            response.body().query().getGroupsFor(userName));
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new LoginClient.LoginFailedException(
                            "Failed to retrieve user ID and group membership data. "
                                    + response.body().getError().toString()));
                } else {
                    cb.failure(call, new LoginClient.LoginFailedException(
                            "Unexpected error trying to retrieve user ID and group membership data. "
                                    + response.body().toString()));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse> call, Throwable caught) {
                cb.failure(call, caught);
            }
        });
        return groupCall;
    }

    public void cancel() {
        cancelTokenRequest();
    }

    private void cancelTokenRequest() {
        if (groupCall == null) {
            return;
        }
        groupCall.cancel();
        groupCall = null;
    }

    @VisibleForTesting interface Service {
        /** Request the groups a user belongs to. */
        @POST("w/api.php?action=query&format=json&formatversion=2&meta=userinfo&list=users&usprop=groups")
        Call<MwQueryResponse> request(@Query("ususers") @NonNull String userName);
    }
}
