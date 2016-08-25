package org.wikipedia.page.action;

import android.support.annotation.NonNull;

import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;

public enum PageActionTab implements EnumCode {
    ADD_TO_READING_LIST() {
        @Override
        public void select(@NonNull Callback cb) {
            cb.onAddToReadingListTabSelected();
        }
    },
    SHARE() {
        @Override
        public void select(@NonNull Callback cb) {
            cb.onSharePageTabSelected();
        }
    },
    CHOOSE_LANGUAGE() {
        @Override
        public void select(@NonNull Callback cb) {
            cb.onChooseLangTabSelected();
        }
    },
    FIND_IN_PAGE() {
        @Override
        public void select(@NonNull Callback cb) {
            cb.onFindInPageTabSelected();
        }
    },
    VIEW_TOC() {
        @Override
        public void select(@NonNull Callback cb) {
            cb.onViewToCTabSelected();
        }
    };

    private static final EnumCodeMap<PageActionTab> MAP = new EnumCodeMap<>(PageActionTab.class);

    @NonNull
    public static PageActionTab of(int code) {
        return MAP.get(code);
    }

    public abstract void select(@NonNull Callback cb);

    @Override public int code() {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal();
    }

    public interface Callback {
        void onAddToReadingListTabSelected();
        void onSharePageTabSelected();
        void onChooseLangTabSelected();
        void onFindInPageTabSelected();
        void onViewToCTabSelected();
        void updateBookmark(boolean pageSaved);
    }
}
