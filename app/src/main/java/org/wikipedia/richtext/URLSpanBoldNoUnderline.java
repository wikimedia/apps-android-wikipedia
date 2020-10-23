package org.wikipedia.richtext;

import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class URLSpanBoldNoUnderline extends URLSpanNoUnderline {
    public static final Parcelable.Creator<URLSpanBoldNoUnderline> CREATOR = new Parcelable.Creator<URLSpanBoldNoUnderline>() {
        @Override public URLSpanBoldNoUnderline createFromParcel(Parcel source) {
            return new URLSpanBoldNoUnderline(source);
        }

        @Override public URLSpanBoldNoUnderline[] newArray(int size) {
            return new URLSpanBoldNoUnderline[size];
        }
    };

    public URLSpanBoldNoUnderline(String url) {
        super(url);
    }

    @Override public void updateDrawState(TextPaint paint) {
        super.updateDrawState(paint);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    @Override public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override public boolean equals(Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }

    protected URLSpanBoldNoUnderline(Parcel parcel) {
        super(parcel);
    }
}
