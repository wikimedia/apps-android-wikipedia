package org.wikipedia.createaccount;

import android.os.Parcel;
import android.os.Parcelable;

public class CreateAccountSuccessResult extends CreateAccountResult implements Parcelable {
    private final String username;

    public CreateAccountSuccessResult(String username) {
        super("Success");
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

    protected CreateAccountSuccessResult(Parcel in) {
        super(in);
        username = in.readString();
    }

    public static final Creator<CreateAccountSuccessResult> CREATOR
            = new Creator<CreateAccountSuccessResult>() {
        @Override
        public CreateAccountSuccessResult createFromParcel(Parcel in) {
            return new CreateAccountSuccessResult(in);
        }

        @Override
        public CreateAccountSuccessResult[] newArray(int size) {
            return new CreateAccountSuccessResult[size];
        }
    };

}
