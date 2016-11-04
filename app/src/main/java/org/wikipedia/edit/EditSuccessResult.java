package org.wikipedia.edit;

import android.os.Parcel;
import android.os.Parcelable;

public class EditSuccessResult extends EditResult {
    private final int revID;
    public EditSuccessResult(int revID) {
        super("Success");
        this.revID = revID;
    }

    private EditSuccessResult(Parcel in) {
        super(in);
        revID = in.readInt();
    }

    public int getRevID() {
        return revID;
    }

    public static final Parcelable.Creator<EditSuccessResult> CREATOR
            = new Parcelable.Creator<EditSuccessResult>() {
        @Override
        public EditSuccessResult createFromParcel(Parcel in) {
            return new EditSuccessResult(in);
        }

        @Override
        public EditSuccessResult[] newArray(int size) {
            return new EditSuccessResult[size];
        }
    };
}
