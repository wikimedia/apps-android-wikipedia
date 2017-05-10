package org.wikipedia.zero;


import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.model.BaseModel;

public class ZeroConfig extends BaseModel {
    @SuppressWarnings("unused") @Nullable private String message;
    @SuppressWarnings("unused") @ColorInt private int background = Color.WHITE;
    @SuppressWarnings("unused") @ColorInt private int foreground = Color.BLACK;
    @SuppressWarnings("unused") @Nullable private String exitTitle;
    @SuppressWarnings("unused") @Nullable private String exitWarning;
    @SuppressWarnings("unused") @Nullable private String partnerInfoText;
    @SuppressWarnings("unused") @Nullable private Uri partnerInfoUrl;
    @SuppressWarnings("unused") @Nullable private Uri bannerUrl;

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
        return background;
    }

    public void setBackground(@ColorInt int background) {
        this.background = background;
    }

    @ColorInt public int getForeground() {
        return foreground;
    }

    public void setForeground(@ColorInt int foreground) {
        this.foreground = foreground;
    }

    @Nullable String getExitTitle() {
        return exitTitle;
    }

    public void setExitTitle(@NonNull String exitTitle) {
        this.exitTitle = exitTitle;
    }

    @Nullable String getExitWarning() {
        return exitWarning;
    }

    public void setExitWarning(@NonNull String exitWarning) {
        this.exitWarning = exitWarning;
    }

    @Nullable String getPartnerInfoText() {
        return partnerInfoText;
    }

    public void setPartnerInfoText(@NonNull String partnerInfoText) {
        this.partnerInfoText = partnerInfoText;
    }

    @Nullable Uri getPartnerInfoUrl() {
        return partnerInfoUrl;
    }

    public void setPartnerInfoUrl(@NonNull Uri partnerInfoUrl) {
        this.partnerInfoUrl = partnerInfoUrl;
    }

    @Nullable Uri getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(@NonNull Uri bannerUrl) {
        this.bannerUrl = bannerUrl;
    }
}
