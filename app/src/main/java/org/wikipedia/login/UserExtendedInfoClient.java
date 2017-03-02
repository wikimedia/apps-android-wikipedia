package org.wikipedia.login;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.util.ArraySet;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.useroption.dataclient.UserInfo;
import org.wikipedia.util.log.L;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Retrofit DataClient to retrieve implicit user info and group membership information for a specific user.
 */
class UserExtendedInfoClient {
    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @Nullable private Call<MwQueryResponse<QueryResult>> groupCall;

    interface Callback {
        void success(@NonNull Call<MwQueryResponse<QueryResult>> call, int id, @NonNull Set<String> groups);
        void failure(@NonNull Call<MwQueryResponse<QueryResult>> call, @NonNull Throwable caught);
    }

    public Call<MwQueryResponse<QueryResult>> request(@NonNull WikiSite wiki, @NonNull String userName,
                                                      @NonNull Callback cb) {
        return request(cachedService.service(wiki), userName, cb);
    }

    @VisibleForTesting Call<MwQueryResponse<QueryResult>> request(@NonNull Service service,
                                                                  @NonNull final String userName,
                                                                  @NonNull final Callback cb) {
        cancel();

        groupCall = service.request(userName);
        groupCall.enqueue(new retrofit2.Callback<MwQueryResponse<QueryResult>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<QueryResult>> call,
                                   Response<MwQueryResponse<QueryResult>> response) {
                final MwQueryResponse<QueryResult> body = response.body();
                final QueryResult query = body.query();
                if (response.body().success()) {
                    // noinspection ConstantConditions
                    int userId = query.id();
                    cb.success(call, userId, query.getGroupsFor(userName));
                    L.v("Found user ID: " + userId);
                } else if (response.body().hasError()) {
                    // noinspection ConstantConditions
                    cb.failure(call, new LoginClient.LoginFailedException(
                            "Failed to retrieve user ID and group membership data. "
                                    + body.getError().toString()));
                } else {
                    cb.failure(call, new LoginClient.LoginFailedException(
                            "Unexpected error trying to retrieve user ID and group membership data. "
                                    + body.toString()));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<QueryResult>> call, Throwable caught) {
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

    static final class QueryResult {
        @SuppressWarnings("MismatchedReadAndWriteOfArray") @SerializedName("users") @NonNull
        private List<ListUsersResponse> users = Collections.emptyList();

        @SuppressWarnings("unused") @SerializedName("userinfo") private UserInfo userInfo;

        int id() {
            return userInfo.id();
        }

        @NonNull Set<String> getGroupsFor(@NonNull String userName) {
            if (!users.isEmpty()) {
                for (ListUsersResponse user : users) {
                    final Set<String> groups = user.getGroupsFor(userName);
                    if (groups != null) {
                        return groups;
                    }
                }
            }
            return Collections.emptySet();
        }

        private static final class ListUsersResponse {
            @SerializedName("name") @Nullable private String name;

            @SerializedName("implicitgroups") @Nullable private String[] implicitGroups;

            @Nullable Set<String> getGroupsFor(@NonNull String userName) {
                if (userName.equals(name) && implicitGroups != null) {
                    Set<String> groups = new ArraySet<>();
                    groups.addAll(Arrays.asList(implicitGroups));
                    return Collections.unmodifiableSet(groups);
                } else {
                    return null;
                }
            }
        }
    }

    @VisibleForTesting interface Service {
        /** Request the implicit groups a user belongs to. */
        @NonNull
        @POST("w/api.php?action=query&format=json&formatversion=2&meta=userinfo&list=users&usprop=implicitgroups")
        Call<MwQueryResponse<QueryResult>> request(@Query("ususers") @NonNull String userName);
    }
}
