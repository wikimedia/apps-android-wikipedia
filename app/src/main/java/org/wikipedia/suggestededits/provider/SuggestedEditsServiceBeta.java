package org.wikipedia.suggestededits.provider;

import androidx.annotation.NonNull;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface SuggestedEditsServiceBeta {
    String URL = "https://app-editor-tasks.wmflabs.org";

    @GET("/commons.wikimedia.org/v1/caption/addition/{lang}")
    @NonNull
    Observable<List<SuggestedEditItem>> getImagesWithoutCaptions(@NonNull @Path("lang") String lang);

    @GET("/commons.wikimedia.org/v1/caption/translation/from/{fromLang}/to/{toLang}")
    @NonNull
    Observable<List<SuggestedEditItem>> getImagesWithTranslatableCaptions(@NonNull @Path("fromLang") String fromLang,
                                                                          @NonNull @Path("toLang") String toLang);

    @GET("/www.wikidata.org/v1/description/addition/{lang}")
    @NonNull
    Observable<List<SuggestedEditItem>> getArticlesWithoutDescriptions(@NonNull @Path("lang") String lang);

    @GET("/www.wikidata.org/v1/description/translation/from/{fromLang}/to/{toLang}")
    @NonNull
    Observable<List<SuggestedEditItem>> getArticlesWithTranslatableDescriptions(@NonNull @Path("fromLang") String fromLang,
                                                                                @NonNull @Path("toLang") String toLang);
}
