package org.wikipedia.edit;

import android.os.Parcel;
import android.os.Parcelable;

import org.wikipedia.model.BaseModel;

public abstract class EditResult extends BaseModel implements Parcelable {
    private final String result;

    public EditResult(String result) {
        this.result = result;
    }

    protected EditResult(Parcel in) {
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
