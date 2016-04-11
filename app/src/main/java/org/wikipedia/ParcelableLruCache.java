package org.wikipedia;

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
 * V can either be a String, Parcelable or a List<Parcelable>
 */
public class ParcelableLruCache<V> extends LruCache<String, V> implements Parcelable {
    private static final int TYPE_LIST = 1;
    private static final int TYPE_PARCELABLE = 2;
    private static final int TYPE_STRING = 3;

    private final int type;
    public ParcelableLruCache(int maxSize, Class<?> valueClass) {
        super(maxSize);
        if (valueClass.equals(String.class)) {
            type = TYPE_STRING;
        } else if (valueClass.isAssignableFrom(List.class)) {
            type = TYPE_LIST;
        } else {
            type = TYPE_PARCELABLE;
        }
    }

    private ParcelableLruCache(Parcel in) {
        super(in.readInt());
        type = in.readInt();
        Bundle contents = in.readBundle();
        contents.setClassLoader(getClass().getClassLoader());
        Set<String> keys = contents.keySet();
        for (String key : keys) {
            switch (type) {
                case TYPE_LIST:
                    //noinspection unchecked
                    put(key, (V) contents.getParcelableArrayList(key));
                    break;
                case TYPE_PARCELABLE:
                    //noinspection unchecked
                    put(key, (V) contents.getParcelable(key));
                    break;
                case TYPE_STRING:
                    //noinspection unchecked
                    put(key, (V) contents.getString(key));
                    break;
                default:
                    throw new RuntimeException("Unknown key type encountered " + type);
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
        dest.writeInt(type);
        Map<String, V> snapshot = snapshot();
        Bundle bundle = new Bundle();
        for (Map.Entry<String, V> entry : snapshot.entrySet()) {
            switch (type) {
                case TYPE_LIST:
                    //noinspection unchecked
                    bundle.putParcelableArrayList(entry.getKey(),
                            (ArrayList<? extends Parcelable>) entry.getValue());
                    break;
                case TYPE_PARCELABLE:
                    bundle.putParcelable(entry.getKey(), (Parcelable) entry.getValue());
                    break;
                case TYPE_STRING:
                    bundle.putString(entry.getKey(), (String) entry.getValue());
                    break;
                default:
                    throw new RuntimeException("Unknown key type encountered " + type);
            }
        }
        dest.writeBundle(bundle);
    }

    public static final Parcelable.Creator<ParcelableLruCache<?>> CREATOR
            = new Parcelable.Creator<ParcelableLruCache<?>>() {
        @Override
        public ParcelableLruCache<?> createFromParcel(Parcel in) {
            return new ParcelableLruCache<>(in);
        }

        @Override
        public ParcelableLruCache<?>[] newArray(int size) {
            return new ParcelableLruCache<?>[size];
        }
    };
}
