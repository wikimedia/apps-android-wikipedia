package org.wikipedia.edit.preview;

import android.support.annotation.Nullable;

import org.wikipedia.model.BaseModel;
import org.wikipedia.server.mwapi.MwApiResponsePage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Wikitext extends BaseModel {
    @SuppressWarnings("unused,NullableProblems,MismatchedQueryAndUpdateOfCollection") @Nullable
    private Map<String, MwApiResponsePage> pages;

    @Nullable String wikitext() {
        if (pages == null) {
            return null;
        }
        Iterator<Map.Entry<String, MwApiResponsePage>> i = pages.entrySet().iterator();
        MwApiResponsePage page = i.next().getValue();
        if (page == null
                || page.revisions() == null
                || page.revisions().get(0) == null) {
            return null;
        }
        return new ArrayList<>(page.revisions()).get(0).content();
    }
}
