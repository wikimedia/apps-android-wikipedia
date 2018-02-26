package org.wikipedia.language;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.JsonObject;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.json.GsonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;

public class SiteMatrixClient {
    public interface Callback {
        void success(@NonNull Call<SiteMatrix> call, @NonNull List<SiteInfo> sites);
        void failure(@NonNull Call<SiteMatrix> call, @NonNull Throwable caught);
    }

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Call<SiteMatrix> request(@NonNull WikiSite wiki, @NonNull Callback cb) {
        return request(cachedService.service(wiki), cb);
    }

    @VisibleForTesting Call<SiteMatrix> request(@NonNull Service service, @NonNull final Callback cb) {
        Call<SiteMatrix> call = service.siteMatrix();
        call.enqueue(new retrofit2.Callback<SiteMatrix>() {
            @Override
            public void onResponse(@NonNull Call<SiteMatrix> call, @NonNull Response<SiteMatrix> response) {
                if (response.body() != null) {
                    List<SiteInfo> sites = new ArrayList<>();
                    // noinspection ConstantConditions
                    JsonObject sitematrix = response.body().siteMatrix();
                    // We have to parse the Json manually because the list of SiteInfo objects
                    // contains a "count" member that prevents it from being able to deserialize
                    // as a list automatically.
                    for (String key : sitematrix.keySet()) {
                        if (key.equals("count")) {
                            continue;
                        }
                        SiteInfo info = GsonUtil.getDefaultGson().fromJson(sitematrix.get(key), SiteInfo.class);
                        if (info != null) {
                            sites.add(info);
                        }
                    }
                    cb.success(call, sites);
                } else {
                    cb.failure(call, new IOException("An unknown error occurred."));
                }
            }

            @Override
            public void onFailure(@NonNull Call<SiteMatrix> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    public class SiteMatrix {
        @SuppressWarnings("unused,NullableProblems") @NonNull private JsonObject sitematrix;

        JsonObject siteMatrix() {
            return sitematrix;
        }
    }

    public class SiteInfo {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String code;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String name;
        @SuppressWarnings("unused,NullableProblems") @NonNull private String localname;

        @NonNull public String code() {
            return code;
        }

        @NonNull public String name() {
            return name;
        }

        @NonNull public String localName() {
            return localname;
        }
    }

    @VisibleForTesting interface Service {
        @GET("w/api.php?action=sitematrix&format=json&smtype=language&smlangprop=code%7Cname%7Clocalname")
        Call<SiteMatrix> siteMatrix();
    }
}
