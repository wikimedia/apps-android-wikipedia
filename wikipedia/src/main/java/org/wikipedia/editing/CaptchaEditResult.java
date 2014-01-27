package org.wikipedia.editing;

import android.os.Parcel;
import android.os.Parcelable;
import org.wikipedia.Site;

// Handles only Image Captchas
public class CaptchaEditResult extends EditingResult {
    private final String captchaId;

    public CaptchaEditResult(String captchaId) {
        super("Failure");
        this.captchaId = captchaId;
    }

    protected CaptchaEditResult(Parcel in) {
        super(in);
        captchaId = in.readString();
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public String getCaptchaUrl(Site site) {
        return site.getFullUrl("/w/index.php?title=Special:Captcha/image&wpCaptchaId=" + captchaId);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(captchaId);
    }

    public static final Parcelable.Creator<CaptchaEditResult> CREATOR
            = new Parcelable.Creator<CaptchaEditResult>() {
        public CaptchaEditResult createFromParcel(Parcel in) {
            return new CaptchaEditResult(in);
        }

        public CaptchaEditResult[] newArray(int size) {
            return new CaptchaEditResult[size];
        }
    };

}
