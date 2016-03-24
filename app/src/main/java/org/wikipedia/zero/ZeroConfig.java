package org.wikipedia.zero;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ZeroConfig implements Parcelable {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ZeroConfig that = (ZeroConfig) o;

        if (foreground != that.foreground) {
            return false;
        }
        if (background != that.background) {
            return false;
        }
        if (!message.equals(that.message)) {
            return false;
        }
        if (exitTitle != null ? !exitTitle.equals(that.exitTitle) : that.exitTitle != null) {
            return false;
        }
        if (exitWarning != null ? !exitWarning.equals(that.exitWarning) : that.exitWarning != null) {
            return false;
        }
        if (partnerInfoText != null ? !partnerInfoText.equals(that.partnerInfoText) : that.partnerInfoText != null) {
            return false;
        }
        if (partnerInfoUrl != null ? !partnerInfoUrl.equals(that.partnerInfoUrl) : that.partnerInfoUrl != null) {
            return false;
        }
        return !(bannerUrl != null ? !bannerUrl.equals(that.bannerUrl) : that.bannerUrl != null);

    }

    @Override
    public int hashCode() {
        int result = message.hashCode();
        result = 31 * result + foreground;
        result = 31 * result + background;
        result = 31 * result + (exitTitle != null ? exitTitle.hashCode() : 0);
        result = 31 * result + (exitWarning != null ? exitWarning.hashCode() : 0);
        result = 31 * result + (partnerInfoText != null ? partnerInfoText.hashCode() : 0);
        result = 31 * result + (partnerInfoUrl != null ? partnerInfoUrl.hashCode() : 0);
        result = 31 * result + (bannerUrl != null ? bannerUrl.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.message);
        out.writeInt(this.foreground);
        out.writeInt(this.background);
        out.writeString(this.exitTitle);
        out.writeString(this.exitWarning);
        out.writeString(this.partnerInfoText);
        out.writeString(this.partnerInfoUrl);
        out.writeString(this.bannerUrl);
    }

    @NonNull
    public static final Parcelable.Creator<ZeroConfig> CREATOR
            = new Parcelable.Creator<ZeroConfig>() {
        @Override
        public ZeroConfig createFromParcel(Parcel in) {
            return new ZeroConfig(in);
        }

        @Override
        public ZeroConfig[] newArray(int size) {
            return new ZeroConfig[size];
        }
    };

    public ZeroConfig(Parcel in) {
        new Builder(in.readString(), in.readInt(), in.readInt())
                .exitTitle(in.readString())
                .exitWarning(in.readString())
                .partnerInfoText(in.readString())
                .partnerInfoUrl(in.readString())
                .bannerUrl(in.readString())
        .build();
    }
}