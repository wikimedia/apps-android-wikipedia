package org.wikipedia.analytics;

import org.wikipedia.PageTitle;
import org.wikipedia.Site;
import org.wikipedia.WikipediaApp;

import java.util.Hashtable;

/**
 * Creates and stores analytics tracking funnels.
 */
public class FunnelManager {
    private final WikipediaApp app;
    private final Hashtable<PageTitle, EditFunnel> editFunnels = new Hashtable<PageTitle, EditFunnel>();
    private final Hashtable<Site, SavedPagesFunnel> savedPageFunnels = new Hashtable<Site, SavedPagesFunnel>();

    public FunnelManager(WikipediaApp app) {
        this.app = app;
    }

    public EditFunnel getEditFunnel(PageTitle title) {
        if (!editFunnels.containsKey(title)) {
            editFunnels.put(title, new EditFunnel(app, title));
        }

        return editFunnels.get(title);
    }

    public SavedPagesFunnel getSavedPagesFunnel(Site site) {
        if (!savedPageFunnels.contains(site)) {
            savedPageFunnels.put(site, new SavedPagesFunnel(app, site));
        }

        return savedPageFunnels.get(site);
    }
}
