package org.wikipedia.page;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.util.UriUtil;

import static org.wikipedia.util.UriUtil.decodeURL;
import static org.wikipedia.util.UriUtil.handleExternalLink;

/**
 * Handles any html links coming from a {@link org.wikipedia.page.PageFragment}
 */
public abstract class LinkHandler implements CommunicationBridge.JSEventListener, LinkMovementMethodExt.UrlHandler {
    private final Context context;

    public LinkHandler(Context context, CommunicationBridge bridge) {
        this(context);

        bridge.addListener("linkClicked", this);
    }

    public LinkHandler(Context context) {
        this.context = context;
    }

    public abstract void onPageLinkClicked(String anchor);

    public abstract void onInternalLinkClicked(PageTitle title);

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            String href = decodeURL(messagePayload.getString("href"));
            onUrlClick(href, messagePayload.getString("title"));
        } catch (IllegalArgumentException e) {
            // The URL is malformed and URL decoder can't understand it. Just do nothing.
            Log.d("Wikipedia", "A malformed URL was tapped.");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUrlClick(@NonNull String href, @Nullable String titleString) {
        if (href.startsWith("//")) {
            // That's a protocol specific link! Make it https!
            href = "https:" + href;
        }
        Log.d("Wikipedia", "Link clicked was " + href);
        if (href.startsWith("/wiki/")) {
            PageTitle title = PageTitle.withSeparateFragment(titleString, UriUtil.getFragment(href), getWikiSite());
            onInternalLinkClicked(title);
        } else if (href.startsWith("#")) {
            onPageLinkClicked(href.substring(1));
        } else {
            Uri uri = Uri.parse(href);
            String authority = uri.getAuthority();
            // FIXME: Make this more complete, only to not handle URIs that contain unsupported actions
            if (authority != null && WikiSite.supportedAuthority(authority) && uri.getPath().startsWith("/wiki/")) {
                WikiSite wiki = new WikiSite(authority, getWikiSite().languageCode());
                PageTitle title = wiki.titleForUri(uri);
                onInternalLinkClicked(title);
            } else {
                // if it's a /w/ URI, turn it into a full URI and go external
                if (href.startsWith("/w/")) {
                    href = String.format("%1$s://%2$s", getWikiSite().scheme(), getWikiSite().authority()) + href;
                }
                handleExternalLink(context, Uri.parse(href));
            }
        }
    }

    public abstract WikiSite getWikiSite();
}
