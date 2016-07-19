package org.wikipedia.createaccount;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class CreateAccountResult implements Parcelable {
    @NonNull private final String status;
    @NonNull private final String message;

    public CreateAccountResult(@NonNull String status, @NonNull String message) {
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

    @Override
    public int describeContents() {
        return 0;
    }

    protected CreateAccountResult(Parcel in) {
        status = in.readString();
        message = in.readString();
    }

    @NonNull
    public static final Parcelable.Creator<CreateAccountResult> CREATOR
            = new Parcelable.Creator<CreateAccountResult>() {
        @Override
        public CreateAccountResult createFromParcel(Parcel in) {
            return new CreateAccountResult(in);
        }

        @Override
        public CreateAccountResult[] newArray(int size) {
            return new CreateAccountResult[size];
        }
    };

}
