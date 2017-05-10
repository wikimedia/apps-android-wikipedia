package org.wikipedia.nearby;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwQueryPage;

import java.util.ArrayList;
import java.util.List;

class Nearby {
    @SuppressWarnings("unused") @Nullable private List<MwQueryPage> pages;

    @NonNull List<NearbyPage> list() {
        List<NearbyPage> result = new ArrayList<>();
        if (pages == null) {
            return result;
        }
        for (MwQueryPage page : pages) {
            NearbyPage nearbyPage = new NearbyPage(page);
            if (nearbyPage.getLocation() != null) {
                result.add(nearbyPage);
            }
        }
        return result;
    }
}
