package org.wikipedia.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.gallery.GalleryCollection;
import org.wikipedia.settings.RbSwitch;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a particular page along with its full contents.
 */
public class Page {
    @VisibleForTesting
    static final int MEDIAWIKI_ORIGIN = 0;
    @VisibleForTesting
    static final int RESTBASE_ORIGIN = 1;

    private final PageTitle title;
    private final List<Section> sections;
    private final PageProperties pageProperties;

    /**
     * The media gallery collection associated with this page.
     * This will be populated by the Gallery activity when necessary, and will be kept in
     * the page cache because the page itself is cached. Subsequent instances of the Gallery
     * activity will then be able to retrieve the page's gallery collection from cache.
     */
    private GalleryCollection galleryCollection;

    /**
     * An indicator what payload version the page content was originally retrieved from.
     * If it's set to RESTBASE_ORIGIN the it came from the Mobile Content Service
     * (via RESTBase). This is esp. useful for saved pages, so that an older saved page will get the
     * correct kind of DOM transformations applied.
     */
    private int version = MEDIAWIKI_ORIGIN;

    public GalleryCollection getGalleryCollection() {
        return galleryCollection;
    }
    public void setGalleryCollection(GalleryCollection collection) {
        galleryCollection = collection;
    }

    /** Regular constructor */
    public Page(@NonNull PageTitle title, @NonNull List<Section> sections,
                @NonNull PageProperties pageProperties) {
        if (RbSwitch.INSTANCE.isRestBaseEnabled(title.getSite())) {
            this.version = RESTBASE_ORIGIN;
        }
        this.title = title;
        this.sections = sections;
        this.pageProperties = pageProperties;
    }

    @VisibleForTesting
    Page(@NonNull PageTitle title, @NonNull List<Section> sections,
         @NonNull PageProperties pageProperties, int version) {
        this.version = version;
        this.title = title;
        this.sections = sections;
        this.pageProperties = pageProperties;
    }

    /**
     * This could also be called getVersion but since there are only two different versions
     * I like to call it isFromRestBase to make it clearer.
     */
    public boolean isFromRestBase() {
        return version == RESTBASE_ORIGIN;
    }

    public PageTitle getTitle() {
        return title;
    }

    public List<Section> getSections() {
        return sections;
    }

    public String getDisplayTitle() {
        return pageProperties.getDisplayTitle();
    }

    @Nullable
    public String getTitlePronunciationUrl() {
        return getPageProperties().getTitlePronunciationUrl();
    }

    public PageProperties getPageProperties() {
        return pageProperties;
    }

    public boolean couldHaveReadMoreSection() {
        return getTitle().namespace() == Namespace.MAIN;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Page)) {
            return false;
        }

        Page other = (Page) o;
        return title.equals(other.title)
                && sections.equals(other.sections)
                && pageProperties.equals(other.pageProperties);
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + sections.hashCode();
        result = 31 * result + pageProperties.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Page{"
                + "title=" + title
                + ", sections=" + sections
                + ", pageProperties=" + pageProperties
                + ", version=" + version
                + '}';
    }

    public boolean isFilePage() {
        return title.isFilePage();
    }

    public boolean isMainPage() {
        return pageProperties.isMainPage();
    }

    public boolean isArticle() {
        return !isMainPage() && getTitle().namespace() == Namespace.MAIN;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.putOpt("version", version);
            json.putOpt("title", getTitle().toJSON());
            JSONArray sectionsJSON = new JSONArray();
            for (Section section : getSections()) {
                sectionsJSON.put(section.toJSON());
            }
            json.putOpt("sections", sectionsJSON);
            json.putOpt("properties", pageProperties.toJSON());
            if (galleryCollection != null) {
                json.put("gallery", galleryCollection.toJSON());
            }
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Page(JSONObject json) {
        version = json.optInt("version");
        title = new PageTitle(json.optJSONObject("title"));
        JSONArray sectionsJSON = json.optJSONArray("sections");
        sections = new ArrayList<>(sectionsJSON.length());
        for (int i = 0; i < sectionsJSON.length(); i++) {
            sections.add(Section.fromJson(sectionsJSON.optJSONObject(i)));
        }
        pageProperties = new PageProperties(json.optJSONObject("properties"));
        if (json.has("gallery")) {
            galleryCollection = new GalleryCollection(json.optJSONObject("gallery"));
        }
    }

    /** For old PHP API */
    public void addRemainingSections(List<Section> remainingSections) {
        sections.addAll(remainingSections);
    }

    /** For new RESTBase API */
    public void augmentRemainingSections(List<Section> remainingSections) {
        // TODO: Use Parsoid to request the same revision ID, so that there's no race condition
        // that can lead to a mismatched number of sections.
        Section leadSection = sections.get(0);
        sections.clear();
        sections.add(leadSection);
        sections.addAll(remainingSections);
    }

    public boolean isProtected() {
        return !getPageProperties().canEdit();
    }
}
