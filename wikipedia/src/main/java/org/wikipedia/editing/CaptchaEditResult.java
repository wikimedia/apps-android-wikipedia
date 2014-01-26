package org.wikipedia.editing;

import android.os.Parcel;
import android.os.Parcelable;
import org.wikipedia.Site;

// Handles only Image Captchas
public class CaptchaEditResult extends EditingResult {
    private final String captchaId;
    private final String captchaPath;

    public CaptchaEditResult(String captchaId, String captchaPath) {
        super("Failure");
        this.captchaId = captchaId;
        this.captchaPath = captchaPath;
    }

    protected CaptchaEditResult(Parcel in) {
        super(in);
        captchaId = in.readString();
        captchaPath = in.readString();
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public String getCaptchaUrl(Site site) {
        return site.getFullUrl(captchaPath);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(captchaId);
        dest.writeString(captchaPath);
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
