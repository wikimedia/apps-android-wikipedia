package org.wikipedia.dataclient.restbase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.dataclient.page.PageSummary;

import java.util.ArrayList;
import java.util.List;

public class RbRelatedPages {
    @SuppressWarnings("unused") @Nullable private List<PageSummary> pages;

    @Nullable
    public List<PageSummary> getPages() {
        return pages;
    }

    @NonNull
    public List<PageSummary> getPages(int limit) {
        List<PageSummary> list = new ArrayList<>();
        if (getPages() != null) {
            for (PageSummary page : getPages()) {
                list.add(page);
                if (limit == list.size()) {
                    break;
                }
            }
        }

        return list;
    }
}
