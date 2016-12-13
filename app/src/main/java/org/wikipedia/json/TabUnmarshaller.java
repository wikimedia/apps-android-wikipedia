package org.wikipedia.json;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.wikipedia.crash.RemoteLogException;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.tabs.Tab;
import org.wikipedia.util.log.L;

import java.util.Collections;
import java.util.List;

public final class TabUnmarshaller {
    private static final TypeToken<List<Tab>> TYPE_TOKEN = new TypeToken<List<Tab>>() { };

    @NonNull public static List<Tab> unmarshal(@Nullable String json) {
        List<Tab> object = null;
        try {
            object = GsonUnmarshaller.unmarshal(TYPE_TOKEN, json);
        } catch (Exception e) {
            // Catch all. Any Exception can be thrown when unmarshalling.
            L.logRemoteErrorIfProd(new RemoteLogException(e).put("json", json));
        }
        if (object == null) {
            object = Collections.emptyList();
        }

        // T152980
        // When upgrading from much older versions (namely, 2.1.141 or earlier), the serialized
        // tab list may be in a format that causes WikiSite objects to have null Uri components.
        // If we encounter one of these occurrences, just clear out the entire tab list.
        // to be on the safe side.
        // TODO: Remove when the incidence of this is sufficiently diminished (April 2017?)
        boolean bad = false;
        for (Tab tab : object) {
            for (PageBackStackItem item : tab.getBackStack()) {
                if (TextUtils.isEmpty(item.getTitle().getWikiSite().authority())
                        || TextUtils.isEmpty(item.getHistoryEntry().getTitle().getWikiSite().authority())) {
                    L.logRemoteErrorIfProd(new IllegalArgumentException("Format error in serialized tab list."));
                    bad = true;
                    break;
                }
            }
        }
        if (bad) {
            object.clear();
        }

        return object;
    }

    private TabUnmarshaller() { }
}
