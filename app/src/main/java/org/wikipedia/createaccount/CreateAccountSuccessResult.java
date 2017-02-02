package org.wikipedia.createaccount;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

class CreateAccountSuccessResult extends CreateAccountResult implements Parcelable {
    private String username;

    CreateAccountSuccessResult(@NonNull String username) {
        super("PASS", "Account created");
        this.username = username;
    }

    String getUsername() {
        return username;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(username);
    }

    private CreateAccountSuccessResult(Parcel in) {
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
