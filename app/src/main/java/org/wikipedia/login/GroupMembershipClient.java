package org.wikipedia.login;

import org.wikipedia.Site;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.POST;
import retrofit2.http.Query;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Retrofit DataClient to retrieve implicit group membership information for a specific user.
 */
class GroupMembershipClient {
    @NonNull private final MwCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    @Nullable private Call<MwQueryResponse<UserMemberships>> groupCall;

    interface GroupMembershipCallback {
        void success(@NonNull Set<String> result);
        void error(@NonNull Throwable caught);
    }

    public void request(@NonNull final Site site, @NonNull final String userName,
                        @NonNull final GroupMembershipCallback cb) {
        cancel();

        groupCall = cachedService.service(site).listUsers(userName);
        groupCall.enqueue(new Callback<MwQueryResponse<UserMemberships>>() {
            @Override
            public void onResponse(Call<MwQueryResponse<UserMemberships>> call,
                                   Response<MwQueryResponse<UserMemberships>> response) {
                if (response.isSuccessful()) {
                    final MwQueryResponse<UserMemberships> body = response.body();
                    final UserMemberships query = body.query();
                    if (query != null) {
                        cb.success(query.getGroupsFor(userName));
                    } else if (body.getError() != null) {
                        cb.error(new LoginClient.LoginFailedException(
                                "Failed to retrieve group membership data. "
                                        + body.getError().toString()));
                    } else {
                        cb.error(new LoginClient.LoginFailedException(
                                "Unexpected error trying to retrieve group membership data. "
                                        + body.toString()));
                    }
                } else {
                    cb.error(new LoginClient.LoginFailedException(response.message()));
                }
            }

            @Override
            public void onFailure(Call<MwQueryResponse<UserMemberships>> call, Throwable caught) {
                cb.error(caught);
            }
        });
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

    private interface Service {

        /** Request the implicit groups a user belongs to. */
        @NonNull
        @POST("w/api.php?format=json&formatversion=2&action=query"
                + "&list=users&usprop=implicitgroups")
        Call<MwQueryResponse<UserMemberships>> listUsers(
                @Query("ususers") @NonNull String userName);
    }

    private static final class UserMemberships {
        @SuppressWarnings("MismatchedReadAndWriteOfArray") @SerializedName("users") @NonNull
        private List<ListUsersResponse> users = Collections.emptyList();

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
                    Set<String> groups = new HashSet<>();
                    groups.addAll(Arrays.asList(implicitGroups));
                    return Collections.unmodifiableSet(groups);
                } else {
                    return null;
                }
            }
        }
    }
}
