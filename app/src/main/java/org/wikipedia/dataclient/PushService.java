package org.wikipedia.dataclient;

public interface PushService {
    String PUSH_API_PREFIX = "/w/rest.php/v1/";

    // TODO: add endpoints for registering/unregistering device tokens.

    /*
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
    */
}
