package org.wikipedia.dataclient.page;

import android.location.Location;

import org.wikipedia.page.Namespace;
import org.wikipedia.page.Section;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The main properties of a page
 */
public interface PageLeadProperties {

    int getId();

    @NonNull Namespace getNamespace();

    long getRevision();

    @Nullable
    String getLastModified();

    int getLanguageCount();

    @Nullable
    String getDisplayTitle();

    @Nullable
    String getTitlePronunciationUrl();

    @Nullable
    Location getGeo();

    @Nullable
    String getRedirected();

    @Nullable
    String getNormalizedTitle();

    @Nullable
    String getWikiBaseItem();

    @Nullable
    String getDescriptionSource();

    /**
     * @return Nullable URL with no scheme. For example, foo.bar.com/ instead of
     *         http://foo.bar.com/.
     */
    @Nullable
    String getLeadImageUrl(int leadImageWidth);

    @Nullable
    String getThumbUrl();

    @Nullable
    String getLeadImageFileName();

    @Nullable
    String getFirstAllowedEditorRole();

    boolean isEditable();

    boolean isMainPage();

    boolean isDisambiguation();

    @NonNull List<Section> getSections();
}
