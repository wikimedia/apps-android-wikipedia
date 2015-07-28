package org.wikipedia.data;

import com.google.gson.reflect.TypeToken;

import org.wikipedia.page.tabs.Tab;

import java.util.List;

public final class TabUnmarshaller {
    private static final TypeToken<List<Tab>> TYPE_TOKEN = new TypeToken<List<Tab>>() { };

    public static List<Tab> unmarshal(String json) {
        return GsonUnmarshaller.unmarshal(TYPE_TOKEN, json);
    }

    private TabUnmarshaller() { }
}