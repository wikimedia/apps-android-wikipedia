package org.wikipedia.server.restbase;

import org.wikipedia.page.Page;
import org.wikipedia.page.Section;
import org.wikipedia.server.PageRemaining;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Gson POJO for loading remaining page content.
 */
public class RbPageRemaining implements PageRemaining {
    @Nullable private List<Section> sections;

    @Nullable
    public List<Section> getSections() {
        return sections;
    }

    @Override
    public void mergeInto(Page page) {
        page.augmentRemainingSections(getSections());
    }
}
