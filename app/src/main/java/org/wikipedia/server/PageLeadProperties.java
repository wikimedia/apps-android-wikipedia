package org.wikipedia.server;

import org.wikipedia.page.Section;

import android.support.annotation.VisibleForTesting;

import java.util.List;

/**
 * The main properties of a page
 */
public interface PageLeadProperties {

    int getId();

    long getRevision();

    String getLastModified();

    int getLanguageCount();

    String getDisplayTitle();

    String getRedirected();

    String getNormalizedTitle();

    String getDescription();

    String getLeadImageUrl();

    String getLeadImageName();

    String getFirstAllowedEditorRole();

    boolean isEditable();

    boolean isMainPage();

    boolean isDisambiguation();

    @VisibleForTesting
    List<Section> getSections();
}
