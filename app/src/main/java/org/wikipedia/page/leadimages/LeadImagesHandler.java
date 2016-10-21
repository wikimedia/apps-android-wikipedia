package org.wikipedia.page.leadimages;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ObservableWebView;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class LeadImagesHandler {
    /**
     * Minimum screen height for enabling lead images. If the screen is smaller than
     * this height, lead images will not be displayed, and will be substituted with just
     * the page title.
     */
    private static final int MIN_SCREEN_HEIGHT_DP = 480;

    public interface OnLeadImageLayoutListener {
        void onLayoutComplete(int sequence);
    }

    @NonNull private final PageFragment parentFragment;
    @NonNull private final CommunicationBridge bridge;
    @NonNull private final ObservableWebView webView;

    @NonNull private final ArticleHeaderView articleHeaderView;
    private View image;

    private int displayHeightDp;

    public LeadImagesHandler(@NonNull final PageFragment parentFragment,
                             @NonNull CommunicationBridge bridge,
                             @NonNull ObservableWebView webView,
                             @NonNull ArticleHeaderView articleHeaderView) {
        this.articleHeaderView = articleHeaderView;
        this.parentFragment = parentFragment;
        this.bridge = bridge;
        this.webView = webView;

        image = articleHeaderView.getImage();

        initDisplayDimensions();

        initWebView();

        initArticleHeaderView();

        // hide ourselves by default
        hide();
    }

    /**
     * Completely hide the lead image view. Useful in case of network errors, etc.
     * The only way to "show" the lead image view is by calling the beginLayout function.
     */
    public void hide() {
        articleHeaderView.hide();
    }

    @Nullable public Bitmap getLeadImageBitmap() {
        return isLeadImageEnabled() ? articleHeaderView.copyBitmap() : null;
    }

    public boolean isLeadImageEnabled() {
        return WikipediaApp.getInstance().isImageDownloadEnabled()
                && displayHeightDp >= MIN_SCREEN_HEIGHT_DP
                && !TextUtils.isEmpty(getLeadImageUrl());
    }

    public void setAnimationPaused(boolean paused) {
        articleHeaderView.setAnimationPaused(paused);
    }

    /**
     * Triggers a chain of events that will lay out the lead image, page title, and other
     * elements, at the end of which the WebView contents may begin to be composed.
     * These events (performed asynchronously) are in the following order:
     * - Dynamically resize the page title TextView and, if necessary, adjust its font size
     * based on the length of our page title.
     * - Dynamically resize the lead image container view and restyle it, if necessary, depending
     * on whether the page contains a lead image, and whether our screen resolution is high
     * enough to warrant a lead image.
     * - Send a "padding" event to the WebView so that any subsequent content that's added to it
     * will be correctly offset to account for the lead image view (or lack of it)
     * - Make the lead image view visible.
     * - Fire a callback to the provided Listener indicating that the rest of the WebView content
     * can now be loaded.
     * - Fetch and display the WikiData description for this page, if available.
     * <p/>
     * Realistically, the whole process will happen very quickly, and almost unnoticeably to the
     * user. But it still needs to be asynchronous because we're dynamically laying out views, and
     * because the padding "event" that we send to the WebView must come before any other content
     * is sent to it, so that the effect doesn't look jarring to the user.
     *
     * @param listener Listener that will receive an event when the layout is completed.
     */
    public void beginLayout(OnLeadImageLayoutListener listener,
                            int sequence) {
        if (getPage() == null) {
            return;
        }

        initDisplayDimensions();

        // set the page title text, and honor any HTML formatting in the title
        loadLeadImage();
        articleHeaderView.setTitle(StringUtil.fromHtml(getPage().getDisplayTitle()));
        articleHeaderView.setLocale(getPage().getTitle().getWikiSite().languageCode());
        articleHeaderView.setPronunciation(getPage().getTitlePronunciationUrl());
        // Set the subtitle, too, so text measurements are accurate.
        layoutWikiDataDescription(getTitle().getDescription());
        layoutViews(listener, sequence);
    }

    /**
     * The final step in the layout process:
     * Apply sizing and styling to our page title and lead image views, based on how large our
     * page title ended up, and whether we should display the lead image.
     *
     * @param listener Listener that will receive an event when the layout is completed.
     */
    private void layoutViews(OnLeadImageLayoutListener listener, int sequence) {
        if (!isFragmentAdded()) {
            return;
        }

        if (isMainPage()) {
            articleHeaderView.hide();
        } else {
            if (!isLeadImageEnabled()) {
                articleHeaderView.showText();
            } else {
                articleHeaderView.showTextImage();
            }
        }

        // tell our listener that it's ok to start loading the rest of the WebView content
        listener.onLayoutComplete(sequence);
    }

    private void updatePadding() {
        int padding;
        if (isMainPage()) {
            padding = Math.round(getContentTopOffsetPx(getActivity()) / DimenUtil.getDensityScalar());
        } else {
            padding = Math.round(articleHeaderView.getHeight() / DimenUtil.getDensityScalar());
        }

        setWebViewPaddingTop(padding);
    }

    private void setWebViewPaddingTop(int padding) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingTop", padding);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingTop", payload);
    }

    /**
     * Final step in the WikiData description process: lay out the description, and animate it
     * into place, along with the page title.
     *
     * @param description WikiData description to be shown.
     */
    private void layoutWikiDataDescription(@Nullable final String description) {
        if (TextUtils.isEmpty(description)) {
            articleHeaderView.setSubtitle(null);
        } else {
            int titleLineCount = articleHeaderView.getLineCount();

            articleHeaderView.setSubtitle(description);

            // Only show the description if it's two lines or less.
            if ((articleHeaderView.getLineCount() - titleLineCount) > 2) {
                articleHeaderView.setSubtitle(null);
            }
        }
    }

    /**
     * Determines and sets displayHeightDp for the lead images layout.
     */
    private void initDisplayDimensions() {
        displayHeightDp = (int) (DimenUtil.getDisplayHeightPx() / DimenUtil.getDensityScalar());
    }

    private void loadLeadImage() {
        loadLeadImage(getLeadImageUrl());
    }

    /**
     * @param url Nullable URL with no scheme. For example, foo.bar.com/ instead of
     *            http://foo.bar.com/.
     */
    private void loadLeadImage(@Nullable String url) {
        if (!isMainPage() && !TextUtils.isEmpty(url) && isLeadImageEnabled()) {
            String fullUrl = getTitle().getWikiSite().scheme() + ":" + url;
            articleHeaderView.loadImage(fullUrl);
        } else {
            articleHeaderView.loadImage(null);
        }
    }

    /**
     * @return Nullable URL with no scheme. For example, foo.bar.com/ instead of
     * http://foo.bar.com/.
     */
    @Nullable
    private String getLeadImageUrl() {
        return getPage() == null ? null : getPage().getPageProperties().getLeadImageUrl();
    }

    private void startKenBurnsAnimation() {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.lead_image_zoom);
        image.startAnimation(anim);
    }

    private void initArticleHeaderView() {
        articleHeaderView.setOnImageLoadListener(new ImageLoadListener());
        articleHeaderView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            @SuppressWarnings("checkstyle:parameternumber")
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                updatePadding();
            }
        });
    }

    private void initWebView() {
        webView.addOnScrollChangeListener(articleHeaderView);

        webView.addOnClickListener(new ObservableWebView.OnClickListener() {
            @Override
            public boolean onClick(float x, float y) {
                // if the click event is within the area of the lead image, then the user
                // must have wanted to click on the lead image!
                if (getPage() != null && isLeadImageEnabled() && y < (image.getHeight() - webView.getScrollY())) {
                    String imageName = getPage().getPageProperties().getLeadImageName();
                    if (imageName != null) {
                        String filename = "File:" + imageName;
                        WikiSite wiki = getTitle().getWikiSite();
                        getActivity().startActivityForResult(GalleryActivity.newIntent(getActivity(),
                                parentFragment.getTitleOriginal(), filename, wiki,
                                GalleryFunnel.SOURCE_LEAD_IMAGE),
                                Constants.ACTIVITY_REQUEST_GALLERY);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private boolean isMainPage() {
        return getPage() != null && getPage().isMainPage();
    }

    private PageTitle getTitle() {
        return parentFragment.getTitle();
    }

    @Nullable
    private Page getPage() {
        return parentFragment.getPage();
    }

    private boolean isFragmentAdded() {
        return parentFragment.isAdded();
    }

    private FragmentActivity getActivity() {
        return parentFragment.getActivity();
    }

    private class ImageLoadListener implements FaceAndColorDetectImageView.OnImageLoadListener {
        @Override
        public void onImageLoaded(final int bmpHeight, @Nullable final PointF faceLocation, @ColorInt final int mainColor) {
            articleHeaderView.post(new Runnable() {
                @Override
                public void run() {
                    if (isFragmentAdded()) {
                        if (faceLocation != null) {
                            articleHeaderView.setImageFocus(faceLocation);
                        }
                        startKenBurnsAnimation();
                    }
                }
            });
        }

        @Override
        public void onImageFailed() {
        }
    }
}
