package org.wikipedia.dataclient.restbase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.restbase.page.RbPageSummary;

import java.util.ArrayList;
import java.util.List;

public class RbRelatedPages {
    @SuppressWarnings("unused") @Nullable private List<RbPageSummary> pages;

    @Nullable
    public List<RbPageSummary> getPages() {
        return pages;
    }

    @NonNull
    public List<RbPageSummary> getPages(int limit) {
        List<RbPageSummary> list = new ArrayList<>();
        if (getPages() != null) {
            for (RbPageSummary page : getPages()) {
                list.add(page);
                if (limit == list.size()) {
                    break;
                }
            }
        }

        return list;
    }
}
