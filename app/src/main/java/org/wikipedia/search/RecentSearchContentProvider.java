package org.wikipedia.search;

import org.wikipedia.WikipediaApp;
import org.wikipedia.data.DBOpenHelper;
import org.wikipedia.data.SQLiteContentProvider;

public class RecentSearchContentProvider extends SQLiteContentProvider<RecentSearch> {
    public RecentSearchContentProvider() {
        super(RecentSearch.PERSISTENCE_HELPER);
    }

    @Override
    protected DBOpenHelper getDbOpenHelper() {
        return ((WikipediaApp)getContext().getApplicationContext()).getDbOpenHelper();
    }
}
