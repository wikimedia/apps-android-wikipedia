package org.wikipedia.dataclient.mwapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class MwParseResponse extends MwResponse {
    @Nullable private Parse parse;

    @NonNull public String getText() {
        return StringUtils.defaultString(parse != null ? parse.text : null);
    }

    private static class Parse {
        @Nullable private String text;
    }
}
