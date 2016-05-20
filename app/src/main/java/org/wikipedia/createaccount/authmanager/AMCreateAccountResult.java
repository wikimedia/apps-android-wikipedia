package org.wikipedia.createaccount.authmanager;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.wikipedia.createaccount.CompatCreateAccountResult;

public class AMCreateAccountResult extends CompatCreateAccountResult {
    @NonNull private final String status;
    @NonNull private final String message;

    public AMCreateAccountResult(@NonNull String status, @NonNull String message) {
        this.status = status;
        this.message = message;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(status);
        parcel.writeString(message);
    }

    protected AMCreateAccountResult(Parcel in) {
        status = in.readString();
        message = in.readString();
    }

    @NonNull
    public static final Parcelable.Creator<AMCreateAccountResult> CREATOR
            = new Parcelable.Creator<AMCreateAccountResult>() {
        @Override
        public AMCreateAccountResult createFromParcel(Parcel in) {
            return new AMCreateAccountResult(in);
        }

        @Override
        public AMCreateAccountResult[] newArray(int size) {
            return new AMCreateAccountResult[size];
        }
    };

}
