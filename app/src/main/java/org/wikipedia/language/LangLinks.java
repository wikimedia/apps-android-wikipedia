package org.wikipedia.language;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.page.PageTitle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class LangLinks {
    @SuppressWarnings("unused") @Nullable private Map<String, MwQueryPage> pages;

    @NonNull List<PageTitle> langLinks() {
        List<PageTitle> result = new ArrayList<>();
        if (pages == null) {
            return result;
        }
        Iterator<Map.Entry<String, MwQueryPage>> i = pages.entrySet().iterator();
        MwQueryPage page = i.next().getValue();
        if (page.langLinks() == null) {
            return result;
        }
        for (MwQueryPage.LangLink langLink : page.langLinks()) {
            result.add(new PageTitle(langLink.localizedTitle(), WikiSite.forLanguageCode(langLink.lang())));
        }
        return result;
    }
}
