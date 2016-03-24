package org.wikipedia.editing;

import android.os.Parcel;
import android.os.Parcelable;

public class SpamBlacklistEditResult extends EditingResult {
    private final String domain;
    public SpamBlacklistEditResult(String domain) {
        super("Failure");
        this.domain = domain;
    }

    protected SpamBlacklistEditResult(Parcel in) {
        super(in);
        domain = in.readString();
    }

    public String getDomain() {
        return domain;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(domain);
    }

    public static final Parcelable.Creator<SpamBlacklistEditResult> CREATOR
            = new Parcelable.Creator<SpamBlacklistEditResult>() {
        @Override
        public SpamBlacklistEditResult createFromParcel(Parcel in) {
            return new SpamBlacklistEditResult(in);
        }

        @Override
        public SpamBlacklistEditResult[] newArray(int size) {
            return new SpamBlacklistEditResult[size];
        }
    };
}
