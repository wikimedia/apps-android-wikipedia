package org.wikimedia.wikipedia;

/**
 * Value object representing the title of a page.
 *
 * Points to a specific page in a specific namespace on a specific site.
 * Is immutable.
 */
public class PageTitle {
    private final String namespace;
    private final String title;

    public PageTitle(final String namesapce, final String title) {
        this.namespace = namesapce;
        this.title = title; //FIXME: Actually normalize this!
    }

    public String getNamespace() {
        return namespace;
    }

    public String getTitle() {
        return title;
    }
}
