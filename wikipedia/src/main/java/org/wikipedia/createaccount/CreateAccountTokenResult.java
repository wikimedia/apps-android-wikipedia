package org.wikipedia.createaccount;

import android.os.*;
import org.wikipedia.editing.*;

public class CreateAccountTokenResult extends CreateAccountResult {
    private final CaptchaResult captchaResult;
    private final String token;

    public CreateAccountTokenResult(CaptchaResult captchaResult, String token) {
        super("needtoken");
        this.captchaResult = captchaResult;
        this.token = token;
    }

    public CaptchaResult getCaptchaResult() {
        return captchaResult;
    }

    public String getToken() {
        return token;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        super.writeToParcel(parcel, flags);
        parcel.writeString(token);
        parcel.writeParcelable(captchaResult, flags);
    }

    private CreateAccountTokenResult(Parcel in) {
        super(in);
        token = in.readString();
        captchaResult = in.readParcelable(CaptchaResult.class.getClassLoader());
    }

    public static final Parcelable.Creator<CreateAccountTokenResult> CREATOR
            = new Parcelable.Creator<CreateAccountTokenResult>() {
        public CreateAccountTokenResult createFromParcel(Parcel in) {
            return new CreateAccountTokenResult(in);
        }

        public CreateAccountTokenResult[] newArray(int size) {
            return new CreateAccountTokenResult[size];
        }
    };

}
