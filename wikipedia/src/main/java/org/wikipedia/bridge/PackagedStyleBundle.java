package org.wikipedia.bridge;

/**
 * A bundle of CSS files that are shipped with the app itself,
 * in the assets directory.
 */
public class PackagedStyleBundle extends StyleBundle {
    public PackagedStyleBundle(String... styles) {
        super("file:///android_asset/", styles);
    }
}
