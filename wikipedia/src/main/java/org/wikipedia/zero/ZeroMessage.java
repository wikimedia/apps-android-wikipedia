package org.wikipedia.zero;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

public class ZeroMessage implements Parcelable {
    private String msg;
    private int fg;
    private int bg;

    public ZeroMessage(String msg, String fg, String bg) {
        this(msg, Color.parseColor(fg.toUpperCase()), Color.parseColor(bg.toUpperCase()));
    }

    public ZeroMessage(String msg, int fg, int bg) {
        this.msg = msg;
        this.fg = fg;
        this.bg = bg;
    }

    public String getMsg() {
        return msg;
    }

    public int getFg() {
        return fg;
    }

    public int getBg() {
        return bg;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        try {
            return obj instanceof ZeroMessage && (obj == this || obj.hashCode() == this.hashCode());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.msg + ":" + this.fg + ":" + this.bg;
    }

    // Parcelable stuff
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.msg);
        out.writeInt(this.fg);
        out.writeInt(this.bg);
    }

    public static final Parcelable.Creator<ZeroMessage> CREATOR
            = new Parcelable.Creator<ZeroMessage>() {
        public ZeroMessage createFromParcel(Parcel in) {
            return new ZeroMessage(in);
        }

        public ZeroMessage[] newArray(int size) {
            return new ZeroMessage[size];
        }
    };


    public ZeroMessage(Parcel in) {
        this(in.readString(), in.readInt(), in.readInt());
    }
}