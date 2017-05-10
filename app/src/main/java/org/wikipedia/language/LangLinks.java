package org.wikipedia.language;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.List;

class LangLinks {
    @SuppressWarnings("unused") @Nullable private List<MwQueryPage> pages;

    @NonNull List<PageTitle> langLinks() {
        List<PageTitle> result = new ArrayList<>();
        if (pages == null || pages.isEmpty() || pages.get(0).langLinks() == null) {
            return result;
        }
        // noinspection ConstantConditions
        for (MwQueryPage.LangLink link : pages.get(0).langLinks()) {
            PageTitle title = new PageTitle(link.title(), WikiSite.forLanguageCode(link.lang()));
            result.add(title);
        }
        return result;
    }
}
