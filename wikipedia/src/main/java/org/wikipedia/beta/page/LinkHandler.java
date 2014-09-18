package org.wikipedia.beta.page;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.beta.PageTitle;
import org.wikipedia.beta.Site;
import org.wikipedia.beta.Utils;
import org.wikipedia.beta.WikipediaApp;
import org.wikipedia.beta.bridge.CommunicationBridge;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Handles any html links coming from a {@link org.wikipedia.beta.page.PageViewFragment}
 */
public abstract class LinkHandler implements CommunicationBridge.JSEventListener, LinkMovementMethodExt.UrlHandler {
    private final Context context;
    private final Site currentSite;

    public LinkHandler(Context context, CommunicationBridge bridge, Site currentSite) {
        this(context, currentSite);

        bridge.addListener("linkClicked", this);
    }

    public LinkHandler(Context context, Site currentSite) {
        this.context = context;
        this.currentSite = currentSite;
    }

    public abstract void onPageLinkClicked(String anchor);

    public abstract void onInternalLinkClicked(PageTitle title);

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            String href = URLDecoder.decode(messagePayload.getString("href"), "UTF-8");
            onUrlClick(href);
        } catch (UnsupportedEncodingException e) {
            // will not happen
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUrlClick(String href) {
        if (href.startsWith("//")) {
            // That's a protocol specific link! Make it https!
            href = "https:" + href;
        }
        Log.d("Wikipedia", "Link clicked was " + href);
        if (href.startsWith("/wiki/")) {
            PageTitle title = currentSite.titleForInternalLink(href);
            onInternalLinkClicked(title);
        } else if (href.startsWith("#")) {
            onPageLinkClicked(href.substring(1));
        } else {
            Uri uri = Uri.parse(href);
            String authority = uri.getAuthority();
            // FIXME: Make this more complete, only to not handle URIs that contain unsupported actions
            if (authority != null && Site.isSupportedSite(authority) && uri.getPath().startsWith("/wiki/")) {
                Site site = new Site(authority);
                PageTitle title = site.titleForUri(uri);
                onInternalLinkClicked(title);
            } else {
                // if it's a /w/ URI, turn it into a full URI and go external
                if (href.startsWith("/w/")) {
                    href = String.format("%1$s://%2$s", WikipediaApp.getInstance().getNetworkProtocol(), currentSite.getDomain()) + href;
                }
                Utils.handleExternalLink(context, Uri.parse(href));
            }
        }
    }
}
