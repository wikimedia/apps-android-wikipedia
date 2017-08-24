package org.wikipedia.offline;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

enum OfflineTutorialPage implements EnumCode {
    PAGE_BUILD_LIBRARY(R.layout.inflate_offline_tutorial_page_one),
    PAGE_UNINTERRUPTED_READING(R.layout.inflate_offline_tutorial_page_two),
    PAGE_STORE_KNOWLEDGE(R.layout.inflate_offline_tutorial_page_three);

    @LayoutRes private final int layout;

    private static EnumCodeMap<OfflineTutorialPage> MAP
            = new EnumCodeMap<>(OfflineTutorialPage.class);

    int getLayout() {
        return layout;
    }

    @NonNull
    public static OfflineTutorialPage of(int code) {
        return MAP.get(code);
    }

    public static int size() {
        return MAP.size();
    }

    @Override
    public int code() {
        return ordinal();
    }

    OfflineTutorialPage(@LayoutRes int layout) {
        this.layout = layout;
    }
}
