package org.wikipedia.page.bottomcontent;

import android.graphics.Point;
import android.os.Build;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageViewFragment;
import org.wikipedia.views.ObservableWebView;

public class BottomContentHandler implements ObservableWebView.OnScrollChangeListener {
    private final PageViewFragment parentFragment;
    private final CommunicationBridge bridge;
    private final WebView webView;
    private final LinkHandler linkHandler;

    private int displayHeight;
    private float displayDensity;

    private View bottomContentContainer;
    private TextView pageLastUpdatedText;
    private TextView pageLicenseText;

    public BottomContentHandler(final PageViewFragment parentFragment, CommunicationBridge bridge,
                        ObservableWebView webview, LinkHandler linkHandler, ViewGroup hidingView) {
        this.parentFragment = parentFragment;
        this.bridge = bridge;
        this.webView = webview;
        this.linkHandler = linkHandler;
        displayDensity = parentFragment.getResources().getDisplayMetrics().density;

        bottomContentContainer = hidingView;
        webview.addOnScrollChangeListener(this);

        pageLastUpdatedText = (TextView)bottomContentContainer.findViewById(R.id.page_last_updated_text);
        pageLicenseText = (TextView)bottomContentContainer.findViewById(R.id.page_license_text);

        // preload the display density, since it will be used in a lot of places
        displayDensity = parentFragment.getResources().getDisplayMetrics().density;

        // get the screen height, using correct methods for different API versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            parentFragment.getActivity().getWindowManager().getDefaultDisplay().getSize(size);
            displayHeight = (int)(size.y / displayDensity);
        } else {
            displayHeight = (int)(parentFragment.getActivity()
                    .getWindowManager().getDefaultDisplay().getHeight() / displayDensity);
        }

        layoutContent();
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        int contentHeight = (int)(webView.getContentHeight() * displayDensity);
        int screenHeight = (int)(displayHeight * displayDensity);
        final int bottomOffsetExtra = 25;
        int bottomOffset = contentHeight - scrollY - screenHeight
                + (int)(bottomOffsetExtra * displayDensity);
        int bottomHeight = bottomContentContainer.getHeight();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bottomContentContainer.getLayoutParams();
        if (bottomOffset > bottomHeight) {
            if (params.bottomMargin != -bottomHeight) {
                params.bottomMargin = -bottomHeight;
                bottomContentContainer.setLayoutParams(params);
            }
        } else {
            params.bottomMargin = -bottomOffset;
            bottomContentContainer.setLayoutParams(params);
        }
    }

    private void layoutContent() {
        // pad the bottom of the webview, to make room for ourselves
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingBottom", (int)(bottomContentContainer.getHeight() / displayDensity));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingBottom", payload);

        Page page = parentFragment.getFragment().getPage();
        String lastUpdatedHtml = "<a href=\"" + page.getTitle().getUriForAction("history")
                + "\">" + parentFragment.getString(R.string.last_updated_text,
                Utils.formatDateRelative(page.getPageProperties().getLastModified())
                + "</a>");
        pageLastUpdatedText.setText(Html.fromHtml(lastUpdatedHtml));
        pageLastUpdatedText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
        pageLicenseText.setText(Html.fromHtml(parentFragment.getString(R.string.content_license_html)));
        pageLicenseText.setMovementMethod(new LinkMovementMethodExt(linkHandler));

        // and make it visible!
        bottomContentContainer.setVisibility(View.VISIBLE);
        // trigger a manual scroll event to update our position
        onScrollChanged(webView.getScrollY(), webView.getScrollY());
    }
}
