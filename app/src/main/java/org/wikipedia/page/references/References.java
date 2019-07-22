package org.wikipedia.page.references;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
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
        @SuppressWarnings("unused,NullableProblems") @NonNull @SerializedName("back_links") private List<ReferenceBackLink> backLinks;
        @SuppressWarnings("unused,NullableProblems") @NonNull private ReferenceContent content;
        private String text;

        @NonNull
        public String getContent() {
            return content.getHtml();
        }

        @NonNull
        public List<ReferenceBackLink> getBackLinks() {
            return backLinks;
        }

        public void setText(@NonNull String text) {
            this.text = text;
        }

        @NonNull
        public String getText() {
            return text;
        }
    }

    private static class ReferenceContent  {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String html;

        @NonNull
        public String getHtml() {
            return StringUtils.defaultString(html);
        }
    }

    public static class ReferenceBackLink  {
        @SuppressWarnings("unused,NullableProblems") @NonNull private String href;

        @NonNull
        public String getHref() {
            return StringUtils.defaultString(href);
        }
    }
}
