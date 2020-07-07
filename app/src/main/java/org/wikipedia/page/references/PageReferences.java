package org.wikipedia.page.references;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class PageReferences {
    private int selectedIndex;

    @Nullable private String tid;
    @Nullable private Reference[] referencesGroup;

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @NonNull public List<Reference> getReferencesGroup() {
        return referencesGroup == null ? Collections.emptyList() : Arrays.asList(referencesGroup);
    }

    public static class Reference  {
        @Nullable private String id;
        @Nullable private String html;
        @Nullable private String href;
        private String text;

        @NonNull public String getContent() {
            return StringUtils.defaultString(html);
        }

        @NonNull public String getText() {
            return StringUtils.defaultString(text);
        }

        @NonNull public String getHref() {
            return StringUtils.defaultString(href);
        }
    }
}
