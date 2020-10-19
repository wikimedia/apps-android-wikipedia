package org.wikipedia.analytics.eventplatform;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.core.Observable;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Event Platform intake service interface.
 *
 * To match the existing event logging behavior, we send hastily, i.e., we ask the service to
 * respond immediately, before events are processed and regardless of whether they are
 * ultimately processed successfully.
 *
 * TODO: In the future, consider updating to wait for processing and handle partial-success and
 * failure responses.
 */
interface EventService {
    @POST("/v1/events?hasty=true")
    Observable<Response<EventServiceResponse>> postEvent(@NonNull @Body Event event);
}
