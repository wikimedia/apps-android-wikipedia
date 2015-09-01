package org.wikipedia.server.restbase;

import org.wikipedia.page.Page;
import org.wikipedia.page.Section;
import org.wikipedia.server.PageRemaining;

import com.google.gson.annotations.Expose;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Gson POJO for loading remaining page content.
 */
public class RbPageRemaining implements PageRemaining {
    @Expose @Nullable private List<Section> sections;

    @Nullable
    public List<Section> getSections() {
        return sections;
    }

    @Override
    public void mergeInto(Page page) {
        page.augmentRemainingSections(getSections());
    }
}
