package org.wikipedia.suggestededits.provider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.gallery.GalleryItem;
import org.wikipedia.wikidata.Entities;

import java.util.Collections;
import java.util.Map;

public class SuggestedEditItem extends RbPageSummary {
    @Nullable @SerializedName("structured") private GalleryItem.StructuredData structuredData;
    @Nullable @SerializedName("wikibase_item") private Entities.Entity entity;

    @NonNull public Map<String, String> getCaptions() {
        return (structuredData != null && structuredData.getCaptions() != null) ? structuredData.getCaptions() : Collections.emptyMap();
    }

    @Nullable public Entities.Entity getEntity() {
        return entity;
    }
}
