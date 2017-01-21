package org.wikipedia.edit.preview;

import android.support.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.model.BaseModel;

import java.util.Iterator;
import java.util.Map;

public class Wikitext extends BaseModel {
    @SuppressWarnings("unused,NullableProblems,MismatchedQueryAndUpdateOfCollection") @Nullable
    private Map<String, MwQueryPage> pages;

    @Nullable String wikitext() {
        if (pages == null) {
            return null;
        }
        Iterator<Map.Entry<String, MwQueryPage>> i = pages.entrySet().iterator();
        MwQueryPage page = i.next().getValue();
        if (page == null
                || page.revisions() == null
                || page.revisions().get(0) == null) {
            return null;
        }
        return page.revisions().get(0).content();
    }
}
