package org.wikipedia.page.tabs;

import org.wikipedia.page.PageBackStackItem;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import java.util.ArrayList;

public class Tab implements Parcelable {
    private final ArrayList<PageBackStackItem> backStack;

    public Tab() {
        backStack = new ArrayList<>();
    }

    @NonNull
    public ArrayList<PageBackStackItem> getBackStack() {
        return backStack;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(backStack);
    }

    private Tab(Parcel in) {
        backStack = (ArrayList<PageBackStackItem>) in.readArrayList(Tab.class.getClassLoader());
    }

    public static final Parcelable.Creator<Tab> CREATOR
            = new Parcelable.Creator<Tab>() {
        public Tab createFromParcel(Parcel in) {
            return new Tab(in);
        }

        public Tab[] newArray(int size) {
            return new Tab[size];
        }
    };
}
