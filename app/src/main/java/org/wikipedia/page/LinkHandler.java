package org.wikipedia.page;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonObject;

import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.util.UriUtil;
import org.wikipedia.util.log.L;

import java.util.Arrays;
import java.util.List;

import static org.wikipedia.util.UriUtil.handleExternalLink;

/**
 * Handles any html links coming from a {@link org.wikipedia.page.PageFragment}
 */
public abstract class LinkHandler implements CommunicationBridge.JSEventListener, LinkMovementMethodExt.UrlHandlerWithText {
    private static final List<String> KNOWN_SCHEMES
            = Arrays.asList("http", "https", "geo", "file", "content");

    @NonNull private final Context context;

    public LinkHandler(@NonNull Context context) {
        this.context = context;
    }

    public abstract void onPageLinkClicked(@NonNull String anchor, @NonNull String linkText);

    public abstract void onInternalLinkClicked(@NonNull PageTitle title);

    public abstract void onMediaLinkClicked(@NonNull PageTitle title);

    public abstract WikiSite getWikiSite();

    // message from JS bridge:
    @Override
    public void onMessage(String messageType, JsonObject messagePayload) {
        String href = UriUtil.decodeURL(messagePayload.get("href").getAsString());
        onUrlClick(href, messagePayload.has("title") ? messagePayload.get("title").getAsString() : null,
                messagePayload.has("text") ? messagePayload.get("text").getAsString() : "");
    }

    @Override
    public void onUrlClick(@NonNull String href, @Nullable String titleString, @NonNull String linkText) {
        if (href.startsWith("//")) {
            // for URLs without an explicit scheme, add our default scheme explicitly.
            href = getWikiSite().scheme() + ":" + href;
        } else if (href.startsWith("./")) {
            href = href.replace("./", "/wiki/");
        }

        // special: returned by page-library when clicking Read More items in the footer.
        int eventLoggingParamIndex = href.indexOf("?event-logging-label");
        if (eventLoggingParamIndex > 0) {
            href = href.substring(0, eventLoggingParamIndex);
        }

        Uri uri = Uri.parse(href);

        if (!TextUtils.isEmpty(uri.getFragment()) && uri.getFragment().contains("cite")) {
            onPageLinkClicked(uri.getFragment(), linkText);
            return;
        }

        boolean knownScheme = false;
        for (String scheme : KNOWN_SCHEMES) {
            if (href.startsWith(scheme + ":")) {
                knownScheme = true;
                break;
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

        // TODO: remove this after the endpoint supporting language variants
        String convertedText = UriUtil.getTitleFromUrl(href);
        if (!convertedText.equals(titleString)) {
            titleString = convertedText;
        }

        L.d("Link clicked was " + uri.toString());
        if (!TextUtils.isEmpty(uri.getPath()) && WikiSite.supportedAuthority(uri.getAuthority())
                && (uri.getPath().matches("^" + UriUtil.WIKI_REGEX + ".*"))) {
            WikiSite site = new WikiSite(uri);
            if (site.subdomain().equals(getWikiSite().subdomain())
                    && !site.languageCode().equals(getWikiSite().languageCode())) {
                // override the languageCode from the parent WikiSite, in case it's a variant.
                site = new WikiSite(uri.getAuthority(), getWikiSite().languageCode());
            }
            PageTitle title = TextUtils.isEmpty(titleString)
                    ? site.titleForInternalLink(uri.getPath())
                    : PageTitle.withSeparateFragment(titleString, uri.getFragment(), site);
            if (title.isFilePage()) {
                onMediaLinkClicked(title);
            } else {
                onInternalLinkClicked(title);
            }
        } else if (!TextUtils.isEmpty(uri.getAuthority()) && WikiSite.supportedAuthority(uri.getAuthority())
                && !TextUtils.isEmpty(uri.getFragment())) {
            onPageLinkClicked(uri.getFragment(), linkText);
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
