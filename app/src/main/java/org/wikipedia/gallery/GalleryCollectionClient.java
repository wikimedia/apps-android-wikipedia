package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.retrofit.MwCachedService;
import org.wikipedia.dataclient.retrofit.WikiCachedService;
import org.wikipedia.page.PageTitle;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

import static org.wikipedia.Constants.PREFERRED_THUMB_SIZE;

public class GalleryCollectionClient {

    @NonNull private final WikiCachedService<Service> cachedService = new MwCachedService<>(Service.class);

    public Map<String, ImageInfo> request(@NonNull WikiSite wiki, @NonNull PageTitle title, boolean getThumbs) throws IOException {
        return request(cachedService.service(wiki), title, getThumbs);
    }

    @VisibleForTesting Map<String, ImageInfo> request(@NonNull Service service, @NonNull PageTitle title, boolean getThumbs)
            throws IOException, MwException {

        Map<String, ImageInfo> result = new HashMap<>();
        MwQueryResponse currentResponse;

        Map<String, String> continuation = null;
        do {
            currentResponse = continuation == null
                    ? fetch(service, title, getThumbs)
                    : continueFetch(service, title, getThumbs, continuation);
            if (currentResponse.success()) {
                // TODO: Technically, new results should be merged with rather than overwrite old ones.
                // However, Map.merge() requires Java 8. As of this writing (May 2017), the Jack
                // compiler is deprecated, but still required to bump JAVA_VERSION to 1_8.
                //
                // In the meantime, based on manual testing, overwriting seems not to result in any
                // information loss, and should be adequate in practice.
                //
                // noinspection ConstantConditions
                result.putAll(currentResponse.query().images());
                continuation = currentResponse.continuation();
            } else if (currentResponse.hasError()) {
                // noinspection ConstantConditions
                throw new MwException(currentResponse.getError());
            } else {
                throw new IOException("An unknown error occurred.");
            }
        } while (continuation != null);

        return result;
    }

    private MwQueryResponse fetch(@NonNull Service service, @NonNull PageTitle title, boolean getThumbs)
            throws IOException {
        Call<MwQueryResponse> call = getThumbs
                ? service.fetch("dimensions|mime|url", Integer.toString(PREFERRED_THUMB_SIZE),
                    Integer.toString(PREFERRED_THUMB_SIZE), title.toString())
                : service.fetch("dimensions|mime", null, null, title.toString());
        return call.execute().body();
    }

    private MwQueryResponse continueFetch(@NonNull Service service, @NonNull PageTitle title,
                                          boolean getThumbs, @NonNull Map<String, String> continuation)
            throws IOException {
        Call<MwQueryResponse> call = getThumbs
                ? service.continueFetch("dimensions|mime|url", Integer.toString(PREFERRED_THUMB_SIZE),
                    Integer.toString(PREFERRED_THUMB_SIZE), title.toString(), continuation)
                : service.continueFetch("dimensions|mime", null, null, title.toString(), continuation);
        return call.execute().body();
    }


    @VisibleForTesting interface Service {
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=imageinfo&generator=images&converttitles=&redirects=")
        Call<MwQueryResponse> fetch(@NonNull @Query("iiprop") String properties,
                                    @Nullable @Query("iiurlwidth") String thumbWidth,
                                    @Nullable @Query("iiurlheight") String thumbHeight,
                                    @NonNull @Query("titles") String title);

        // N.B. @QueryMap will throw if it receives a null parameter, separate handling is required.
        @GET("w/api.php?action=query&format=json&formatversion=2&prop=imageinfo&generator=images&converttitles=&redirects=")
        Call<MwQueryResponse> continueFetch(@NonNull @Query("iiprop") String properties,
                                            @Nullable @Query("iiurlwidth") String thumbWidth,
                                            @Nullable @Query("iiurlheight") String thumbHeight,
                                            @NonNull @Query("titles") String title,
                                            @NonNull @QueryMap Map<String, String> continuation);
    }
}
