package org.wikipedia.page;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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
    @NonNull private final Context context;

    public LinkHandler(@NonNull Context context) {
        this.context = context;
    }

    public abstract void onPageLinkClicked(@NonNull String anchor);

    public abstract void onInternalLinkClicked(@NonNull PageTitle title);

    public abstract WikiSite getWikiSite();

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JSONObject messagePayload) {
        try {
            String href = decodeURL(messagePayload.getString("href"));
            onUrlClick(href, messagePayload.optString("title"));
        } catch (IllegalArgumentException e) {
            // The URL is malformed and URL decoder can't understand it. Just do nothing.
            Log.d("Wikipedia", "A malformed URL was tapped.");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUrlClick(@NonNull String href, @Nullable String titleString) {
        // other domain links
        if (href.startsWith("//")) {
            href = getWikiSite().scheme() + ":" + href;
        }

        Uri uri = Uri.parse(href);
        if (!href.startsWith("http:") && !href.startsWith("https:")) {
            uri = uri.buildUpon()
                     .scheme(getWikiSite().scheme())
                     .authority(getWikiSite().authority())
                     .path(href)
                     .build();
        }

        Log.d("Wikipedia", "Link clicked was " + uri.toString());
        if (!TextUtils.isEmpty(uri.getPath()) && WikiSite.supportedAuthority(uri.getAuthority())
                && uri.getPath().startsWith("/wiki/")) {
            WikiSite site = new WikiSite(uri.getAuthority());
            PageTitle title = site.titleForInternalLink(uri.getPath());
            onInternalLinkClicked(title);
        } else if (!TextUtils.isEmpty(titleString) && UriUtil.isValidOfflinePageLink(uri)) {
            WikiSite site = new WikiSite(uri.getAuthority());
            PageTitle title = PageTitle.withSeparateFragment(titleString, uri.getFragment(), site);
            onInternalLinkClicked(title);
        } else if (!TextUtils.isEmpty(uri.getFragment())) {
            onPageLinkClicked(uri.getFragment());
        } else {
            onExternalLinkClicked(uri);
        }
    }

    public void onExternalLinkClicked(@NonNull Uri uri) {
        handleExternalLink(context, uri);
    }

    @NonNull protected Context getContext() {
        return context;
    }
}
