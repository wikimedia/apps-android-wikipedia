package org.wikipedia.test;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.PageTitle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public final class TestParcelUtil {
    public static void test(WikiSite wiki) throws Throwable {
        Parcel parcel = parcel(wiki);
        parcel.setDataPosition(0);
        WikiSite wiki2 = (WikiSite) unparcel(parcel, WikiSite.class);
        assertThat(wiki.getUri(), is(wiki2.getUri()));
        assertThat(wiki.getLanguageCode(), is(wiki2.getLanguageCode()));
    }

    public static void test(PageTitle title) throws Throwable {
        Parcel parcel = parcel(title);
        parcel.setDataPosition(0);
        PageTitle title2 = (PageTitle) unparcel(parcel, PageTitle.class);
        assertThat(title.getText(), is(title2.getText()));
        assertThat(title.namespace(), is(title2.namespace()));
        assertThat(title.getUri(), is(title2.getUri()));
    }

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
