package org.wikipedia.nearby;

import android.support.annotation.NonNull;

import java.util.List;

class NearbyResult {
    @NonNull private List<NearbyPage> list;

    NearbyResult(@NonNull List<NearbyPage> list) {
        this.list = list;
    }

    @NonNull public List<NearbyPage> getList() {
        return list;
    }
}
