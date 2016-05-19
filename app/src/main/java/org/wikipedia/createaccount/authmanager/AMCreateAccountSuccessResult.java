package org.wikipedia.createaccount.authmanager;

import android.os.Parcel;
import android.os.Parcelable;

public class AMCreateAccountSuccessResult extends AMCreateAccountResult implements Parcelable {
    private final String username;

    public AMCreateAccountSuccessResult(String username) {
        super("PASS", "Account created");
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(username);
    }

    protected AMCreateAccountSuccessResult(Parcel in) {
        super(in);
        username = in.readString();
    }

    public static final Creator<AMCreateAccountSuccessResult> CREATOR
            = new Creator<AMCreateAccountSuccessResult>() {
        @Override
        public AMCreateAccountSuccessResult createFromParcel(Parcel in) {
            return new AMCreateAccountSuccessResult(in);
        }

        @Override
        public AMCreateAccountSuccessResult[] newArray(int size) {
            return new AMCreateAccountSuccessResult[size];
        }
    };
}
