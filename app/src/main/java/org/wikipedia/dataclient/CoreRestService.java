package org.wikipedia.dataclient;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.restbase.DiffResponse;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CoreRestService {
    String CORE_REST_API_PREFIX = "/w/rest.php/v1/";

    @GET("revision/{oldRev}/compare/{newRev}")
    @NonNull
    Observable<DiffResponse> getDiff(@Path("oldRev") long oldRev, @Path("newRev") long newRev);
}
