package org.wikipedia.edit;

import android.os.Parcel;
import android.os.Parcelable;

public class EditSpamBlacklistResult extends EditResult {
    private final String domain;
    public EditSpamBlacklistResult(String domain) {
        super("Failure");
        this.domain = domain;
    }

    protected EditSpamBlacklistResult(Parcel in) {
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

    public static final Parcelable.Creator<EditSpamBlacklistResult> CREATOR
            = new Parcelable.Creator<EditSpamBlacklistResult>() {
        @Override
        public EditSpamBlacklistResult createFromParcel(Parcel in) {
            return new EditSpamBlacklistResult(in);
        }

        @Override
        public EditSpamBlacklistResult[] newArray(int size) {
            return new EditSpamBlacklistResult[size];
        }
    };
}
