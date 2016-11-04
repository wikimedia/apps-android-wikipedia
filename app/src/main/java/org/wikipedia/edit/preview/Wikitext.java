package org.wikipedia.edit.preview;

import android.support.annotation.NonNull;

import org.wikipedia.model.BaseModel;
import org.wikipedia.server.mwapi.MwApiResponsePage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Wikitext extends BaseModel {
    @SuppressWarnings("unused,NullableProblems") @NonNull private Query query;
    @NonNull protected String wikitext() {
        return query.wikitext();
    }

    private static class Query {
        @SuppressWarnings("unused,NullableProblems") @NonNull private Map<String, MwApiResponsePage> pages;
        @NonNull String wikitext() {
            Iterator<Map.Entry<String, MwApiResponsePage>> i = pages.entrySet().iterator();
            MwApiResponsePage page = i.next().getValue();
            ArrayList<MwApiResponsePage.Revision> revisions =  new ArrayList<>(page.revisions());
            return revisions.get(0).content();
        }
    }
}
