package org.wikipedia.zero;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.wikipedia.model.BaseModel;

@SuppressWarnings("unused")
public class ZeroConfig extends BaseModel {
    @Nullable private String message;
    @Nullable private String background;
    @Nullable private String foreground;
    @Nullable private String exitTitle;
    @Nullable private String exitWarning;
    @Nullable private String partnerInfoText;
    @Nullable private String partnerInfoUrl;
    @Nullable private String bannerUrl;

    public boolean isEligible() {
        return message != null;
    }

    @Nullable public String getMessage() {
        return message;
    }

    public void setMessage(@NonNull String message) {
        this.message = message;
    }

    @ColorInt public int getBackground() {
        return !TextUtils.isEmpty(background) ? Color.parseColor(background) : Color.WHITE;
    }

    @ColorInt public int getForeground() {
        return !TextUtils.isEmpty(foreground) ? Color.parseColor(foreground) : Color.BLACK;
    }

    @Nullable String getExitTitle() {
        return exitTitle;
    }

    @Nullable String getExitWarning() {
        return exitWarning;
    }

    @Nullable String getPartnerInfoText() {
        return partnerInfoText;
    }

    @Nullable String getPartnerInfoUrl() {
        return partnerInfoUrl;
    }

    @Nullable String getBannerUrl() {
        return bannerUrl;
    }
}
