package org.wikipedia.dataclient;

import androidx.annotation.NonNull;

import org.wikipedia.push.PushServiceSubscriptionResponse;

import io.reactivex.Observable;
import retrofit2.Response;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PushService {

    String PROTOCOL = "gcm";

    @FormUrlEncoded
    @POST("subscribers")
    @NonNull Observable<PushServiceSubscriptionResponse> subscribe(
        @NonNull @Field("proto") String protocol,
        @NonNull @Field("lang") String language,
        @NonNull @Field("token") String token
    );

    @FormUrlEncoded
    @POST("subscriber/{id}")
    @NonNull Observable<Response<Void>> updateSubscription(
        @NonNull @Path("id") String subscriberId,
        @NonNull @Field("lang") String language
    );

    @DELETE("subscriber/{id}")
    @NonNull Observable<Response<Void>> deleteSubscription(
        @NonNull @Path("id") String subscriberId
    );

}
