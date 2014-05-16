package org.wikipedia.page;

import android.os.*;
import java.util.*;

public class BackStack implements Parcelable {

    private ArrayList<BackStackItem> backStack;

    public ArrayList<BackStackItem> getStack() {
        return backStack;
    }

    public int size() {
        return backStack.size();
    }

    public BackStack() {
        backStack = new ArrayList<BackStackItem>();
    }

    public BackStack(Parcel in) {
        backStack = in.readArrayList(BackStackItem.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeList(backStack);
    }

    public static final Parcelable.Creator<BackStack> CREATOR
            = new Parcelable.Creator<BackStack>() {
        public BackStack createFromParcel(Parcel in) {
            return new BackStack(in);
        }

        public BackStack[] newArray(int size) {
            return new BackStack[size];
        }
    };

}