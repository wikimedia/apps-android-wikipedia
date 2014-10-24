package org.wikipedia.wikidata;

import org.wikipedia.Site;

/**
 * A Site for Wikidata API calls. One site for all languages.
 */
public class WikidataSite extends Site {
    public WikidataSite() {
        super("www.wikidata.org");
    }

    @Override
    public String getApiDomain() {
        return getDomain();
    }

    @Override
    public String getLanguage() {
        throw new UnsupportedOperationException("getLanguage not supported");
    }
}
