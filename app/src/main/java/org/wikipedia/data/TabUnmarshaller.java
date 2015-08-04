package org.wikipedia.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.reflect.TypeToken;

import org.acra.ACRA;
import org.wikipedia.WikipediaApp;
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

            // TODO: replace this block with silent exception reporting.
            if (WikipediaApp.getInstance().isProdRelease()) {
                L.e(e);
            } else {
                ACRA.getErrorReporter().putCustomData("json", json);
                ACRA.getErrorReporter().handleException(e, false);
            }
        }
        if (object == null) {
            object = Collections.emptyList();
        }
        return object;
    }

    private TabUnmarshaller() { }
}
