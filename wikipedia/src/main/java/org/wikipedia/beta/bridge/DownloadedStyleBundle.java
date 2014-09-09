package org.wikipedia.beta.bridge;

import org.wikipedia.beta.WikipediaApp;

/**
 * A bundle of CSS files that have been downloaded by the app,
 * independent of the ones shipped in the assets folder.
 */
public class DownloadedStyleBundle extends StyleBundle {
    /**
     * Creates a new StyleBundle with a styles that have been downloaded.
     *
     * @param styles Array of CSS File names that are available together in
     */
    public DownloadedStyleBundle(String... styles) {
        super("file://" + WikipediaApp.getInstance().getFilesDir().toString() + "/", styles);
    }
}
