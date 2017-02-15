package org.wikipedia.richtext;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.style.URLSpan;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class URLSpanNoUnderline extends URLSpan {
    public static final Parcelable.Creator<URLSpanNoUnderline> CREATOR = new Parcelable.Creator<URLSpanNoUnderline>() {
        @Override public URLSpanNoUnderline createFromParcel(Parcel source) {
            return new URLSpanNoUnderline(source);
        }

        @Override public URLSpanNoUnderline[] newArray(int size) {
            return new URLSpanNoUnderline[size];
        }
    };

    public URLSpanNoUnderline(String url) {
        super(url);
    }

    @Override public void updateDrawState(TextPaint paint) {
        super.updateDrawState(paint);
        paint.setUnderlineText(false);
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

    protected URLSpanNoUnderline(Parcel parcel) {
        super(parcel);
    }
}
