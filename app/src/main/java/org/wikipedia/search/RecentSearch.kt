package org.wikipedia.search

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class RecentSearch : Parcelable {
    val text: String?
    val timestamp: Date

    @JvmOverloads
    constructor(text: String?, timestamp: Date = Date()) {
        this.text = text
        this.timestamp = timestamp
    }

    override fun equals(other: Any?): Boolean {
        if (other !is RecentSearch) {
            return false
        }
        return text == other.text
    }

    override fun hashCode(): Int {
        return text.hashCode()
    }

    override fun toString(): String {
        return ("RecentSearch{" +
                "text=" + text +
                ", timestamp=" + timestamp.time +
                '}')
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(text)
        dest.writeLong(timestamp.time)
    }

    private constructor(`in`: Parcel) {
        text = `in`.readString()
        timestamp = Date(`in`.readLong())
    }

    companion object {
        @JvmField
        val DATABASE_TABLE = RecentSearchDatabaseTable()
        val CREATOR: Parcelable.Creator<RecentSearch> = object : Parcelable.Creator<RecentSearch> {
            override fun createFromParcel(`in`: Parcel): RecentSearch {
                return RecentSearch(`in`)
            }

            override fun newArray(size: Int): Array<RecentSearch?> {
                return arrayOfNulls(size)
            }
        }
    }
}
