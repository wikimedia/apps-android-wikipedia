package org.wikipedia.edit.richtext;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;

public class ColorSpanEx extends ForegroundColorSpan implements SpanExtents {
    public static final Parcelable.Creator<ColorSpanEx> CREATOR = new Parcelable.Creator<ColorSpanEx>() {
        @Override public ColorSpanEx createFromParcel(Parcel source) {
            return new ColorSpanEx(source);
        }

        @Override public ColorSpanEx[] newArray(int size) {
            return new ColorSpanEx[size];
        }
    };

    private int spanStart;
    private int spanEnd;
    private SyntaxRule syntaxRule;
    @ColorInt private int backColor;

    public ColorSpanEx(@ColorInt int foreColor, @ColorInt int backColor, int spanStart, SyntaxRule syntaxRule) {
        super(foreColor);
        this.spanStart = spanStart;
        this.syntaxRule = syntaxRule;
        this.backColor = backColor;
    }

    public ColorSpanEx(@NonNull Parcel src) {
        super(src.readInt());
        this.spanStart = src.readInt();
        this.spanEnd = src.readInt();
        this.syntaxRule = GsonUnmarshaller.unmarshal(SyntaxRule.class, src.readString());
        this.backColor = src.readInt();
    }

    @Override
    public void updateDrawState(@NonNull TextPaint tp) {
        tp.bgColor = backColor;
        super.updateDrawState(tp);
    }

    @Override
    public int getStart() {
        return spanStart;
    }

    @Override
    public void setStart(int start) {
        spanStart = start;
    }

    @Override
    public int getEnd() {
        return spanEnd;
    }

    @Override
    public void setEnd(int end) {
        spanEnd = end;
    }

    @Override
    public SyntaxRule getSyntaxRule() {
        return syntaxRule;
    }

    @Override
    public void setSyntaxRule(SyntaxRule item) {
        syntaxRule = item;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(spanStart);
        dest.writeInt(spanEnd);
        dest.writeString(GsonMarshaller.marshal(syntaxRule));
        dest.writeInt(backColor);
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
}
