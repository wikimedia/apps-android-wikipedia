package org.wikipedia.overhaul.navtab;

import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import org.wikipedia.R;
import org.wikipedia.feed.FeedFragment;
import org.wikipedia.history.HistoryFragment;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.nearby.NearbyFragment;

public enum NavViewTab implements EnumCode {
    EXPLORE(R.string.nav_item_feed, R.drawable.ic_globe) {
        @NonNull @Override public Fragment newInstance() {
            return FeedFragment.newInstance();
        }
    },
    NEARBY(R.string.nav_item_nearby, R.drawable.ic_explore_black_24dp) {
        @NonNull @Override public Fragment newInstance() {
            return new NearbyFragment();
        }
    },
    LIBRARY(R.string.nav_item_reading_lists, R.drawable.ic_bookmark_black_24dp) {
        @NonNull @Override public Fragment newInstance() {
            return new HistoryFragment();
        }
    };

    private static final EnumCodeMap<NavViewTab> MAP = new EnumCodeMap<>(NavViewTab.class);

    @StringRes private final int text;
    @DrawableRes private final int icon;

    @NonNull public static NavViewTab of(int code) {
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

    NavViewTab(@StringRes int text, @DrawableRes int icon) {
        this.text = text;
        this.icon = icon;
    }
}