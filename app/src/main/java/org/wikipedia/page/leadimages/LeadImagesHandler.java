package org.wikipedia.page.leadimages;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.graphics.PointF;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageTitle;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.views.ArticleHeaderView;
import org.wikipedia.views.ObservableWebView;

import static org.wikipedia.util.DimenUtil.getContentTopOffsetPx;

public class LeadImagesHandler {
    /**
     * Minimum screen height for enabling lead images. If the screen is smaller than
     * this height, lead images will not be displayed, and will be substituted with just
     * the page title.
     */
    private static final int MIN_SCREEN_HEIGHT_DP = 480;

    /**
     * Ratio of how much of the screen will be filled up by the lead image view, versus the
     * total screen height.
     */
    private static final float IMAGES_CONTAINER_RATIO = 0.5f;

    /**
     * Maximum height of the page title text. If the text overflows this size, then the
     * font size of the title will be automatically reduced to fit.
     */
    private static final int TITLE_MAX_HEIGHT_DP = 256;

    /**
     * Minimum font size of the page title. Will not be reduced any further than this.
     */
    private static final int TITLE_MIN_TEXT_SIZE_SP = 12;

    /**
     * Amount by which the page title font will be reduced, for each step of the
     * reduction process.
     */
    private static final int TITLE_TEXT_SIZE_DECREMENT_SP = 4;

    /**
     * Number of pixels to offset the WebView content (in addition to page title height),
     * when lead images are disabled.
     */
    private static final int DISABLED_OFFSET_DP = 88;

    public interface OnLeadImageLayoutListener {
        void onLayoutComplete(int sequence);
    }

    @NonNull private final PageFragment parentFragment;
    @NonNull private final CommunicationBridge bridge;
    @NonNull private final ObservableWebView webView;

    /**
     * Whether lead images are enabled, overall.  They will be disabled automatically
     * if the screen height is less than a defined constant (above), or if the current article
     * doesn't have a lead image associated with it.
     */
    private boolean leadImagesEnabled;

    @NonNull private final ArticleHeaderView articleHeaderView;
    private ImageView imagePlaceholder;
    private ImageViewWithFace image;

    private int displayHeightDp;
    private float faceYOffsetNormalized;
    private float displayDensity;

    public LeadImagesHandler(@NonNull final PageFragment parentFragment,
                             @NonNull CommunicationBridge bridge,
                             @NonNull ObservableWebView webView,
                             @NonNull ArticleHeaderView articleHeaderView) {
        this.articleHeaderView = articleHeaderView;
        this.parentFragment = parentFragment;
        this.bridge = bridge;
        this.webView = webView;

        imagePlaceholder = articleHeaderView.getPlaceholder();
        image = articleHeaderView.getImage();

        initDisplayDimensions();

        initWebView();

        // hide ourselves by default
        hide();

        articleHeaderView.setOnImageLoadListener(new ImageLoadListener());
    }

    /**
     * Completely hide the lead image view. Useful in case of network errors, etc.
     * The only way to "show" the lead image view is by calling the beginLayout function.
     */
    public void hide() {
        articleHeaderView.hide();
    }

    @Nullable public Bitmap getLeadImageBitmap() {
        return leadImagesEnabled ? articleHeaderView.copyImage() : null;
    }

    public boolean isLeadImageEnabled() {
        return leadImagesEnabled;
    }

    /**
     * Returns the normalized (0.0 to 1.0) vertical focus position of the lead image.
     * A value of 0.0 represents the top of the image, and 1.0 represents the bottom.
     * The "focus position" is currently defined by automatic face detection, but may be
     * defined by other factors in the future.
     *
     * @return Normalized vertical focus position.
     */
    public float getLeadImageFocusY() {
        return faceYOffsetNormalized;
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
    public void beginLayout(OnLeadImageLayoutListener listener, int sequence) {
        String thumbUrl = getLeadImageUrl();
        initDisplayDimensions();

        if (!WikipediaApp.getInstance().isImageDownloadEnabled() || displayHeightDp < MIN_SCREEN_HEIGHT_DP) {
            // disable the lead image completely
            leadImagesEnabled = false;
        } else {
            // Enable only if the image is not a GIF, since GIF images are usually mathematical
            // diagrams or animations that won't look good as a lead image.
            // TODO: retrieve the MIME type of the lead image, instead of relying on file name.
            leadImagesEnabled = thumbUrl != null && !thumbUrl.endsWith(".gif");
        }

        // set the page title text, and honor any HTML formatting in the title
        articleHeaderView.setTitle(Html.fromHtml(getPage().getDisplayTitle()));
        articleHeaderView.setLocale(getPage().getTitle().getSite().getLanguageCode());
        articleHeaderView.setPronunciation(getPage().getTitlePronunciationUrl());
        // Set the subtitle, too, so text measurements are accurate.
        layoutWikiDataDescription(getTitle().getDescription());

        // kick off the (asynchronous) laying out of the page title text
        layoutPageTitle((int) (getDimension(R.dimen.titleTextSize)
                / displayDensity), listener, sequence);
    }

    /**
     * Intermediate step in the layout process:
     * Recursive function that will dynamically size down the page title TextView if the page title
     * is too long. Since it's assumed that the overall lead image view is hidden at this stage,
     * this process will be invisible to the user, and will not appear jarring. Once the optimal
     * font size is reached, the next step in the layout process is triggered.
     *
     * @param fontSizeSp Font size to be tested.
     * @param listener   Listener that will receive an event when the layout is completed.
     */
    private void layoutPageTitle(final int fontSizeSp, final OnLeadImageLayoutListener listener, final int sequence) {
        if (!isFragmentAdded()) {
            return;
        }
        // set the font size of the title
        articleHeaderView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        // if we're still not being shown (if the fragment is still being created),
        // then retry after a delay.
        if (articleHeaderView.getTextHeight() == 0) {
            final int postDelay = 50;
            articleHeaderView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layoutPageTitle(fontSizeSp, listener, sequence);
                }
            }, postDelay);
        } else {
            // give it a chance to redraw, and then see if it fits
            articleHeaderView.post(new Runnable() {
                @Override
                public void run() {
                    if (!isFragmentAdded()) {
                        return;
                    }
                    if (((int) (articleHeaderView.getTextHeight() / displayDensity) > TITLE_MAX_HEIGHT_DP)
                            && (fontSizeSp > TITLE_MIN_TEXT_SIZE_SP)) {
                        int newSize = fontSizeSp - TITLE_TEXT_SIZE_DECREMENT_SP;
                        if (newSize < TITLE_MIN_TEXT_SIZE_SP) {
                            newSize = TITLE_MIN_TEXT_SIZE_SP;
                        }
                        layoutPageTitle(newSize, listener, sequence);
                    } else {
                        // we're done!
                        layoutViews(listener, sequence);
                    }
                }
            });
        }
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

        int titleContainerHeight;

        if (isMainPage()) {
            titleContainerHeight = (int) (getContentTopOffsetPx(getActivity()) / displayDensity);
            articleHeaderView.hide();
        } else if (!leadImagesEnabled) {
            articleHeaderView.showText();

            // TODO: remove. Somebody is resetting the visibility of the ImageView.
            image.setImageDrawable(null);

            // ok, we're not going to show lead images, so we need to make some
            // adjustments to our layout:
            // make the WebView padding be just the height of the title text, plus a fixed offset
            titleContainerHeight = (int) ((articleHeaderView.getTextHeight() / displayDensity))
                    + DISABLED_OFFSET_DP;
            articleHeaderView.setLayoutParams(new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    (int) ((titleContainerHeight) * displayDensity)));
        } else {
            articleHeaderView.showTextImage();

            // we're going to show the lead image, so make some adjustments to the
            // layout, in case we were previously not showing it:
            // make the WebView padding be a proportion of the total screen height
            titleContainerHeight = (int) (displayHeightDp * IMAGES_CONTAINER_RATIO);
            articleHeaderView.setLayoutParams(new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    (int) (titleContainerHeight * displayDensity)));

        }

        final int paddingExtra = 8;
        setWebViewPaddingTop(titleContainerHeight + paddingExtra);

        // and start fetching the lead image, if we have one
        loadLeadImage();

        // tell our listener that it's ok to start loading the rest of the WebView content
        listener.onLayoutComplete(sequence);
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
     * Determines and sets displayDensity and displayHeightDp for the lead images layout.
     */
    private void initDisplayDimensions() {
        // preload the display density, since it will be used in a lot of places
        displayDensity = DimenUtil.getDensityScalar();

        int displayHeightPx = DimenUtil.getDisplayHeightPx();

        displayHeightDp = (int) (displayHeightPx / displayDensity);
    }

    private void setImageLayoutParams(int width, int height) {
        image.setLayoutParams(new FrameLayout.LayoutParams(width, height));
    }

    private void loadLeadImage() {
        loadLeadImage(getLeadImageUrl());
    }

    /**
     * @param url Nullable URL with no scheme. For example, foo.bar.com/ instead of
     *            http://foo.bar.com/.
     */
    private void loadLeadImage(@Nullable String url) {
        if (!isMainPage() && !TextUtils.isEmpty(url) && leadImagesEnabled) {
            String fullUrl = WikipediaApp.getInstance().getNetworkProtocol() + ":" + url;
            articleHeaderView.setImageYOffset(0);
            articleHeaderView.loadImage(fullUrl);
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

    private void initWebView() {
        webView.addOnScrollChangeListener(articleHeaderView);

        webView.addOnClickListener(new ObservableWebView.OnClickListener() {
            @Override
            public boolean onClick(float x, float y) {
                // if the click event is within the area of the lead image, then the user
                // must have wanted to click on the lead image!
                if (leadImagesEnabled && y < (articleHeaderView.getHeight() - webView.getScrollY())) {
                    String imageName = getPage().getPageProperties().getLeadImageName();
                    if (imageName != null) {
                        PageTitle imageTitle = new PageTitle("File:" + imageName,
                                getTitle().getSite());
                        GalleryActivity.showGallery(getActivity(),
                                parentFragment.getTitleOriginal(), imageTitle,
                                GalleryFunnel.SOURCE_LEAD_IMAGE);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private boolean isMainPage() {
        return getPage().isMainPage();
    }

    private PageTitle getTitle() {
        return parentFragment.getTitle();
    }

    private Page getPage() {
        return parentFragment.getPage();
    }

    private boolean isFragmentAdded() {
        return parentFragment.isAdded();
    }

    private float getDimension(@DimenRes int id) {
        return getResources().getDimension(id);
    }

    private Resources getResources() {
        return getActivity().getResources();
    }

    private FragmentActivity getActivity() {
        return parentFragment.getActivity();
    }

    private class ImageLoadListener implements ImageViewWithFace.OnImageLoadListener {
        @Override
        public void onImageLoaded(Bitmap bitmap, @Nullable final PointF faceLocation) {
            final int bmpHeight = bitmap.getHeight();
            final float aspect = (float) bitmap.getHeight() / (float) bitmap.getWidth();
            articleHeaderView.post(new Runnable() {
                @Override
                public void run() {
                    if (isFragmentAdded()) {
                        detectFace(bmpHeight, aspect, faceLocation);
                    }
                }
            });
        }

        @Override
        public void onImageFailed() {
            // just keep showing the placeholder image...
        }

        private void detectFace(int bmpHeight, float aspect, @Nullable PointF faceLocation) {
            int newWidth = image.getWidth();
            int newHeight = (int) (newWidth * aspect);

            // give our image an offset based on the location of the face,
            // relative to the image container
            float scale = (float) newHeight / (float) bmpHeight;
            int imageBaseYOffset;
            if (faceLocation != null) {
                int faceY = (int) (faceLocation.y * scale);
                // if we have a face, then offset to the face location
                imageBaseYOffset = -(faceY - (imagePlaceholder.getHeight() / 2));
                // Adjust the face position by a slight amount.
                // The face recognizer gives the location of the *eyes*, whereas we actually
                // want to center on the *nose*...
                imageBaseYOffset += getDimension(R.dimen.face_detection_nose_y_offset);
                faceYOffsetNormalized = faceLocation.y / bmpHeight;
            } else {
                // No face, so we'll just chop the top 25% off rather than centering
                final float oneQuarter = 0.25f;
                imageBaseYOffset = -(int) ((newHeight - imagePlaceholder.getHeight()) * oneQuarter);
                faceYOffsetNormalized = oneQuarter;
            }

            // is the offset too far to the top?
            imageBaseYOffset = Math.min(0, imageBaseYOffset);

            // is the offset too far to the bottom?
            imageBaseYOffset = Math.max(imageBaseYOffset, imagePlaceholder.getHeight() - newHeight);

            // resize our image to have the same proportions as the acquired bitmap
            if (newHeight < imagePlaceholder.getHeight()) {
                // if the height of the image is less than the container, then just
                // make it the same height as the placeholder.
                setImageLayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, imagePlaceholder.getHeight());
                imageBaseYOffset = 0;
            } else {
                setImageLayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, newHeight);
            }

            articleHeaderView.setImageYOffset(imageBaseYOffset);

            // fade in the new image!
            ViewAnimations.crossFade(imagePlaceholder, image);

            startKenBurnsAnimation();
        }
    }
}
