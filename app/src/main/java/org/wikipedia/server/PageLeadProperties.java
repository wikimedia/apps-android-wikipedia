package org.wikipedia.server;

import org.wikipedia.page.Section;

import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.List;

/**
 * The main properties of a page
 */
public interface PageLeadProperties {

    int getId();

    long getRevision();

    @Nullable
    String getLastModified();

    int getLanguageCount();

    @Nullable
    String getDisplayTitle();

    @Nullable
    String getRedirected();

    @Nullable
    String getNormalizedTitle();

    @Nullable
    String getDescription();

    @Nullable
    String getLeadImageUrl();

    @Nullable
    String getLeadImageName();

    @Nullable
    String getFirstAllowedEditorRole();

    boolean isEditable();

    boolean isMainPage();

    boolean isDisambiguation();

    @VisibleForTesting
    @Nullable
    List<Section> getSections();
}
