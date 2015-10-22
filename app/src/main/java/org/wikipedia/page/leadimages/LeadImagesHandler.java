package org.wikipedia.page.leadimages;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.util.TypedValue;
import android.graphics.PointF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageTitle;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.richtext.LeadingSpan;
import org.wikipedia.richtext.ParagraphSpan;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ConfigurableTextView;
import org.wikipedia.views.ViewUtil;

import static org.wikipedia.views.ViewUtil.findView;

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
    @NonNull private final ViewGroup imageContainer;
    @NonNull private final CommunicationBridge bridge;
    @NonNull private final ObservableWebView webView;

    /**
     * Whether lead images are enabled, overall.  They will be disabled automatically
     * if the screen height is less than a defined constant (above), or if the current article
     * doesn't have a lead image associated with it.
     */
    private boolean leadImagesEnabled;

    private ImageView imagePlaceholder;
    private ImageViewWithFace image;
    private ConfigurableTextView pageTitleText;
    private Drawable pageTitleGradient;

    private int displayHeightDp;
    private int imageBaseYOffset;
    private float faceYOffsetNormalized;
    private float displayDensity;
    @NonNull private final WebViewScrollListener webViewScrollListener = new WebViewScrollListener();

    public LeadImagesHandler(@NonNull final PageFragment parentFragment,
                             @NonNull CommunicationBridge bridge,
                             @NonNull ObservableWebView webView,
                             @NonNull ViewGroup hidingView) {
        this.parentFragment = parentFragment;
        this.imageContainer = hidingView;
        this.bridge = bridge;
        this.webView = webView;

        imagePlaceholder = findView(imageContainer, R.id.page_image_placeholder);
        image = findView(imageContainer, R.id.page_image);
        pageTitleText = findView(imageContainer, R.id.page_title_text);

        pageTitleGradient = GradientUtil.getCubicGradient(getColor(R.color.lead_gradient_start), Gravity.BOTTOM);
        pageTitleText.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));

        initDisplayDimensions();

        initWebView();

        // hide ourselves by default
        hide();

        imagePlaceholder.setImageResource(Utils.getThemedAttributeId(getActivity(),
                R.attr.lead_image_drawable));

        image.setOnImageLoadListener(new ImageLoadListener());
    }

    /**
     * Completely hide the lead image view. Useful in case of network errors, etc.
     * The only way to "show" the lead image view is by calling the beginLayout function.
     */
    public void hide() {
        imageContainer.setVisibility(View.INVISIBLE);
    }

    @Nullable public Bitmap getLeadImageBitmap() {
        return leadImagesEnabled ? newBitmapFromView(image) : null;
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
            // enable the lead image, only if we actually have a url for it
            if (thumbUrl == null) {
                leadImagesEnabled = false;
            } else {
                // ...and only if the image is not a GIF, since GIF images are usually
                // mathematical diagrams or animations that won't look good as a lead image.
                // TODO: retrieve the MIME type of the lead image, instead of relying on file name.
                leadImagesEnabled = !thumbUrl.endsWith(".gif");

                // TODO: what's the harm in always setting the background to white unconditionally.
                // also, if the image is not a JPG (i.e. it's a PNG or SVG) and might have
                // transparency, give it a white background.
                if (!thumbUrl.endsWith(".jpg")) {
                    image.setBackgroundColor(Color.WHITE);
                }
            }
        }

        // set the page title text, and honor any HTML formatting in the title
        pageTitleText.setText(Html.fromHtml(getPage().getDisplayTitle()), getPage().getTitle().getSite().getLanguageCode());
        // Set the subtitle, too, so text measurements are accurate.
        layoutWikiDataDescription(getTitle().getDescription());

        // kick off the (asynchronous) laying out of the page title text
        layoutPageTitle((int) (getDimension(R.dimen.titleTextSize)
                / displayDensity), listener, sequence);
    }

    // ideas from:
    // http://stackoverflow.com/questions/2801116/converting-a-view-to-bitmap-without-displaying-it-in-android
    // View has to be already displayed. Note: a copy of the ImageView's Drawable must be made in
    // some fashion as it may be recycled. See T114658.
    private Bitmap newBitmapFromView(ImageView view) {
        // Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        view.draw(canvas);
        return returnedBitmap;
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
        pageTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        // if we're still not being shown (if the fragment is still being created),
        // then retry after a delay.
        if (pageTitleText.getHeight() == 0) {
            final int postDelay = 50;
            pageTitleText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layoutPageTitle(fontSizeSp, listener, sequence);
                }
            }, postDelay);
        } else {
            // give it a chance to redraw, and then see if it fits
            pageTitleText.post(new Runnable() {
                @Override
                public void run() {
                    if (!isFragmentAdded()) {
                        return;
                    }
                    if (((int) (pageTitleText.getHeight() / displayDensity) > TITLE_MAX_HEIGHT_DP)
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
            titleContainerHeight = (int) (Utils.getContentTopOffsetPx(getActivity()) / displayDensity);
            hideLeadSection();
        } else if (!leadImagesEnabled) {
            // ok, we're not going to show lead images, so we need to make some
            // adjustments to our layout:
            // make the WebView padding be just the height of the title text, plus a fixed offset
            titleContainerHeight = (int) ((pageTitleText.getHeight() / displayDensity))
                    + DISABLED_OFFSET_DP;
            imageContainer.setLayoutParams(new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    (int) ((titleContainerHeight) * displayDensity)));
            // reset the background on the lead image, in case we previously set it to white.
            image.setBackgroundColor(Color.TRANSPARENT);
            // hide the lead image
            image.setVisibility(View.GONE);
            image.setImageDrawable(null);
            imagePlaceholder.setVisibility(View.GONE);
            pageTitleText.setVisibility(View.INVISIBLE);
            // set the color of the title
            pageTitleText.setTextColor(getColor(Utils.getThemedAttributeId(getActivity(),
                    R.attr.lead_disabled_text_color)));
            // and give it no drop shadow
            pageTitleText.setShadowLayer(0, 0, 0, 0);
            pageTitleText.setBackgroundColor(Color.TRANSPARENT);
        } else {
            // we're going to show the lead image, so make some adjustments to the
            // layout, in case we were previously not showing it:
            // make the WebView padding be a proportion of the total screen height
            titleContainerHeight = (int) (displayHeightDp * IMAGES_CONTAINER_RATIO);
            imageContainer.setLayoutParams(new CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    (int) (titleContainerHeight * displayDensity)));
            // prepare the lead image to be populated
            image.setVisibility(View.INVISIBLE);
            imagePlaceholder.setVisibility(View.VISIBLE);
            pageTitleText.setVisibility(View.INVISIBLE);
            // set the color of the title
            pageTitleText.setTextColor(getColor(R.color.lead_text_color));
            // and give it a nice drop shadow!
            pageTitleText.setShadowLayer(2, 1, 1, getColor(R.color.lead_text_shadow));
            // set the title container background to be a gradient
            ViewUtil.setBackgroundDrawable(pageTitleText, pageTitleGradient);
        }

        final int paddingExtra = 8;
        setWebViewPaddingTop(titleContainerHeight + paddingExtra);

        // and start fetching the lead image, if we have one
        loadLeadImage();

        // tell our listener that it's ok to start loading the rest of the WebView content
        listener.onLayoutComplete(sequence);

        forceRefreshWebView();

        if (!isMainPage()) {
            // make everything visible!
            imageContainer.setVisibility(View.VISIBLE);
            pageTitleText.setVisibility(View.VISIBLE);
        }
    }

    // TODO: try to get DRY with hide() or consider renaming.
    private void hideLeadSection() {
        image.setVisibility(View.GONE);
        image.setImageDrawable(null);
        imagePlaceholder.setVisibility(View.GONE);
        pageTitleText.setVisibility(View.GONE);
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
        if (!TextUtils.isEmpty(description)) {
            CharSequence title = pageTitleText.getText();
            int titleLineCount = pageTitleText.getLineCount();

            SpannableStringBuilder builder = new SpannableStringBuilder(title);
            builder.append("\n");
            builder.append(subtitleSpannable(description, getDimensionPixelSize(R.dimen.descriptionTextSize)));
            pageTitleText.setText(builder);

            // Only show the description if it's two lines or less.
            if ((pageTitleText.getLineCount() - titleLineCount) > 2) {
                // Restore title.
                pageTitleText.setText(title);
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

    private void detectFace(int bmpHeight, float aspect, @Nullable PointF faceLocation) {
        int newWidth = image.getWidth();
        int newHeight = (int) (newWidth * aspect);

        // give our image an offset based on the location of the face,
        // relative to the image container
        float scale = (float) newHeight / (float) bmpHeight;
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

        forceRefreshWebView();

        // fade in the new image!
        ViewAnimations.crossFade(imagePlaceholder, image);

        startKenBurnsAnimation();
    }

    private void setImageLayoutParams(int width, int height) {
        image.setLayoutParams(new FrameLayout.LayoutParams(width, height));
    }

    private SpannableString subtitleSpannable(@Nullable CharSequence str, int sizePx) {
        final float leadingScalar = DimenUtil.getFloat(R.dimen.lead_subtitle_leading_scalar);
        final float paragraphScalar = DimenUtil.getFloat(R.dimen.lead_subtitle_paragraph_scalar);
        CharSequence nonnullStr = StringUtil.emptyIfNull(str);
        return RichTextUtil.setSpans(new SpannableString(nonnullStr),
                                     0,
                                     nonnullStr.length(),
                                     Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                                     new AbsoluteSizeSpan(sizePx, false),
                                     new LeadingSpan(leadingScalar),
                                     new ParagraphSpan(paragraphScalar));
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
            Picasso.with(getActivity())
                   .load(fullUrl)
                   .noFade()
                   .into((Target) image);
        }
    }

    /**
     * @return Nullable URL with no scheme. For example, foo.bar.com/ instead of
     * http://foo.bar.com/.
     */
    @Nullable
    private String getLeadImageUrl() {
        return getPage().getPageProperties().getLeadImageUrl();
    }

    private void startKenBurnsAnimation() {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.lead_image_zoom);
        image.startAnimation(anim);
    }

    private void forceRefreshWebView() {
        webViewScrollListener.onScrollChanged(webView.getScrollY(), webView.getScrollY());
    }

    private void initWebView() {
        webView.addOnScrollChangeListener(webViewScrollListener);

        webView.addOnClickListener(new ObservableWebView.OnClickListener() {
            @Override
            public boolean onClick(float x, float y) {
                // if the click event is within the area of the lead image, then the user
                // must have wanted to click on the lead image!
                if (leadImagesEnabled && y < (imageContainer.getHeight() - webView.getScrollY())) {
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

    private int getDimensionPixelSize(@DimenRes int id) {
        return getResources().getDimensionPixelSize(id);
    }

    @ColorInt
    private int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    private Resources getResources() {
        return getActivity().getResources();
    }

    private FragmentActivity getActivity() {
        return parentFragment.getActivity();
    }

    private class WebViewScrollListener implements ObservableWebView.OnScrollChangeListener {
        @Override
        public void onScrollChanged(int oldScrollY, int scrollY) {
            CoordinatorLayout.LayoutParams contParams = (CoordinatorLayout.LayoutParams) imageContainer
                    .getLayoutParams();
            FrameLayout.LayoutParams imgParams = (FrameLayout.LayoutParams) image.getLayoutParams();
            if (scrollY > imageContainer.getHeight()) {
                if (contParams.topMargin != -imageContainer.getHeight()) {
                    contParams.topMargin = -imageContainer.getHeight();
                    imgParams.topMargin = 0;
                    imageContainer.setLayoutParams(contParams);
                    image.setLayoutParams(imgParams);
                }
            } else {
                contParams.topMargin = -scrollY;
                imgParams.topMargin = imageBaseYOffset + scrollY / 2; //parallax, baby
                imageContainer.setLayoutParams(contParams);
                image.setLayoutParams(imgParams);
            }
        }
    }

    private class ImageLoadListener implements ImageViewWithFace.OnImageLoadListener {
        @Override
        public void onImageLoaded(Bitmap bitmap, @Nullable final PointF faceLocation) {
            final int bmpHeight = bitmap.getHeight();
            final float aspect = (float) bitmap.getHeight() / (float) bitmap.getWidth();
            imageContainer.post(new Runnable() {
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
    }
}
