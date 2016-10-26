package org.wikipedia.zero;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.model.BaseModel;

public final class ZeroConfig extends BaseModel {
    @NonNull private String message = "";
    private int foreground;
    private int background;
    @Nullable private String exitTitle;
    @Nullable private String exitWarning;
    @Nullable private String partnerInfoText;
    @Nullable private String partnerInfoUrl;
    @Nullable private String bannerUrl;

    public static class Builder {
        @NonNull private String message = "";
        private int foreground;
        private int background;
        @Nullable private String exitTitle;
        @Nullable private String exitWarning;
        @Nullable private String partnerInfoText;
        @Nullable private String partnerInfoUrl;
        @Nullable private String bannerUrl;

        public Builder(@NonNull String message, int foreground, int background) {
            this.message = message;
            this.foreground = foreground;
            this.background = background;
        }

        public Builder exitTitle(String string) {
            exitTitle = string;
            return this;
        }

        public Builder exitWarning(String string) {
            exitWarning = string;
            return this;
        }

        public Builder partnerInfoText(String string) {
            partnerInfoText = string;
            return this;
        }

        public Builder partnerInfoUrl(String string) {
            partnerInfoUrl = string;
            return this;
        }

        public Builder bannerUrl(String string) {
            bannerUrl = string;
            return this;
        }

        public ZeroConfig build() {
            return new ZeroConfig(this);
        }
    }

    private ZeroConfig(Builder builder) {
        message = builder.message;
        background = builder.background;
        foreground = builder.foreground;
        exitTitle = builder.exitTitle;
        exitWarning = builder.exitWarning;
        partnerInfoText = builder.partnerInfoText;
        partnerInfoUrl = builder.partnerInfoUrl;
        bannerUrl = builder.bannerUrl;
    }

    @NonNull
    public String getMessage() {
        return message;
    }

    public int getForeground() {
        return foreground;
    }

    public int getBackground() {
        return background;
    }

    @Nullable
    public String getExitTitle() {
        return exitTitle;
    }

    @Nullable
    public String getExitWarning() {
        return exitWarning;
    }

    @Nullable
    public String getPartnerInfoText() {
        return partnerInfoText;
    }

    @Nullable
    public String getPartnerInfoUrl() {
        return partnerInfoUrl;
    }

    @Nullable
    public String getBannerUrl() {
        return bannerUrl;
    }

    @Override
    public String toString() {
        return "ZeroConfig{"
                + "message='" + message + '\''
                + ", foreground=" + foreground
                + ", background=" + background
                + ", exitTitle='" + exitTitle + '\''
                + ", exitWarning='" + exitWarning + '\''
                + ", partnerInfoText='" + partnerInfoText + '\''
                + ", partnerInfoUrl='" + partnerInfoUrl + '\''
                + ", bannerUrl='" + bannerUrl + '\''
                + '}';
    }
}