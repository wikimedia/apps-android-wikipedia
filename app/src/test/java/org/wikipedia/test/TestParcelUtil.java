package org.wikipedia.test;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public final class TestParcelUtil {
    /** @param parcelable Must implement hashCode and equals */
    public static void test(Parcelable parcelable) throws Throwable {
        Parcel parcel = parcel(parcelable);

        parcel.setDataPosition(0);
        Parcelable unparceled = unparcel(parcel, parcelable.getClass());

        assertThat(parcelable, is(unparceled));
    }

    @NonNull private static Parcelable unparcel(@NonNull Parcel parcel,
                                                Class<? extends Parcelable> clazz) throws Throwable {
        Parcelable.Creator<?> creator = (Parcelable.Creator<?>) clazz.getField("CREATOR").get(null);
        return (Parcelable) creator.createFromParcel(parcel);
    }

    @NonNull private static Parcel parcel(@NonNull Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        return parcel;
    }

    private TestParcelUtil() { }
}
