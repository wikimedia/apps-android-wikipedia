package org.wikipedia.edit.richtext;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.style.StyleSpan;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;

public class StyleSpanEx extends StyleSpan implements SpanExtents {
    public static final Parcelable.Creator<StyleSpanEx> CREATOR = new Parcelable.Creator<StyleSpanEx>() {
        @Override public StyleSpanEx createFromParcel(Parcel source) {
            return new StyleSpanEx(source);
        }

        @Override public StyleSpanEx[] newArray(int size) {
            return new StyleSpanEx[size];
        }
    };

    private int spanStart;
    private int spanEnd;
    private SyntaxRule syntaxRule;

    public StyleSpanEx(int style, int spanStart, SyntaxRule syntaxRule) {
        super(style);
        this.spanStart = spanStart;
        this.syntaxRule = syntaxRule;
    }

    public StyleSpanEx(@NonNull Parcel src) {
        super(src);
        this.spanStart = src.readInt();
        this.spanEnd = src.readInt();
        this.syntaxRule = GsonUnmarshaller.unmarshal(SyntaxRule.class, src.readString());
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
