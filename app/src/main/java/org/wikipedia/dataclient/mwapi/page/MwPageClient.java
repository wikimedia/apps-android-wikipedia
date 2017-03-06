package org.wikipedia.dataclient.mwapi.page;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.dataclient.page.PageSummary;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Retrofit web service client for MediaWiki PHP API.
 */
public class MwPageClient implements PageClient {
    @NonNull private final MwPageService service;

    public MwPageClient(@NonNull MwPageService service) {
        this.service = service;
    }

    @Override
    public void pageSummary(String title, final PageSummary.Callback cb) {
        Call<MwQueryPageSummary> call = service.pageSummary(title);
        call.enqueue(new Callback<MwQueryPageSummary>() {
            @Override
            public void onResponse(Call<MwQueryPageSummary> call, Response<MwQueryPageSummary> response) {
                cb.success(response.body());
            }

            /**
             * Invoked when a network exception occurred talking to the server or when an unexpected
             * exception occurred creating the request or processing the response.
             */
            @Override
            public void onFailure(Call<MwQueryPageSummary> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public void pageLead(String title, int leadImageThumbWidth, boolean noImages,
                         final PageLead.Callback cb) {
        Call<MwMobileViewPageLead> call = service.pageLead(title, leadImageThumbWidth, optional(noImages));
        call.enqueue(new Callback<MwMobileViewPageLead>() {
            @Override
            public void onResponse(Call<MwMobileViewPageLead> call, Response<MwMobileViewPageLead> response) {
                cb.success(response.body());
            }

            @Override
            public void onFailure(Call<MwMobileViewPageLead> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public void pageRemaining(String title, boolean noImages, final PageRemaining.Callback cb) {
        Call<MwMobileViewPageRemaining> call = service.pageRemaining(title, optional(noImages));
        call.enqueue(new Callback<MwMobileViewPageRemaining>() {
            @Override
            public void onResponse(Call<MwMobileViewPageRemaining> call, Response<MwMobileViewPageRemaining> response) {
                cb.success(response.body());
            }

            @Override
            public void onFailure(Call<MwMobileViewPageRemaining> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public MwMobileViewPageCombo pageCombo(String title, boolean noImages) throws IOException {
        Response<MwMobileViewPageCombo> rsp = service.pageCombo(title, optional(noImages)).execute();
        if (!rsp.body().hasError()) {
            return rsp.body();
        }
        ServiceError err = rsp.body() == null || rsp.body().getError() == null
                ? null
                : rsp.body().getError();
        throw new IOException(err == null ? rsp.message() : err.getDetails());
    }

    /**
     * Optional boolean Retrofit parameter.
     * We don't want to send the query parameter at all when it's false since the presence of the
     * alone is enough to trigger the truthy behavior.
     */
    private Boolean optional(boolean param) {
        if (param) {
            return true;
        }
        return null;
    }
}
