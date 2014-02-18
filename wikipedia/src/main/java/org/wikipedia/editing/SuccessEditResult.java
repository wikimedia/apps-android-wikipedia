package org.wikipedia.editing;

import android.os.*;

public class SuccessEditResult extends EditingResult {
    public SuccessEditResult() {
        super("Success");
    }

    private SuccessEditResult(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<SuccessEditResult> CREATOR
            = new Parcelable.Creator<SuccessEditResult>() {
        public SuccessEditResult createFromParcel(Parcel in) {
            return new SuccessEditResult(in);
        }

        public SuccessEditResult[] newArray(int size) {
            return new SuccessEditResult[size];
        }
    };
}
