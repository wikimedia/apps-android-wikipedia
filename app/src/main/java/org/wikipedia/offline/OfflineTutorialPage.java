package org.wikipedia.offline;

import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

enum OfflineTutorialPage implements EnumCode {
    PAGE_BUILD_LIBRARY(R.layout.inflate_offline_tutorial_page_one, R.color.accent30, R.color.green30, R.color.green50),
    PAGE_UNINTERRUPTED_READING(R.layout.inflate_offline_tutorial_page_two, R.color.green30, R.color.accent50, R.color.accent30),
    PAGE_STORE_KNOWLEDGE(R.layout.inflate_offline_tutorial_page_three, R.color.green30, R.color.green50, R.color.accent50);

    @LayoutRes private final int layout;
    @ColorRes private int gradientStart;
    @ColorRes private int gradientCenter;
    @ColorRes private int gradentEnd;

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

    OfflineTutorialPage(@LayoutRes int layout, @ColorRes int gradientStart, @ColorRes int gradientCenter, @ColorRes int gradentEnd) {
        this.layout = layout;
        this.gradientStart = gradientStart;
        this.gradientCenter = gradientCenter;
        this.gradentEnd = gradentEnd;
    }

    int getLayout() {
        return layout;
    }

    int getGradientStart() {
        return gradientStart;
    }

    int getGradientCenter() {
        return gradientCenter;
    }

    int getGradentEnd() {
        return gradentEnd;
    }

    private static EnumCodeMap<OfflineTutorialPage> MAP
            = new EnumCodeMap<>(OfflineTutorialPage.class);
}
