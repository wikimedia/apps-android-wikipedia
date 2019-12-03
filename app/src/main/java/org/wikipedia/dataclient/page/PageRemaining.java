package org.wikipedia.dataclient.page;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.page.Section;

import java.util.Collections;
import java.util.List;

/**
 * Gson POJO for loading remaining page content.
 */
public class PageRemaining {
    @SuppressWarnings("unused") @Nullable
    private List<Section> sections;

    @NonNull public List<Section> sections() {
        if (sections == null) {
            return Collections.emptyList();
        }
        return sections;
    }
}
