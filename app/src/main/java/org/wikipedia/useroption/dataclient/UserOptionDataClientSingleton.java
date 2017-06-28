package org.wikipedia.useroption.dataclient;

import org.wikipedia.dataclient.WikiSite;

public final class UserOptionDataClientSingleton {
    public static DefaultUserOptionDataClient instance() {
        return LazyHolder.INSTANCE;
    }

    private UserOptionDataClientSingleton() { }

    private static class LazyHolder {
        private static final DefaultUserOptionDataClient INSTANCE = instance();

        private static DefaultUserOptionDataClient instance() {
            WikiSite wiki = new WikiSite("meta.wikimedia.org", "");
            return new DefaultUserOptionDataClient(wiki);
        }
    }
}
