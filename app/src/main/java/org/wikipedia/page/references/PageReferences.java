package org.wikipedia.page.references;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused,NullableProblems")
public class PageReferences {
    @Nullable @SerializedName("selectedIndex") private int selectedIndex;

    @Nullable private String tid;
    @Nullable @SerializedName("referencesGroup") private Reference[] referencesGroup;

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Nullable public List<Reference> getReferencesGroup() {
        return referencesGroup == null ? Collections.emptyList() : Arrays.asList(referencesGroup);
    }

    public static class Reference  {
        @Nullable @SerializedName("id") private String id;
        @Nullable @SerializedName("html") private String content;
        @Nullable @SerializedName("href") private String href;
        private String text;

        @NonNull
        public String getContent() {
            return StringUtils.defaultString(content);
        }

        public void setText(@NonNull String text) {
            this.text = text;
        }

        @NonNull
        public String getText() {
            return text;
        }

        @Nullable
        public String getHref() {
            return StringUtils.defaultString(href);
        }
    }
}
