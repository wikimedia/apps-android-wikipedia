package org.wikipedia.editing;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class EditingResult implements Parcelable {
    private final String result;

    public EditingResult(String result) {
        this.result = result;
    }

    protected EditingResult(Parcel in) {
        this.result = in.readString();
    }

    public String getResult() {
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(result);
    }
}
