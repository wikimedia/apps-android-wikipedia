package org.wikipedia.test

import android.os.Parcel
import android.os.Parcelable
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle

object TestParcelUtil {
    @Throws(Throwable::class)
    fun test(title: PageTitle) {

        val parcel = parcel(title)
        parcel.setDataPosition(0)

        val title2 = unparcel(parcel, PageTitle::class.java) as PageTitle

        MatcherAssert.assertThat<String>(title.text, Matchers.equalTo(title2.text))
        MatcherAssert.assertThat<Namespace>(title.namespace(), Matchers.equalTo(title2.namespace()))

        MatcherAssert.assertThat<String>(title.uri, Matchers.equalTo(title2.uri))
    }

    @Throws(Throwable::class)
    fun test(parcelable: Parcelable) {

        val parcel = parcel(parcelable)

        parcel.setDataPosition(0)
        val unparceled = unparcel(parcel, parcelable.javaClass)

        MatcherAssert.assertThat<Parcelable>(parcelable, Matchers.equalTo(unparceled))
    }

    @Throws(Throwable::class)
    private fun unparcel(parcel: Parcel, clazz: Class<out Parcelable>): Parcelable {
        val creator = clazz.getField("CREATOR").get(null) as Parcelable.Creator<*>
        return creator.createFromParcel(parcel) as Parcelable
    }

    private fun parcel(parcelable: Parcelable): Parcel {
        val parcel = Parcel.obtain()
        parcelable.writeToParcel(parcel, 0)
        return parcel
    }
}
