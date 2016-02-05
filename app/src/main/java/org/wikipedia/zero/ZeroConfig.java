package org.wikipedia.zero;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class ZeroConfig implements Parcelable {
    @NonNull private String message;
    private int foreground;
    private int background;
    @Nullable private String exitTitle;
    @Nullable private String exitWarning;
    @Nullable private String partnerInfoText;
    @Nullable private String partnerInfoUrl;

    public ZeroConfig(@NonNull String message, @NonNull String foreground, @NonNull String background,
                      @Nullable String exitTitle, @Nullable String exitWarning, @Nullable String partnerInfoText,
                      @Nullable String partnerInfoUrl) {
        this(message, Color.parseColor(foreground.toUpperCase()), Color.parseColor(background.toUpperCase()),
                exitTitle, exitWarning, partnerInfoText, partnerInfoUrl);
    }

    public ZeroConfig(@NonNull String message, int foreground, int background, @Nullable String exitTitle,
                      @Nullable String exitWarning, @Nullable String partnerInfoText, @Nullable String partnerInfoUrl) {
        this.message = message;
        this.foreground = foreground;
        this.background = background;
        this.exitTitle = exitTitle;
        this.exitWarning = exitWarning;
        this.partnerInfoText = partnerInfoText;
        this.partnerInfoUrl = partnerInfoUrl;
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
    public String partnerInfoUrl() {
        return partnerInfoUrl;
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
        return !(partnerInfoUrl != null ? !partnerInfoUrl.equals(that.partnerInfoUrl) : that.partnerInfoUrl != null);

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
    }

    @NonNull
    public static final Parcelable.Creator<ZeroConfig> CREATOR
            = new Parcelable.Creator<ZeroConfig>() {
        public ZeroConfig createFromParcel(Parcel in) {
            return new ZeroConfig(in);
        }

        public ZeroConfig[] newArray(int size) {
            return new ZeroConfig[size];
        }
    };

    public ZeroConfig(Parcel in) {
        this(in.readString(), in.readInt(), in.readInt(), in.readString(), in.readString(),
                in.readString(), in.readString());
    }
}