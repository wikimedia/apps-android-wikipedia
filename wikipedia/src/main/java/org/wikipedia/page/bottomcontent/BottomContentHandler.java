package org.wikipedia.page.bottomcontent;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import com.squareup.picasso.Target;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.SuggestedPagesFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkHandler;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageViewFragmentInternal;
import org.wikipedia.page.SuggestionsTask;
import org.wikipedia.page.leadimages.ImageViewWithFace;
import org.wikipedia.search.SearchResults;
import org.wikipedia.views.ObservableWebView;

public class BottomContentHandler implements BottomContentInterface,
                                             ObservableWebView.OnScrollChangeListener,
                                             ObservableWebView.OnContentHeightChangedListener,
                                             ImageViewWithFace.OnImageLoadListener {
    private static final String TAG = "BottomContentHandler";
    private final PageViewFragmentInternal parentFragment;
    private final CommunicationBridge bridge;
    private final WebView webView;
    private final LinkHandler linkHandler;
    private PageTitle pageTitle;
    private final PageActivity activity;
    private final WikipediaApp app;
    private float displayDensity;
    private boolean firstTimeShown = false;

    private View bottomContentContainer;
    private TextView pageLastUpdatedText;
    private TextView pageLicenseText;
    private View readMoreContainer;

    private TextView readNextTitle;
    private TextView readNextDescription;
    private ImageView imagePlaceholder;
    private ImageViewWithFace image1;

    private SuggestedPagesFunnel funnel;
    private SearchResults readMoreItems;

    public BottomContentHandler(PageViewFragmentInternal parent, CommunicationBridge bridge,
                                ObservableWebView webview, LinkHandler linkHandler,
                                ViewGroup hidingView, final PageTitle pageTitle) {
        this.parentFragment = parent;
        this.bridge = bridge;
        this.webView = webview;
        this.linkHandler = linkHandler;
        this.pageTitle = pageTitle;
        activity = parentFragment.getActivity();
        app = (WikipediaApp) activity.getApplicationContext();
        displayDensity = activity.getResources().getDisplayMetrics().density;

        bottomContentContainer = hidingView;
        webview.addOnScrollChangeListener(this);
        webview.addOnContentHeightChangedListener(this);

        pageLastUpdatedText = (TextView) bottomContentContainer.findViewById(R.id.page_last_updated_text);
        pageLicenseText = (TextView) bottomContentContainer.findViewById(R.id.page_license_text);
        readMoreContainer = bottomContentContainer.findViewById(R.id.read_next_container);
        readNextTitle = (TextView) bottomContentContainer.findViewById(R.id.read_next_title_text);
        readNextDescription = (TextView) bottomContentContainer.findViewById(R.id.read_next_description_text);
        imagePlaceholder = (ImageView) bottomContentContainer.findViewById(R.id.read_next_image_placeholder);
        image1 = (ImageViewWithFace) bottomContentContainer.findViewById(R.id.read_next_image_1);
        image1.setOnImageLoadListener(this);

        funnel = new SuggestedPagesFunnel(app, pageTitle.getSite(), 1);

        webview.addOnClickListener(new ObservableWebView.OnClickListener() {
            @Override
            public void onClick(float x, float y) {
                // if the click event is within the area of the lead image, then the user
                // must have wanted to click on the lead image!
                int[] pos = new int[2];
                imagePlaceholder.getLocationOnScreen(pos);
                if (y > pos[1] && y < (pos[1] + imagePlaceholder.getHeight())) {
                    PageTitle title = (PageTitle) image1.getTag();
                    HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
                    activity.displayNewPage(title, historyEntry);
                    funnel.logSuggestionClicked(pageTitle, readMoreItems.getPageTitles(), 0);
                }
            }
        });

        // preload the display density, since it will be used in a lot of places
        displayDensity = activity.getResources().getDisplayMetrics().density;
        // hide ourselves by default
        hide();
    }

    public static boolean useNewBottomContent(WikipediaApp app) {
        if (app.getReleaseType() == WikipediaApp.RELEASE_PROD) {
            return false;
        }
        // decide what kind of bottom container this page will have, based on the app install ID.
        final int hexBase = 16;
        return Integer.parseInt(app.getAppInstallID().substring(app.getAppInstallID().length() - 1), hexBase) % 2 == 0;
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        if (bottomContentContainer.getVisibility() == View.GONE) {
            return;
        }
        int contentHeight = (int)(webView.getContentHeight() * displayDensity);
        int bottomOffset = contentHeight - scrollY - webView.getHeight();
        int bottomHeight = bottomContentContainer.getHeight();
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) bottomContentContainer.getLayoutParams();
        if (bottomOffset > bottomHeight) {
            if (params.bottomMargin != -bottomHeight) {
                params.bottomMargin = -bottomHeight;
                params.topMargin = 0;
                bottomContentContainer.setLayoutParams(params);
                bottomContentContainer.setVisibility(View.INVISIBLE);
            }
        } else {
            params.bottomMargin = -bottomOffset;
            params.topMargin = -bottomHeight;
            bottomContentContainer.setLayoutParams(params);
            if (bottomContentContainer.getVisibility() != View.VISIBLE) {
                bottomContentContainer.setVisibility(View.VISIBLE);
            }
            if (!firstTimeShown && readMoreItems != null) {
                firstTimeShown = true;
                funnel.logSuggestionsShown(pageTitle, readMoreItems.getPageTitles());
                // and start fetching the lead image, if we have one
                if (image1.getTag() != null && ((PageTitle)image1.getTag()).getThumbUrl() != null
                        && app.showImages()) {
                    Picasso.with(parentFragment.getActivity())
                           .load(((PageTitle)image1.getTag()).getThumbUrl())
                           .noFade()
                           .into((Target)image1);
                }
            }
        }
    }

    @Override
    public void onContentHeightChanged(int contentHeight) {
        if (bottomContentContainer.getVisibility() != View.VISIBLE) {
            return;
        }
        // trigger a manual scroll event to update our position
        onScrollChanged(webView.getScrollY(), webView.getScrollY());
    }

    /**
     * Hide the bottom content entirely.
     * It can only be shown again by calling beginLayout()
     */
    public void hide() {
        bottomContentContainer.setVisibility(View.GONE);
    }

    public void beginLayout() {
        setupAttribution();
        if (parentFragment.getPage().couldHaveReadMoreSection()) {
            preRequestReadMoreItems();
        } else {
            bottomContentContainer.findViewById(R.id.read_more_container).setVisibility(View.GONE);
            layoutContent();
        }
    }

    private void layoutContent() {
        if (!parentFragment.isAdded()) {
            return;
        }
        bottomContentContainer.setVisibility(View.INVISIBLE);
        // keep trying until our layout has a height...
        if (bottomContentContainer.getHeight() == 0) {
            final int postDelay = 50;
            bottomContentContainer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layoutContent();
                }
            }, postDelay);
            return;
        }

        bottomContentContainer.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        // pad the bottom of the webview, to make room for ourselves
        int totalHeight = bottomContentContainer.getMeasuredHeight();
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingBottom", (int)(totalHeight / displayDensity));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingBottom", payload);
        // ^ sending the padding event will guarantee a ContentHeightChanged event to be triggered,
        // which will update our margin based on the scroll offset, so we don't need to do it here.
    }

    private void setupAttribution() {
        Page page = parentFragment.getPage();
        pageLicenseText.setText(Html.fromHtml(activity.getString(R.string.content_license_html)));
        pageLicenseText.setMovementMethod(new LinkMovementMethodExt(linkHandler));

        // Don't display last updated message for main page or file pages, because it's always wrong
        if (page.isMainPage() || page.isFilePage()) {
            pageLastUpdatedText.setVisibility(View.GONE);
        } else {
            String lastUpdatedHtml = "<a href=\"" + page.getTitle().getUriForAction("history")
                    + "\">" + activity.getString(R.string.last_updated_text,
                    Utils.formatDateRelative(page.getPageProperties().getLastModified())
                            + "</a>");
            pageLastUpdatedText.setText(Html.fromHtml(lastUpdatedHtml));
            pageLastUpdatedText.setMovementMethod(new LinkMovementMethodExt(linkHandler));
        }
    }

    private void preRequestReadMoreItems() {
        if (parentFragment.getPage().isMainPage()) {
            new MainPageReadMoreTopicTask(activity) {
                @Override
                public void onFinish(PageTitle myTitle) {
                    requestReadMoreItems(myTitle);
                }

                @Override
                public void onCatch(Throwable caught) {
                    // Read More titles are expendable.
                    Log.w(TAG, "Error while getting Read More topic for main page.", caught);
                    // but lay out the bottom content anyway:
                    layoutContent();
                }
            }.execute();
        } else {
            requestReadMoreItems(pageTitle);
        }
    }

    private void requestReadMoreItems(final PageTitle myTitle) {
        if (myTitle == null || TextUtils.isEmpty(myTitle.getPrefixedText())) {
            hideReadMore();
            layoutContent();
            return;
        }

        final int numSuggestions = 10;
        new SuggestionsTask(app.getAPIForSite(myTitle.getSite()), myTitle.getSite(),
                myTitle.getPrefixedText(), (int)(parentFragment.getActivity().getResources().getDimension(R.dimen.leadImageWidth) / displayDensity),
                numSuggestions, true) {
            @Override
            public void onFinish(SearchResults results) {
                readMoreItems = results;
                if (!readMoreItems.getPageTitles().isEmpty()) {
                    // If there are results, set up section and make sure it's visible
                    setupReadNextSection(readMoreItems);
                    showReadMore();
                } else {
                    // If there's no results, just hide the section
                    hideReadMore();
                }
                layoutContent();
            }

            @Override
            public void onCatch(Throwable caught) {
                // Read More titles are expendable.
                Log.w(TAG, "Error while fetching Read More titles.", caught);
                // but lay out the bottom content anyway:
                layoutContent();
            }
        }.execute();
    }

    private void hideReadMore() {
        readMoreContainer.setVisibility(View.GONE);
    }

    private void showReadMore() {
        readMoreContainer.setVisibility(View.VISIBLE);
    }

    public PageTitle getTitle() {
        return pageTitle;
    }

    public void setTitle(PageTitle newTitle) {
        pageTitle = newTitle;
    }

    private void setupReadNextSection(final SearchResults results) {
        PageTitle title = results.getPageTitles().get(0);
        readNextTitle.setText(title.getDisplayText());
        readNextDescription.setVisibility(title.getDescription() == null ? View.GONE : View.VISIBLE);
        final int bottomPaddingBase = 12;
        int titleBottomPadding = bottomPaddingBase;
        if (title.getDescription() != null) {
            final int descriptionPadding = 18;
            titleBottomPadding += descriptionPadding;
            readNextDescription.setText(title.getDescription());
        }
        image1.setTag(title);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // for API >10, decrease line spacing and boost bottom padding to account for it.
            // (in API 10, decreased line spacing cuts off the bottom of the text)
            final float lineSpacing = 0.8f;
            final int lineSpacePadding = 12;
            readNextTitle.setLineSpacing(0, lineSpacing);
            // however, if it's Lollipop or greater, then don't boost the bottom padding of the
            // title text, since it now correctly does it automatically.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                titleBottomPadding += lineSpacePadding;
            }
        }
        readNextTitle.setPadding(readNextTitle.getPaddingLeft(), readNextTitle.getPaddingTop(),
                readNextTitle.getPaddingRight(), (int)(titleBottomPadding * displayDensity));
    }

    @Override
    public void onImageLoaded(Bitmap bitmap, final PointF faceLocation) {
        final int bmpHeight = bitmap.getHeight();
        final float aspect = (float)bitmap.getHeight() / (float)bitmap.getWidth();
        readMoreContainer.post(new Runnable() {
            @Override
            public void run() {
                if (!parentFragment.isAdded()) {
                    return;
                }
                int newWidth = image1.getWidth();
                int newHeight = (int)(newWidth * aspect);

                // give our image an offset based on the location of the face,
                // relative to the image container
                int imageBaseYOffset = 0;
                float scale = (float)newHeight / (float)bmpHeight;
                if (faceLocation.y > 0.0f) {
                    int faceY = (int)(faceLocation.y * scale);
                    // if we have a face, then offset to the face location
                    imageBaseYOffset = -(faceY - (imagePlaceholder.getHeight() / 2));
                    // Adjust the face position by a slight amount.
                    // The face recognizer gives the location of the *eyes*, whereas we actually
                    // want to center on the *nose*...
                    final int faceBoost = 24;
                    imageBaseYOffset -= (faceBoost * displayDensity);
                } else {
                    // No face, so we'll just chop the top 25% off rather than centering
                    final float oneQuarter = 0.25f;
                    imageBaseYOffset = -(int)((newHeight - imagePlaceholder.getHeight()) * oneQuarter);
                }
                // is the offset too far to the top?
                if (imageBaseYOffset > 0) {
                    imageBaseYOffset = 0;
                }
                // is the offset too far to the bottom?
                if (imageBaseYOffset < imagePlaceholder.getHeight() - newHeight) {
                    imageBaseYOffset = imagePlaceholder.getHeight() - newHeight;
                }

                // resize our image to have the same proportions as the acquired bitmap,
                // and offset it based on the face position
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) image1.getLayoutParams();
                params.width = LinearLayout.LayoutParams.MATCH_PARENT;
                if (newHeight < imagePlaceholder.getHeight()) {
                    // if the height of the image is less than the container, then just
                    // make it the same height as the placeholder.
                    params.height = imagePlaceholder.getHeight();
                    imageBaseYOffset = 0;
                } else {
                    params.height = newHeight;
                }
                params.topMargin = imageBaseYOffset;
                image1.setLayoutParams(params);

                // fade in the new image!
                ViewAnimations.crossFade(imagePlaceholder, image1);
            }
        });
    }

    @Override
    public void onImageFailed() {
        // just keep showing the placeholder image...
    }

}
