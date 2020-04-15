package org.wikipedia.edit;

import android.os.Parcel;
import android.os.Parcelable;

public class EditSuccessResult extends EditResult {
    private final long revID;
    public EditSuccessResult(long revID) {
        super("Success");
        this.revID = revID;
    }

    private EditSuccessResult(Parcel in) {
        super(in);
        revID = in.readInt();
    }

    public long getRevID() {
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
