package org.wikipedia.dataclient.restbase.page;


import android.support.annotation.NonNull;

import com.google.gson.JsonParseException;

import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.dataclient.restbase.RbDefinition;
import org.wikipedia.dataclient.retrofit.RetrofitException;

import java.io.IOException;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Retrofit web service client for RESTBase Nodejs API.
 */
public class RbPageClient implements PageClient {
    @NonNull private final RbPageService service;

    public RbPageClient(@NonNull RbPageService service) {
        this.service = service;
    }

    @Override
    public void pageSummary(final String title, final PageSummary.Callback cb) {
        Call<RbPageSummary> call = service.pageSummary(title);
        call.enqueue(new Callback<RbPageSummary>() {
            /**
             * Invoked for a received HTTP response.
             * <p/>
             * Note: An HTTP response may still indicate an application-level failure such as a 404 or 500.
             * Call {@link Response#isSuccessful()} to determine if the response indicates success.
             */
            @Override
            public void onResponse(Call<RbPageSummary> call, Response<RbPageSummary> response) {
                if (response.isSuccessful()) {
                    if (response.body() == null) {
                        cb.failure(new JsonParseException("Response missing required field(s)"));
                        return;
                    }
                    cb.success(response.body());
                } else {
                    Throwable throwable = RetrofitException.httpError(response);
                    cb.failure(throwable);
                }
            }

            /**
             * Invoked when a network exception occurred talking to the server or when an unexpected
             * exception occurred creating the request or processing the response.
             */
            @Override
            public void onFailure(Call<RbPageSummary> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public void pageLead(String title, final int leadImageThumbWidth, boolean noImages,
                         final PageLead.Callback cb) {
        Call<RbPageLead> call = service.pageLead(title, optional(noImages));
        call.enqueue(new Callback<RbPageLead>() {
            @Override
            public void onResponse(Call<RbPageLead> call, Response<RbPageLead> response) {
                if (response.isSuccessful()) {
                    RbPageLead pageLead = response.body();
                    pageLead.setLeadImageThumbWidth(leadImageThumbWidth);
                    cb.success(pageLead);
                } else {
                    Throwable throwable = RetrofitException.httpError(response);
                    cb.failure(throwable);
                }
            }

            @Override
            public void onFailure(Call<RbPageLead> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public void pageRemaining(String title, boolean noImages, final PageRemaining.Callback cb) {
        Call<RbPageRemaining> call = service.pageRemaining(title, optional(noImages));
        call.enqueue(new Callback<RbPageRemaining>() {
            @Override
            public void onResponse(Call<RbPageRemaining> call, Response<RbPageRemaining> response) {
                if (response.isSuccessful()) {
                    cb.success(response.body());
                } else {
                    Throwable throwable = RetrofitException.httpError(response);
                    cb.failure(throwable);
                }
            }

            @Override
            public void onFailure(Call<RbPageRemaining> call, Throwable t) {
                cb.failure(t);
            }
        });
    }

    @Override
    public RbPageCombo pageCombo(String title, boolean noImages) throws IOException {
        Response<RbPageCombo> rsp = service.pageCombo(title, optional(noImages)).execute();
        if (rsp.isSuccessful() && !rsp.body().hasError()) {
            return rsp.body();
        }
        ServiceError err = rsp.body() == null || rsp.body().getError() == null
                ? null
                : rsp.body().getError();
        throw new IOException(err == null ? rsp.message() : err.getDetails());
    }

    /* Not defined in the PageClient interface since the Wiktionary definition endpoint exists only
     * in the mobile content service, and does not concern the wholesale retrieval of the contents
     * of a wiki page.
     */
    public void define(String title, final DefinitionCallback cb) {
        Call<Map<String, RbDefinition.Usage[]>> call = service.define(title);
        call.enqueue(new Callback<Map<String, RbDefinition.Usage[]>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, RbDefinition.Usage[]>> call,
                                   @NonNull Response<Map<String, RbDefinition.Usage[]>> response) {
                if (response.isSuccessful()) {
                    if (response.body() == null) {
                        cb.failure(new JsonParseException("Response missing required fields"));
                        return;
                    }
                    cb.success(new RbDefinition(response.body()));
                } else {
                    Throwable throwable = RetrofitException.httpError(response);
                    cb.failure(throwable);
                }
            }

            @Override
            public void onFailure(Call<Map<String, RbDefinition.Usage[]>> call, Throwable throwable) {
                cb.failure(throwable);
            }
        });
    }

    public interface DefinitionCallback {
        void success(@NonNull RbDefinition definition);

        void failure(@NonNull Throwable throwable);
    }

    /**
     * Optional boolean Retrofit parameter.
     * We don't want to send the query parameter at all when it's false since the presence of the
     * parameter alone is enough to trigger the truthy behavior.
     */
    private Boolean optional(boolean param) {
        if (param) {
            return true;
        }
        return null;
    }
}
