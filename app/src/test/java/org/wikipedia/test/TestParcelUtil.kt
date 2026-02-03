package org.wikipedia.test

import android.os.Parcel
import android.os.Parcelable
import org.junit.Assert.assertEquals
import org.wikipedia.page.PageTitle

object TestParcelUtil {
    @Throws(Throwable::class)
    fun test(title: PageTitle) {

        val parcel = parcel(title)
        parcel.setDataPosition(0)

        val title2 = unparcel(parcel, PageTitle::class.java) as PageTitle

        assertEquals(title.text, title2.text)
        assertEquals(title.namespace(), title2.namespace())

        assertEquals(title.uri, title2.uri)
    }

    @Throws(Throwable::class)
    fun test(parcelable: Parcelable) {

        val parcel = parcel(parcelable)

        parcel.setDataPosition(0)
        val unparceled = unparcel(parcel, parcelable.javaClass)

        assertEquals(parcelable, unparceled)
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
