package org.wikipedia.createaccount.authmanager;

import android.os.Parcel;
import android.os.Parcelable;

public class AMCreateAccountCaptchaResult extends AMCreateAccountResult {

    public AMCreateAccountCaptchaResult() {
        super("FAIL", "Incorrect or missing CAPTCHA.");
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
    }

    private AMCreateAccountCaptchaResult(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<AMCreateAccountCaptchaResult> CREATOR
            = new Parcelable.Creator<AMCreateAccountCaptchaResult>() {
        @Override
        public AMCreateAccountCaptchaResult createFromParcel(Parcel in) {
            return new AMCreateAccountCaptchaResult(in);
        }

        @Override
        public AMCreateAccountCaptchaResult[] newArray(int size) {
            return new AMCreateAccountCaptchaResult[size];
        }
    };
}