package org.wikipedia.dataclient.page;

import android.support.annotation.NonNull;

import org.wikipedia.page.Section;

import java.util.List;

/**
 * Gson POJI for loading remaining page content.
 */
public interface PageRemaining {
    @NonNull List<Section> sections();
}
