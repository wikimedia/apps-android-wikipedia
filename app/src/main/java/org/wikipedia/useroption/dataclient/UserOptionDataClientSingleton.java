package org.wikipedia.useroption.dataclient;

import org.wikipedia.dataclient.WikiSite;

public final class UserOptionDataClientSingleton {
    public static UserOptionDataClient instance() {
        return LazyHolder.INSTANCE;
    }

    private UserOptionDataClientSingleton() { }

    private static class LazyHolder {
        private static final UserOptionDataClient INSTANCE = instance();

        private static UserOptionDataClient instance() {
            WikiSite wiki = new WikiSite("meta.wikimedia.org", "");
            return new UserOptionDataClient(wiki);
        }
    }
}
