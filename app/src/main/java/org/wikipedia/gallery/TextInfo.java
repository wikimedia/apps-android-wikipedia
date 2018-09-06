package org.wikipedia.gallery;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class TextInfo implements Serializable {

    @SuppressWarnings("unused,NullableProblems") @Nullable private String html;
    @SuppressWarnings("unused,NullableProblems") @Nullable private String text;

    @NonNull
    public String getHtml() {
        return StringUtils.defaultString(html);
    }

    @NonNull
    public String getText() {
        return StringUtils.defaultString(text);
    }

    public void setHtml(@Nullable String html) {
        this.html = html;
    }

    public void setText(@Nullable String text) {
        this.text = text;
    }
}
