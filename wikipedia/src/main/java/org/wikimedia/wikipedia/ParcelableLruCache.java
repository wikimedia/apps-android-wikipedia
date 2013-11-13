package org.wikimedia.wikipedia;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.LruCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An LRU Cache that can be parcelled.
 *
 * Will throw up a RuntimeError if passed things that are not parcellable.
 *
 * Can be considered sortof a messy hack, but grrr Android.
 *
 * FIXME: This also resets the LRUcounters when recreated.
 *
 * V can either be a Parcelable or a List<Parcelable>
 */
public class ParcelableLruCache<V> extends LruCache<String, V>  implements Parcelable {
    public final boolean isList;
    public ParcelableLruCache(int maxSize, Class valueClass) {
        super(maxSize);
        isList = valueClass.isAssignableFrom(List.class);
    }

    private ParcelableLruCache(Parcel in) {
        super(in.readInt());
        isList = in.readInt() != 0;
        Bundle contents = in.readBundle();
        contents.setClassLoader(getClass().getClassLoader());
        Set<String> keys = contents.keySet();
        for (String key : keys) {
            if (isList) {
                put(key, (V) contents.getParcelableArrayList(key));
            } else {
                put(key, (V) contents.getParcelable(key));
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(maxSize());
        dest.writeInt(isList ? 1 : 0);
        Map<String, V> snapshot = snapshot();
        Bundle bundle = new Bundle();
        for (Map.Entry<String, V> entry : snapshot.entrySet()) {
            if (isList) {
                bundle.putParcelableArrayList(entry.getKey(), (ArrayList)entry.getValue());
            } else {
                bundle.putParcelable(entry.getKey(), (Parcelable) entry.getValue());
            }
        }
        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<ParcelableLruCache> CREATOR
            = new Parcelable.Creator<ParcelableLruCache>() {
        public ParcelableLruCache createFromParcel(Parcel in) {
            return new ParcelableLruCache(in);
        }

        public ParcelableLruCache[] newArray(int size) {
            return new ParcelableLruCache[size];
        }
    };
}
