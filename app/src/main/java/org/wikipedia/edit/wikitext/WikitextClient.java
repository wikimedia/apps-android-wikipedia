package org.wikipedia.edit.wikitext;

import com.google.gson.JsonParseException;

import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResult;
import org.wikipedia.page.PageTitle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import retrofit2.Call;
import retrofit2.Response;

public class WikitextClient {
    public Call<MwQueryResponse> request(@NonNull final WikiSite wiki, @NonNull final PageTitle title,
                                         final int sectionID, @NonNull final Callback cb) {
        return request(ServiceFactory.get(wiki), title, sectionID, cb);
    }

    @VisibleForTesting Call<MwQueryResponse> request(@NonNull Service service, @NonNull final PageTitle title,
                                                     final int sectionID, @NonNull final Callback cb) {
        Call<MwQueryResponse> call = service.getWikiTextForSection(title.getPrefixedText(), sectionID);
        call.enqueue(new retrofit2.Callback<MwQueryResponse>() {
            @Override
            public void onResponse(@NonNull Call<MwQueryResponse> call, @NonNull Response<MwQueryResponse> response) {
                // noinspection ConstantConditions
                if (response.body() != null && response.body().query() != null
                        && response.body().query().firstPage() != null
                        && getRevision(response.body().query()) != null) {
                    // noinspection ConstantConditions
                    MwQueryPage.Revision rev = getRevision(response.body().query());
                    cb.success(call, response.body().query().firstPage().title(),
                            rev.content(), rev.timeStamp());
                } else {
                    Throwable t = new JsonParseException("Error parsing wikitext from query response");
                    cb.failure(call, t);
                }
            }

            @Override
            public void onFailure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable t) {
                cb.failure(call, t);
            }
        });
        return call;
    }

    @Nullable private MwQueryPage.Revision getRevision(@Nullable MwQueryResult result) {
        if (result != null && result.pages() != null && result.pages().size() > 0) {
            MwQueryPage page = result.pages().get(0);
            if (page.revisions() != null && page.revisions().size() > 0) {
                return page.revisions().get(0);
            }
        }
        return null;
    }

    public interface Callback {
        void success(@NonNull Call<MwQueryResponse> call, @NonNull String normalizedTitle,
                     @NonNull String wikitext, @Nullable String baseTimeStamp);
        void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught);
    }
}
