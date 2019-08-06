package org.wikipedia.gallery;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

public class TextInfo implements Serializable {
    @SuppressWarnings("unused,NullableProblems") @Nullable private String html;
    @SuppressWarnings("unused,NullableProblems") @Nullable private String text;
    @SuppressWarnings("unused,NullableProblems") @Nullable private String lang;

    @NonNull
    public String getHtml() {
        return StringUtils.defaultString(html);
    }

    @NonNull
    public String getText() {
        return StringUtils.defaultString(text);
    }

    @NonNull
    public String getLang() {
        return StringUtils.defaultString(lang);
    }
}
