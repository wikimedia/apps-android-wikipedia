package org.wikipedia.dataclient.mwapi.page;

import android.support.annotation.NonNull;

import org.wikipedia.dataclient.ServiceError;
import org.wikipedia.dataclient.page.PageClient;
import org.wikipedia.dataclient.page.PageCombo;
import org.wikipedia.dataclient.page.PageLead;
import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.dataclient.page.PageSummary;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Retrofit web service client for MediaWiki PHP API.
 */
public class MwPageClient implements PageClient {
    @NonNull private final MwPageService service;

    public MwPageClient(@NonNull MwPageService service) {
        this.service = service;
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageSummary> summary(@NonNull String title) {
        return service.summary(title);
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageLead> lead(@NonNull String title,
                                                            int leadThumbnailWidth,
                                                            boolean noImages) {
        return service.lead(title, leadThumbnailWidth, optional(noImages));
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public Call<? extends PageRemaining> sections(@NonNull String title,
                                                                     boolean noImages) {
        return service.sections(title, optional(noImages));
    }

    @Override public PageCombo pageCombo(String title, boolean noImages) throws IOException {
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
