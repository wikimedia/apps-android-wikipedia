package org.wikipedia.createaccount;

import android.os.Parcel;
import android.os.Parcelable;

public class CreateAccountResult extends CompatCreateAccountResult {
    private final String result;

    public CreateAccountResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(result);
    }

    protected CreateAccountResult(Parcel in) {
        result = in.readString();
    }

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
