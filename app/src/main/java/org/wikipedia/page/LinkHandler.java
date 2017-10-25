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

import java.util.Arrays;
import java.util.List;

import static org.wikipedia.util.UriUtil.decodeURL;
import static org.wikipedia.util.UriUtil.handleExternalLink;

/**
 * Handles any html links coming from a {@link org.wikipedia.page.PageFragment}
 */
public abstract class LinkHandler implements CommunicationBridge.JSEventListener, LinkMovementMethodExt.UrlHandler {
    private static final List<String> KNOWN_SCHEMES
            = Arrays.asList("http", "https", "geo", "file", "content");

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
        if (href.startsWith("//")) {
            // for URLs without an explicit scheme, add our default scheme explicitly.
            href = getWikiSite().scheme() + ":" + href;
        }

        Uri uri = Uri.parse(href);

        boolean knownScheme = false;
        for (String scheme : KNOWN_SCHEMES) {
            if (href.startsWith(scheme + ":")) {
                knownScheme = true;
            }
        }
        if (!knownScheme) {
            // for URLs without a known scheme, add our default scheme explicitly.
            uri = uri.buildUpon()
                    .scheme(getWikiSite().scheme())
                    .authority(getWikiSite().authority())
                    .path(href)
                    .build();
        }

        Log.d("Wikipedia", "Link clicked was " + uri.toString());
        if (!TextUtils.isEmpty(uri.getPath()) && WikiSite.supportedAuthority(uri.getAuthority())
                && (uri.getPath().startsWith("/wiki/") || uri.getPath().startsWith("/zh-"))) {
            WikiSite site = new WikiSite(uri);
            if (site.subdomain().equals(getWikiSite().subdomain())
                    && !site.languageCode().equals(getWikiSite().languageCode())) {
                // override the languageCode from the parent WikiSite, in case it's a variant.
                site = new WikiSite(uri.getAuthority(), getWikiSite().languageCode());
            }
            PageTitle title = TextUtils.isEmpty(titleString)
                    ? site.titleForInternalLink(uri.getPath())
                    : new PageTitle(titleString, site);
            onInternalLinkClicked(title);
        } else if (!TextUtils.isEmpty(titleString) && UriUtil.isValidOfflinePageLink(uri)) {
            WikiSite site = new WikiSite(uri);
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
