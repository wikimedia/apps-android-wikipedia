package org.wikipedia.page.references;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused,NullableProblems")
public class References {
    @Nullable private String revision;
    @Nullable private String tid;
    @Nullable @SerializedName("references_by_id") private Map<String, Reference> referencesMap;

    @NonNull
    public Map<String, Reference> getReferencesMap() {
        return referencesMap == null ? Collections.emptyMap() : referencesMap;
    }

    public static class Reference  {
        @Nullable @SerializedName("back_links") private List<ReferenceBackLink> backLinks;
        @Nullable private ReferenceContent content;
        private String text;

        @NonNull
        public String getContent() {
            return content == null ? "" : content.getHtml();
        }

        @NonNull
        public List<ReferenceBackLink> getBackLinks() {
            return backLinks == null ? Collections.emptyList() : backLinks;
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
        @Nullable private String html;

        @NonNull
        public String getHtml() {
            return StringUtils.defaultString(html);
        }
    }

    public static class ReferenceBackLink  {
        @Nullable private String href;

        @NonNull
        public String getHref() {
            return StringUtils.defaultString(href);
        }
    }
}
