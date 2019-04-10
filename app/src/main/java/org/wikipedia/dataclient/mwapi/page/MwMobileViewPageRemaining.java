package org.wikipedia.dataclient.mwapi.page;

import org.wikipedia.dataclient.page.PageRemaining;
import org.wikipedia.page.Section;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Gson POJO for loading remaining page content.
 */
public class MwMobileViewPageRemaining implements PageRemaining {
    @SuppressWarnings("unused") @Nullable private MwMobileViewPageLead.Mobileview mobileview;

    @NonNull @Override public List<Section> sections() {
        return mobileview == null ? Collections.emptyList() : mobileview.getSections();
    }
}
