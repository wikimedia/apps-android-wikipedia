package org.wikipedia.page.references;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class References {
    @SuppressWarnings("unused,NullableProblems")  @NonNull private String revision;
    @SuppressWarnings("unused,NullableProblems") @NonNull private String tid;
    @SuppressWarnings("unused,NullableProblems") @NonNull @SerializedName("references_by_id") private Map<String, Reference> referencesMap;

    @NonNull
    public Map<String, Reference> getReferencesMap() {
        return referencesMap;
    }

    public static class Reference  {
        @SuppressWarnings("unused,NullableProblems") @NonNull private ReferenceContent content;

        @NonNull
        public String getContent() {
            return content.getHtml();
        }
    }

    private static class ReferenceContent  {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String html;

        @NonNull
        public String getHtml() {
            return StringUtils.defaultString(html);
        }
    }
}
