package org.wikipedia.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.feed.becauseyouread.BecauseYouReadItemCard;
import org.wikipedia.search.SearchResult;
import org.wikipedia.search.SearchResults;

import java.util.ArrayList;
import java.util.List;

public class MwApiResultPage {
    private int ns;
    private String title;
    private int index;

    @Nullable private Terms terms;
    @Nullable private Thumbnail thumbnail;

    public String title() {
        return title;
    }

    public void title(String title) {
        this.title = title;
    }

    @Nullable
    public String thumbUrl() {
        return thumbnail != null ? thumbnail.source : null;
    }

    public void thumbUrl(String source) {
        this.thumbnail = new Thumbnail(source);
    }

    @Nullable
    public String description() {
        return terms != null && terms.description != null ? terms.description[0] : null;
    }

    public void description(String description) {
        this.terms = new Terms(description);
    }

    public static List<BecauseYouReadItemCard> searchResultsToCards(SearchResults results, Site site) {
        List<BecauseYouReadItemCard> cards = new ArrayList<>();
        for (SearchResult result : results.getResults()) {
            cards.add(result.toCard(site));
        }
        return cards;
    }

    @NonNull
    public BecauseYouReadItemCard toCard(Site site) {
        PageTitle pageTitle = new PageTitle(title, site);
        pageTitle.setThumbUrl(thumbUrl());
        pageTitle.setDescription(description());
        return new BecauseYouReadItemCard(pageTitle);
    }

    @NonNull
    public SearchResult toSearchResult(Site site) {
        return new SearchResult(new PageTitle(title, site, thumbUrl(), description()));
    }

    static class Terms {
        private String[] description;

        Terms(String description) {
            this.description = new String[1];
            this.description[0] = description;
        }
    }

    static class Thumbnail {
        private String source;
        private int width;
        private int height;

        Thumbnail(String source) {
            this.source = source;
        }
    }

    static class PageProps {
        @Nullable private String disambiguation;
        @Nullable private String mainpage;
    }
}
