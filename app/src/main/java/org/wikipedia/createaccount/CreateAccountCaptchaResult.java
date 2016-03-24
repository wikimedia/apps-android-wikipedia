package org.wikipedia.createaccount;

import android.os.Parcel;
import android.os.Parcelable;
import org.wikipedia.editing.CaptchaResult;

public class CreateAccountCaptchaResult extends CreateAccountResult {
    private final CaptchaResult captchaResult;

    public CreateAccountCaptchaResult(CaptchaResult captchaResult) {
        super("NeedCaptcha");
        this.captchaResult = captchaResult;
    }

    public CaptchaResult getCaptchaResult() {
        return captchaResult;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeParcelable(captchaResult, flags);
    }

    private CreateAccountCaptchaResult(Parcel in) {
        super(in);
        captchaResult = in.readParcelable(CaptchaResult.class.getClassLoader());
    }

    public static final Parcelable.Creator<CreateAccountCaptchaResult> CREATOR
            = new Parcelable.Creator<CreateAccountCaptchaResult>() {
        @Override
        public CreateAccountCaptchaResult createFromParcel(Parcel in) {
            return new CreateAccountCaptchaResult(in);
        }

        @Override
        public CreateAccountCaptchaResult[] newArray(int size) {
            return new CreateAccountCaptchaResult[size];
        }
    };

}
