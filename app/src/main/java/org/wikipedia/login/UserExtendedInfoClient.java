package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArraySet;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Retrofit DataClient to retrieve user info and group membership information for a specific user.
 */
public class UserExtendedInfoClient {
    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @Nullable private Call<MwQueryResponse> call;

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, int id, @NonNull ListUserResponse user);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }

    public Call<MwQueryResponse> request(@NonNull WikiSite wiki, @NonNull String userName, @NonNull Callback cb) {
        return request(cachedService.service(wiki), userName, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull Service service,
                                                     @NonNull final String userName,
                                                     @NonNull final Callback cb) {
        cancel();
        call = service.request(userName);
        // noinspection ConstantConditions
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                if (response.body() != null && response.body().success()
                        && response.body().query().getUserResponse(userName) != null) {
                    // noinspection ConstantConditions
                    cb.success(call, response.body().query().userInfo().id(),
                            response.body().query().getUserResponse(userName));
                } else if (response.body() != null && response.body().hasError()) {
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
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                if (call.isCanceled()) {
                    return;
                }
                cb.failure(call, caught);
            }
        });
        return call;
    }

    private void cancel() {
        if (call == null) {
            return;
        }
        call.cancel();
        call = null;
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&meta=userinfo&list=users&usprop=groups|cancreate")
        Call<MwQueryResponse> request(@Query("ususers") @NonNull String userName);
    }

    public static class ListUserResponse {
        @SuppressWarnings("unused") @SerializedName("name") @Nullable private String name;
        @SuppressWarnings("unused") private long userid;
        @SuppressWarnings("unused") @Nullable private List<String> groups;
        @SuppressWarnings("unused") @Nullable private String cancreate;
        @SuppressWarnings("unused") @Nullable private List<UserResponseCreateError> cancreateerror;

        @Nullable public String name() {
            return name;
        }

        public boolean canCreate() {
            return cancreate != null;
        }

        @NonNull Set<String> getGroups() {
            return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
        }
    }

    public static class UserResponseCreateError {
        @SuppressWarnings("unused") @Nullable private String message;
        @SuppressWarnings("unused") @Nullable private String code;
        @SuppressWarnings("unused") @Nullable private String type;

        @NonNull public String message() {
            return StringUtils.defaultString(message);
        }
    }
}
