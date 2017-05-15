package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.dataclient.mwapi.MwQueryPage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FilePagesWithImageInfo {
    @SuppressWarnings("unused") @Nullable private List<MwQueryPage> pages;

    @NonNull Map<String, ImageInfo> images() {
        Map<String, ImageInfo> result = new HashMap<>();
        for (MwQueryPage page : pages) {
            if (page.imageInfo() != null) {
                result.put(page.title(), page.imageInfo());
            }
        }
        return result;
    }
}
