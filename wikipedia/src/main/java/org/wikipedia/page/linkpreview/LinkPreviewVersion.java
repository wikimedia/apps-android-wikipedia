package org.wikipedia.page.linkpreview;

import org.wikipedia.WikipediaApp;

public final class LinkPreviewVersion {

    // TODO: remove toggle when ready for production.
    // Let's toggle between three possible behaviors:
    // A) not show a link preview, and go directly to the target article (weight: 50%)
    // B) show link preview prototype 1 (weight: 25%)
    // C) show link preview prototype 2 (weight: 25%)
    private static final int LINK_PREVIEW_TOGGLE_WEIGHT = 4;

    // Take the app install id modulo 4 to get our toggle value.
    // Values per the schema at https://meta.wikimedia.org/w/index.php?title=Schema:MobileWikiAppLinkPreview:
    // 0: No link preview prototype
    // 1: Link preview prototype A
    // 2: Link preview prototype B
    // Return results 0, 1, or 2.  For result of 3, also return 0 (no preview).
    // (For building one-off APKs for testing specific prototypes, hard-code version to 1 or 2.)
    public static int getVersion(WikipediaApp app) {
        return ((app.getABTestingID() % LINK_PREVIEW_TOGGLE_WEIGHT) > 2) ? 0 : (app.getABTestingID() % LINK_PREVIEW_TOGGLE_WEIGHT);
    }

    private LinkPreviewVersion() {
    }
}
