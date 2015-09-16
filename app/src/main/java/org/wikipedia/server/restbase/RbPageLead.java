package org.wikipedia.server.restbase;

import org.wikipedia.Utils;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.Section;
import org.wikipedia.server.PageLead;
import org.wikipedia.server.PageLeadProperties;
import org.wikipedia.util.log.L;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import android.support.annotation.Nullable;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Gson POJO for loading the first stage of page content.
 */
public class RbPageLead implements PageLead, PageLeadProperties {
    @Expose private RbServiceError error;
    @Expose private int id;
    @Expose private long revision;
    @Expose @Nullable private String lastmodified;
    @Expose @Nullable private String displaytitle;
    @Expose @Nullable private String redirected;
    @Expose @Nullable private String normalizedtitle;
    @Expose @Nullable private String extract;
    @Expose private int languagecount;
    @Expose private boolean editable;
    @Expose private boolean mainpage;
    @Expose private boolean disambiguation;
    @Expose @Nullable private String description;
    @Expose @Nullable private Image image;
    @Expose @Nullable private Protection protection;
    @Expose @Nullable private List<Section> sections;

    private transient int leadImageThumbWidth;

    @Override
    public boolean hasError() {
        return error != null || sections == null;
    }

    @Nullable
    public RbServiceError getError() {
        return error;
    }

    public void logError(String message) {
        if (error != null) {
            message += ": " + error.toString();
        }
        L.e(message);
    }

    /** Note: before using this check that #getMobileview != null */
    @Override
    public Page toPage(PageTitle title) {
        return new Page(adjustPageTitle(title),
                getSections(),
                toPageProperties());
    }

    /* package */ PageTitle adjustPageTitle(PageTitle title) {
        if (redirected != null) {
            // Handle redirects properly.
            title = new PageTitle(redirected, title.getSite(), title.getThumbUrl());
        } else if (normalizedtitle != null) {
            // We care about the normalized title only if we were not redirected
            title = new PageTitle(normalizedtitle, title.getSite(), title.getThumbUrl());
        }
        return title;
    }

    public String getLeadSectionContent() {
        if (sections != null) {
            return sections.get(0).getContent();
        } else {
            return "";
        }
    }

    /** Converter */
    public PageProperties toPageProperties() {
        return new PageProperties(this);
    }

    public int getId() {
        return id;
    }

    public long getRevision() {
        return revision;
    }

    @Nullable
    public String getLastModified() {
        return lastmodified;
    }

    @Nullable
    public String getExtract() {
        return extract;
    }

    public int getLanguageCount() {
        return languagecount;
    }

    @Nullable
    public String getDisplayTitle() {
        return displaytitle;
    }

    @Nullable
    public String getRedirected() {
        return redirected;
    }

    @Nullable
    public String getNormalizedTitle() {
        return normalizedtitle;
    }

    @Nullable
    public String getDescription() {
        return description != null ? Utils.capitalizeFirstChar(description) : null;
    }

    @Nullable
    public String getLeadImageUrl() {
        return image != null ? image.getUrl(leadImageThumbWidth) : null;
    }

    @Nullable
    public String getLeadImageName() {
        return image != null ? image.getFile() : null;
    }

    @Nullable
    public String getFirstAllowedEditorRole() {
        return protection != null ? protection.getFirstAllowedEditorRole() : null;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isMainPage() {
        return mainpage;
    }

    public boolean isDisambiguation() {
        return disambiguation;
    }

    @Nullable
    public List<Section> getSections() {
        return sections;
    }

    public void setLeadImageThumbWidth(int leadImageThumbWidth) {
        this.leadImageThumbWidth = leadImageThumbWidth;
    }


    /**
     * For the lead image File: page name
     */
    public static class Image {
        @Expose private String file;
        @Expose private ThumbUrls urls;

        public String getFile() {
            return file;
        }

        @Nullable
        public String getUrl(int leadImageThumbWidth) {
            return urls != null ? urls.get(leadImageThumbWidth) : null;
        }
    }


    /**
     * For the lead image URLs
     */
    public static class ThumbUrls {
        private static final int SMALL = 640;
        private static final int MEDIUM = 800;
        private static final int LARGE = 1024;
        @Expose @SerializedName("640") private String small;
        @Expose @SerializedName("800") private String medium;
        @Expose @SerializedName("1024") private String large;

        @Nullable
        public String get(int leadImageThumbWidth) {
            switch (leadImageThumbWidth) {
                case SMALL:
                    return small;
                case MEDIUM:
                    return medium;
                case LARGE:
                    return large;
                default:
                    return null;
            }
        }
    }


    /**
     * Protection settings for this page
     */
    public static class Protection {
        @Expose private String[] edit;

        public Protection() {
            this.edit = new String[]{};
        }

        public Protection(String[] edit) {
            this.edit = edit;
        }

        // TODO should send them all, but callers need to be updated, too, (future patch)
        @Nullable
        public String getFirstAllowedEditorRole() {
            if (edit.length > 0) {
                return edit[0];
            }
            return null;
        }

        /**
         * Need a custom Deserializer since the mediawiki API provides an inconsistent API.
         * Sometimes it returns an object, and other times when it's empty it returns an empty
         * array. See https://phabricator.wikimedia.org/T69054
         */
        public static class Deserializer implements JsonDeserializer<Protection> {
            /**
             * Gson invokes this call-back method during deserialization when it encounters a field
             * of the specified type.
             * <p>In the implementation of this call-back method, you should consider invoking
             * {@link JsonDeserializationContext#deserialize(JsonElement, Type)} method to create
             * objects for any non-trivial field of the returned object. However, you should never
             * invoke it on the the same type passing {@code json} since that will cause an infinite
             * loop (Gson will call your call-back method again).
             *
             * @param jsonEl The Json data being deserialized
             * @param typeOfT The type of the Object to deserialize to
             * @param jdc The deserialization context
             * @return a deserialized object of the specified type typeOfT which is
             * a subclass of {@code T}
             * @throws JsonParseException if json is not in the expected format of {@code typeofT}
             */
            @Override
            public Protection deserialize(JsonElement jsonEl, Type typeOfT,
                                          JsonDeserializationContext jdc)
                    throws JsonParseException {
                if (jsonEl.isJsonArray()) {
                    JsonArray array = jsonEl.getAsJsonArray();
                    if (array.size() != 0) {
                        L.w("Unexpected array size " + array.toString());
                    }
                } else {
                    JsonElement editEl = jsonEl.getAsJsonObject().get("edit");
                    if (editEl != null) {
                        JsonArray editorRolesJsonArray = editEl.getAsJsonArray();
                        String[] editorRoles = new String[editorRolesJsonArray.size()];
                        for (int i = 0; i < editorRolesJsonArray.size(); i++) {
                            editorRoles[i] = editorRolesJsonArray.get(i).getAsString();
                        }
                        return new Protection(editorRoles);
                    }
                }
                return new Protection();
            }
        }
    }
}