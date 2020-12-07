package org.wikipedia.watchlist

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class Watchlist : Parcelable {
    val apiTitle: String?
    val displayTitle: String?
    val timestamp: Date
    val timePeriod: Long

    constructor(apiTitle: String,
                displayTitle: String,
                timestamp: Date,
                timePeriod: Long) {
        this.apiTitle = apiTitle
        this.displayTitle = displayTitle
        this.timestamp = timestamp
        this.timePeriod = timePeriod
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Watchlist) {
            return false
        }
        return apiTitle == other.apiTitle && displayTitle == other.displayTitle
    }

    override fun hashCode(): Int {
        var result = apiTitle.hashCode()
        result = 31 * result + displayTitle.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + timePeriod.toInt()
        return result
    }

    override fun toString(): String {
        return ("Watchlist{"
                + "apiTitle=" + apiTitle
                + ", displayTitle=" + displayTitle
                + ", timestamp=" + timestamp.time
                + ", timePeriod=" + timePeriod
                + '}')
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(apiTitle)
        dest.writeString(displayTitle)
        dest.writeLong(timestamp.time)
        dest.writeLong(timePeriod)
    }

    private constructor(`in`: Parcel) {
        apiTitle = `in`.readString()
        displayTitle = `in`.readString()
        timestamp = Date(`in`.readLong())
        timePeriod = `in`.readLong()
    }

    companion object {
        @JvmField val DATABASE_TABLE = WatchlistDatabaseTable()
        @JvmField val CREATOR: Parcelable.Creator<Watchlist?> = object : Parcelable.Creator<Watchlist?> {
            override fun createFromParcel(`in`: Parcel): Watchlist {
                return Watchlist(`in`)
            }

            override fun newArray(size: Int): Array<Watchlist?> {
                return arrayOfNulls(size)
            }
        }
    }
}