package org.wikipedia.dataclient.page;

import org.wikipedia.page.Page;

/**
 * Gson POJI for loading remaining page content.
 */
public interface PageRemaining {
    void mergeInto(Page page);
}
