package org.wikipedia.editing;

import android.os.Parcel;
import android.os.Parcelable;
import org.wikipedia.Site;

// Handles only Image Captchas
public class CaptchaResult extends EditingResult {
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

    public String getCaptchaUrl(Site site) {
        return site.url("index.php") + "?title=Special:Captcha/image&wpCaptchaId=" + captchaId;
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
