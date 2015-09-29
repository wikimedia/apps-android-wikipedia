package org.wikipedia.page.linkpreview;

import org.json.JSONObject;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.RequestBuilder;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageQueryTask;
import org.wikipedia.page.PageTitle;

public class PreviewFetchTask extends PageQueryTask<LinkPreviewContents> {
    private final PageTitle title;

    public PreviewFetchTask(Api api, PageTitle title) {
        super(LOW_CONCURRENCY, api, title.getSite(), title);
        this.title = title;
    }

    @Override
    public void buildQueryParams(RequestBuilder builder) {
        /*
        Here's the rationale for this API call:
        We request 10 sentences from the lead section, and then re-parse the text using our own
        sentence parsing logic to end up with 2 sentences for the link preview. We trust our
        parsing logic more than TextExtracts because it's better-tailored to the user's
        Locale on the client side. For example, the TextExtracts extension incorrectly treats
        abbreviations like "i.e.", "B.C.", "Jr.", etc. as separate sentences, whereas our parser
        will leave those alone.

        Also, we no longer request "excharacters" from TextExtracts, since it has an issue where
        it's liable to return content that lies beyond the lead section, which might include
        unparsed wikitext, which we certainly don't want.
        */
        builder.param("prop", "extracts|pageimages|pageterms")
               .param("redirects", "true")
               .param("exsentences", "10")
               .param("explaintext", "true")
               .param("piprop", "thumbnail|name")
               .param("pithumbsize", Integer.toString(WikipediaApp.PREFERRED_THUMB_SIZE))
               .param("wbptterms", "description");
    }

    @Override
    public LinkPreviewContents processPage(int pageId, PageTitle pageTitle, JSONObject pageData) throws Throwable {
        return new LinkPreviewContents(pageData, title.getSite());
    }
}
