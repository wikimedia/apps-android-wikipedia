package org.wikipedia.edit.preview;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class EditPreviewHtml {
    @SuppressWarnings("unused")
    @Nullable
    private String mwAQ;


    @Nullable
    public String result() {
        return StringUtils.defaultString(mwAQ);
    }


}
