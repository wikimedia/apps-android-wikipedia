package org.wikipedia.descriptions;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

enum DescriptionEditTutorialPage implements EnumCode {
    PAGE_ONE(R.layout.inflate_description_edit_tutorial_page_one),
    PAGE_TWO(R.layout.inflate_description_edit_tutorial_page_two);

    private static EnumCodeMap<DescriptionEditTutorialPage> MAP
            = new EnumCodeMap<>(DescriptionEditTutorialPage.class);

    @LayoutRes private final int layout;

    int getLayout() {
        return layout;
    }

    @NonNull public static DescriptionEditTutorialPage of(int code) {
        return MAP.get(code);
    }

    public static int size() {
        return MAP.size();
    }

    @Override public int code() {
        return ordinal();
    }

    DescriptionEditTutorialPage(@LayoutRes int layout) {
        this.layout = layout;
    }
}
