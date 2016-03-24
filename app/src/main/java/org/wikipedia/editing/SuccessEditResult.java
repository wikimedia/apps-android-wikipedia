package org.wikipedia.editing;

import android.os.Parcel;
import android.os.Parcelable;

public class SuccessEditResult extends EditingResult {
    private final int revID;
    public SuccessEditResult(int revID) {
        super("Success");
        this.revID = revID;
    }

    private SuccessEditResult(Parcel in) {
        super(in);
        revID = in.readInt();
    }

    public int getRevID() {
        return revID;
    }

    public static final Parcelable.Creator<SuccessEditResult> CREATOR
            = new Parcelable.Creator<SuccessEditResult>() {
        @Override
        public SuccessEditResult createFromParcel(Parcel in) {
            return new SuccessEditResult(in);
        }

        @Override
        public SuccessEditResult[] newArray(int size) {
            return new SuccessEditResult[size];
        }
    };
}
