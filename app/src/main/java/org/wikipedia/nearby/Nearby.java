package org.wikipedia.nearby;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwQueryNearbyPage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Nearby {
    @SuppressWarnings("unused") @Nullable private Map<String, MwQueryNearbyPage> pages;

    @NonNull List<NearbyPage> list() {
        List<NearbyPage> result = new ArrayList<>();

        if (pages == null) {
            return result;
        }

        for (Map.Entry<String, MwQueryNearbyPage> entry : pages.entrySet()) {
            NearbyPage page = new NearbyPage(entry.getValue());
            if (page.getLocation() != null) {
                result.add(page);
            }
        }

        return result;
    }
}
