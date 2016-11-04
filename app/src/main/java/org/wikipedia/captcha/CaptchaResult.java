package org.wikipedia.captcha;

import android.os.Parcel;
import android.os.Parcelable;

import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.edit.EditResult;

// Handles only Image Captchas
public class CaptchaResult extends EditResult {
    private final String captchaId;

    public CaptchaResult(String captchaId) {
        super("Failure");
        this.captchaId = captchaId;
    }

    protected CaptchaResult(Parcel in) {
        super(in);
        captchaId = in.readString();
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public String getCaptchaUrl(WikiSite wiki) {
        return wiki.url("index.php") + "?title=Special:Captcha/image&wpCaptchaId=" + captchaId;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(captchaId);
    }

    public static final Parcelable.Creator<CaptchaResult> CREATOR
            = new Parcelable.Creator<CaptchaResult>() {
        @Override
        public CaptchaResult createFromParcel(Parcel in) {
            return new CaptchaResult(in);
        }

        @Override
        public CaptchaResult[] newArray(int size) {
            return new CaptchaResult[size];
        }
    };
}
