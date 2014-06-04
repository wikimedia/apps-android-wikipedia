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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(username);
    }

    protected CreateAccountSuccessResult(Parcel in) {
        super(in);
        username = in.readString();
    }

    public static final Creator<CreateAccountSuccessResult> CREATOR
            = new Creator<CreateAccountSuccessResult>() {
        public CreateAccountSuccessResult createFromParcel(Parcel in) {
            return new CreateAccountSuccessResult(in);
        }

        public CreateAccountSuccessResult[] newArray(int size) {
            return new CreateAccountSuccessResult[size];
        }
    };

}
