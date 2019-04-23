package org.wikipedia.dataclient.mwapi.page;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.page.Namespace;

/**
 * Useful for link previews coming from MW API.
 */
public class MwQueryPageSummary extends MwQueryResponse implements PageSummary {
    @Override @Nullable public String getTitle() {
        if (query() == null || query().firstPage() == null) {
            return null;
        }
        return query().firstPage().title();
    }

    @Override @Nullable public String getDisplayTitle() {
        if (query() == null || query().firstPage() == null) {
            return null;
        }
        return (query().firstPage().pageProps() != null && !TextUtils.isEmpty(query().firstPage().pageProps().getDisplayTitle()))
                ? query().firstPage().pageProps().getDisplayTitle() : query().firstPage().title();
    }

    @Override @Nullable public String getConvertedTitle() {
        if (query() == null || query().firstPage() == null) {
            return null;
        }
        return (query().firstPage().convertedTo() != null && !TextUtils.isEmpty(query().firstPage().convertedTo()))
                ? query().firstPage().convertedTo() : query().firstPage().title();
    }

    @Override @Nullable
    public String getExtract() {
        if (query() == null || query().firstPage() == null) {
            return null;
        }
        return query().firstPage().extract();
    }

    @Override @Nullable
    public String getExtractHtml() {
        return getExtract();
    }

    @Override @Nullable
    public String getThumbnailUrl() {
        if (query() == null || query().firstPage() == null) {
            return null;
        }
        return query().firstPage().thumbUrl();
    }

    @Override @NonNull
    public Namespace getNamespace() {
        if (query() == null || query().firstPage() == null) {
            return Namespace.MAIN;
        }
        return query().firstPage().namespace();
    }

    @NonNull @Override
    public String getType() {
        if (query() != null && query().firstPage() != null && query().firstPage().pageProps() != null
                && query().firstPage().pageProps().isDisambiguation()) {
            return TYPE_DISAMBIGUATION;
        }
        return TYPE_STANDARD;
    }

    @Override public int getPageId() {
        if (query() == null || query().firstPage() == null) {
            return 0;
        }
        return query().firstPage().pageId();
    }
}
