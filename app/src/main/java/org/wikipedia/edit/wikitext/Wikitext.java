package org.wikipedia.edit.wikitext;

import android.support.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.model.BaseModel;

import java.util.List;

public class Wikitext extends BaseModel {
    @SuppressWarnings("unused,NullableProblems,MismatchedQueryAndUpdateOfCollection") @Nullable
    private List<MwQueryPage> pages;

    @Nullable String wikitext() {
        if (pages == null) {
            return null;
        }
        for (MwQueryPage page : pages) {
            if (page.revisions() != null && page.revisions().get(0) != null) {
                return page.revisions().get(0).content();
            }
        }
        return null;
    }
}
