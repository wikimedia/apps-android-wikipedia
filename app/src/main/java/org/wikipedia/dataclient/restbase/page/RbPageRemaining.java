package org.wikipedia.dataclient.restbase.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.page.Page;
import org.wikipedia.page.Section;

import java.util.Collections;
import java.util.List;

/**
 * Gson POJO for loading remaining page content.
 */
public class RbPageRemaining implements PageRemaining {
    @Nullable private List<Section> sections;

    @Override public void mergeInto(Page page) {
        page.augmentRemainingSections(sections());
    }

    @NonNull @Override public List<Section> sections() {
        if (sections == null) {
            return Collections.emptyList();
        }
        return sections;
    }
}
