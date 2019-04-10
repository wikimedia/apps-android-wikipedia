package org.wikipedia.dataclient.page;

import org.wikipedia.page.Section;

import java.util.List;

import androidx.annotation.NonNull;

/**
 * Gson POJI for loading remaining page content.
 */
public interface PageRemaining {
    @NonNull List<Section> sections();
}
