package org.wikipedia.analytics;

import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageTitle;

import java.util.Hashtable;
import java.util.Map;

/**
 * Creates and stores analytics tracking funnels.
 */
public class FunnelManager {
    private final WikipediaApp app;
    private final Map<PageTitle, EditFunnel> editFunnels = new Hashtable<>();

    public FunnelManager(WikipediaApp app) {
        this.app = app;
    }

    public EditFunnel getEditFunnel(PageTitle title) {
        return editFunnels.computeIfAbsent(title, key -> new EditFunnel(app, key));
    }
}
