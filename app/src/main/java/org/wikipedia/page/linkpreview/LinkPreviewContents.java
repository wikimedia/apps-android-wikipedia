package org.wikipedia.page.linkpreview;

import androidx.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.StringUtil;

public class LinkPreviewContents {
    private final PageTitle title;
    private final CharSequence extract;
    private final boolean disambiguation;

    public PageTitle getTitle() {
        return title;
    }

    public CharSequence getExtract() {
        return extract;
    }

    public boolean isDisambiguation() {
        return disambiguation;
    }

    LinkPreviewContents(@NonNull PageSummary pageSummary, @NonNull WikiSite wiki) {
        title = new PageTitle(pageSummary.getApiTitle(), wiki);
        disambiguation = pageSummary.getType().equals(PageSummary.TYPE_DISAMBIGUATION);
        String extractStr;
        extractStr = pageSummary.getExtractHtml();
        if (disambiguation) {
            extractStr = "<p>" + L10nUtil.getStringForArticleLanguage(title, R.string.link_preview_disambiguation_description) + "</p>" + extractStr;
        }
        extract = StringUtil.fromHtml(extractStr);
        title.setThumbUrl(pageSummary.getThumbnailUrl());
    }
}
