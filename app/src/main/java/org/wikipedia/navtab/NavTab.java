package org.wikipedia.navtab;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import org.wikipedia.R;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.nearby.NearbyLazyLoadFragment;
import org.wikipedia.readinglist.ReadingListsFragment;

public enum NavTab implements EnumCode {
    EXPLORE(R.string.nav_item_feed, R.drawable.ic_globe) {
        @NonNull @Override public Fragment newInstance() {
            return FeedFragment.newInstance();
        }
    },
    READING_LISTS(R.string.nav_item_reading_lists, R.drawable.ic_bookmark_white_24dp) {
        @NonNull @Override public Fragment newInstance() {
            return ReadingListsFragment.newInstance();
        }
    },
    HISTORY(R.string.nav_item_history, R.drawable.ic_restore_black_24dp) {
        @NonNull @Override public Fragment newInstance() {
            return HistoryFragment.newInstance();
        }
    },
    NEARBY(R.string.nav_item_nearby, R.drawable.ic_explore_black_24dp) {
        @NonNull @Override public Fragment newInstance() {
            return NearbyLazyLoadFragment.newInstance();
        }
    };

    private static final EnumCodeMap<NavTab> MAP = new EnumCodeMap<>(NavTab.class);

    @StringRes private final int text;
    @DrawableRes private final int icon;

    @NonNull public static NavTab of(int code) {
        return MAP.get(code);
    }

    public static int size() {
        return MAP.size();
    }

    @StringRes public int text() {
        return text;
    }

    @DrawableRes public int icon() {
        return icon;
    }

    @NonNull public abstract Fragment newInstance();

    @Override public int code() {
        // This enumeration is not marshalled so tying declaration order to presentation order is
        // convenient and consistent.
        return ordinal();
    }

    NavTab(@StringRes int text, @DrawableRes int icon) {
        this.text = text;
        this.icon = icon;
    }
}
