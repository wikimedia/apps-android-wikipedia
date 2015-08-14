package org.wikipedia.page.leadimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.graphics.PointF;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.page.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.PageFragment;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.ViewUtil;

public class LeadImagesHandler implements ObservableWebView.OnScrollChangeListener, ImageViewWithFace.OnImageLoadListener {
    private final Context context;
    private final PageFragment parentFragment;
    private final CommunicationBridge bridge;
    private final WebView webView;

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
     * The height, in dp, that the gradient will extend above the page title.
     */
    private static final int TITLE_GRADIENT_HEIGHT_DP = 64;

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

    /**
     * Whether lead images are enabled, overall.  They will be disabled automatically
     * if the screen height is less than a defined constant (above), or if the current article
     * doesn't have a lead image associated with it.
     */
    private boolean leadImagesEnabled = false;
    public boolean isLeadImageEnabled() {
        return leadImagesEnabled;
    }

    private final ViewGroup imageContainer;
    private ImageView imagePlaceholder;
    private ImageViewWithFace image1;
    private View pageTitleContainer;
    private TextView pageTitleText;
    private TextView pageDescriptionText;
    private Drawable pageTitleGradient;

    private int displayHeightDp;
    private int imageBaseYOffset = 0;
    private float faceYOffsetNormalized = 0f;
    private float displayDensity;

    public interface OnLeadImageLayoutListener {
        void onLayoutComplete();
    }

    public LeadImagesHandler(final Context context, final PageFragment parentFragment,
                             CommunicationBridge bridge, ObservableWebView webview,
                             ViewGroup hidingView) {
        this.context = context;
        this.parentFragment = parentFragment;
        this.imageContainer = hidingView;
        this.bridge = bridge;
        this.webView = webview;

        imagePlaceholder = (ImageView)imageContainer.findViewById(R.id.page_image_placeholder);
        image1 = (ImageViewWithFace)imageContainer.findViewById(R.id.page_image_1);
        pageTitleContainer = imageContainer.findViewById(R.id.page_title_container);
        pageTitleText = (TextView)imageContainer.findViewById(R.id.page_title_text);
        pageDescriptionText = (TextView)imageContainer.findViewById(R.id.page_description_text);

        pageTitleGradient = GradientUtil.getCubicGradient(
                parentFragment.getResources().getColor(R.color.lead_gradient_start), Gravity.BOTTOM);

        initDisplayDimensions();

        webview.addOnScrollChangeListener(this);

        webview.addOnClickListener(new ObservableWebView.OnClickListener() {
            @Override
            public boolean onClick(float x, float y) {
                // if the click event is within the area of the lead image, then the user
                // must have wanted to click on the lead image!
                if (leadImagesEnabled && y < imageContainer.getHeight() - webView.getScrollY()) {
                    String imageName = parentFragment.getPage().getPageProperties()
                                                     .getLeadImageName();
                    if (imageName != null) {
                        PageTitle imageTitle = new PageTitle("File:" + imageName,
                                                             parentFragment.getTitle()
                                                                           .getSite());
                        parentFragment.showImageGallery(imageTitle, true);
                    }
                    return true;
                }
                return false;
            }
        });

        // hide ourselves by default
        hide();

        imagePlaceholder.setImageResource(Utils.getThemedAttributeId(parentFragment.getActivity(),
                R.attr.lead_image_drawable));
        image1.setOnImageLoadListener(this);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        LinearLayout.LayoutParams contParams = (LinearLayout.LayoutParams) imageContainer
                .getLayoutParams();
        LinearLayout.LayoutParams imgParams = (LinearLayout.LayoutParams) image1.getLayoutParams();
        if (scrollY > imageContainer.getHeight()) {
            if (contParams.topMargin != -imageContainer.getHeight()) {
                contParams.topMargin = -imageContainer.getHeight();
                imgParams.topMargin = 0;
                imageContainer.setLayoutParams(contParams);
                image1.setLayoutParams(imgParams);
            }
        } else {
            contParams.topMargin = -scrollY;
            imgParams.topMargin = imageBaseYOffset + scrollY / 2; //parallax, baby
            imageContainer.setLayoutParams(contParams);
            image1.setLayoutParams(imgParams);
        }
    }

    /**
     * Completely hide the lead image view. Useful in case of network errors, etc.
     * The only way to "show" the lead image view is by calling the beginLayout function.
     */
    public void hide() {
        imageContainer.setVisibility(View.INVISIBLE);
    }

    public Bitmap getLeadImageBitmap() {
        return leadImagesEnabled ? getBitmapFromView(image1) : null;
    }

    // ideas from:
    // http://stackoverflow.com/questions/2801116/converting-a-view-to-bitmap-without-displaying-it-in-android
    // View has to be already displayed
    private static Bitmap getBitmapFromView(ImageView view) {
        // Define a bitmap with the same size as the view
        Bitmap returnedBitmap
                = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        // Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
//        // Get the view's background
//        Drawable bgDrawable = view.getBackground();
//        if (bgDrawable != null)
//            // has background drawable, then draw it on the canvas
//            bgDrawable.draw(canvas);
//        else
//            // does not have background drawable, then draw white background on the canvas
//            canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return returnedBitmap;
    }

    /**
     * Returns the normalized (0.0 to 1.0) vertical focus position of the lead image.
     * A value of 0.0 represents the top of the image, and 1.0 represents the bottom.
     * The "focus position" is currently defined by automatic face detection, but may be
     * defined by other factors in the future.
     * @return Normalized vertical focus position.
     */
    public float getLeadImageFocusY() {
        return faceYOffsetNormalized;
    }

    @Override
    public void onImageLoaded(Bitmap bitmap, final PointF faceLocation) {
        final int bmpHeight = bitmap.getHeight();
        final float aspect = (float)bitmap.getHeight() / (float)bitmap.getWidth();
        imageContainer.post(new Runnable() {
            @Override
            public void run() {
                if (!parentFragment.isAdded()) {
                    return;
                }
                int newWidth = image1.getWidth();
                int newHeight = (int)(newWidth * aspect);

                // give our image an offset based on the location of the face,
                // relative to the image container
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
                    faceYOffsetNormalized = faceLocation.y / bmpHeight;
                } else {
                    // No face, so we'll just chop the top 25% off rather than centering
                    final float oneQuarter = 0.25f;
                    imageBaseYOffset = -(int)((newHeight - imagePlaceholder.getHeight()) * oneQuarter);
                    faceYOffsetNormalized = oneQuarter;
                }
                // is the offset too far to the top?
                if (imageBaseYOffset > 0) {
                    imageBaseYOffset = 0;
                }
                // is the offset too far to the bottom?
                if (imageBaseYOffset < imagePlaceholder.getHeight() - newHeight) {
                    imageBaseYOffset = imagePlaceholder.getHeight() - newHeight;
                }

                // resize our image to have the same proportions as the acquired bitmap
                if (newHeight < imagePlaceholder.getHeight()) {
                    // if the height of the image is less than the container, then just
                    // make it the same height as the placeholder.
                    image1.setLayoutParams(
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                          imagePlaceholder.getHeight()));
                    imageBaseYOffset = 0;
                } else {
                    image1.setLayoutParams(
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                          newHeight));
                }

                // and force a refresh of its position within the container view.
                onScrollChanged(webView.getScrollY(), webView.getScrollY());

                // fade in the new image!
                ViewAnimations.crossFade(imagePlaceholder, image1);

                if (WikipediaApp.getInstance().getReleaseType() != WikipediaApp.RELEASE_PROD) {
                    // and perform a subtle Ken Burns animation...
                    Animation anim = AnimationUtils.loadAnimation(parentFragment.getActivity(),
                                                                  R.anim.lead_image_zoom);
                    anim.setFillAfter(true);
                    image1.startAnimation(anim);
                }
            }
        });
    }

    @Override
    public void onImageFailed() {
        // just keep showing the placeholder image...
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
     *
     * Realistically, the whole process will happen very quickly, and almost unnoticeably to the
     * user. But it still needs to be asynchronous because we're dynamically laying out views, and
     * because the padding "event" that we send to the WebView must come before any other content
     * is sent to it, so that the effect doesn't look jarring to the user.
     *
     * @param listener Listener that will receive an event when the layout is completed.
     */
    public void beginLayout(OnLeadImageLayoutListener listener) {
        String thumbUrl = parentFragment.getPage().getPageProperties().getLeadImageUrl();
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
                // also, if the image is not a JPG (i.e. it's a PNG or SVG) and might have
                // transparency, give it a white background.
                if (!thumbUrl.endsWith(".jpg")) {
                    image1.setBackgroundColor(Color.WHITE);
                }
            }
        }

        // set the page title text, and honor any HTML formatting in the title
        pageTitleText.setText(Html.fromHtml(parentFragment.getPage().getDisplayTitle()));
        // hide the description text...
        pageDescriptionText.setVisibility(View.INVISIBLE);

        // kick off the (asynchronous) laying out of the page title text
        layoutPageTitle((int) (context.getResources().getDimension(R.dimen.titleTextSize)
                               / displayDensity), listener);
    }

    /**
     * Intermediate step in the layout process:
     * Recursive function that will dynamically size down the page title TextView if the page title
     * is too long. Since it's assumed that the overall lead image view is hidden at this stage,
     * this process will be invisible to the user, and will not appear jarring. Once the optimal
     * font size is reached, the next step in the layout process is triggered.
     * @param fontSizeSp Font size to be tested.
     * @param listener Listener that will receive an event when the layout is completed.
     */
    private void layoutPageTitle(final int fontSizeSp, final OnLeadImageLayoutListener listener) {
        if (!parentFragment.isAdded()) {
            return;
        }
        // remove padding from the title container while measuring
        pageTitleContainer.setPadding(0, 0, 0, 0);
        // set the font size of the title
        pageTitleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp);
        // if we're still not being shown (if the fragment is still being created),
        // then retry after a delay.
        if (pageTitleText.getHeight() == 0) {
            final int postDelay = 50;
            pageTitleText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layoutPageTitle(fontSizeSp, listener);
                }
            }, postDelay);
        } else {
            // give it a chance to redraw, and then see if it fits
            pageTitleText.post(new Runnable() {
                @Override
                public void run() {
                    if (!parentFragment.isAdded()) {
                        return;
                    }
                    if (((int) (pageTitleText.getHeight() / displayDensity) > TITLE_MAX_HEIGHT_DP)
                            && (fontSizeSp > TITLE_MIN_TEXT_SIZE_SP)) {
                        int newSize = fontSizeSp - TITLE_TEXT_SIZE_DECREMENT_SP;
                        if (newSize < TITLE_MIN_TEXT_SIZE_SP) {
                            newSize = TITLE_MIN_TEXT_SIZE_SP;
                        }
                        layoutPageTitle(newSize, listener);
                    } else {
                        // we're done!
                        layoutViews(listener);
                    }
                }
            });
        }
    }

    /**
     * The final step in the layout process:
     * Apply sizing and styling to our page title and lead image views, based on how large our
     * page title ended up, and whether we should display the lead image.
     * @param listener Listener that will receive an event when the layout is completed.
     */
    private void layoutViews(OnLeadImageLayoutListener listener) {
        if (!parentFragment.isAdded()) {
            return;
        }
        boolean isMainPage = parentFragment.getPage().isMainPage();
        int titleContainerHeight;

        if (isMainPage) {
            titleContainerHeight = (int)(Utils.getActionBarSize(parentFragment.getActivity()) / displayDensity);
            // hide everything
            image1.setVisibility(View.GONE);
            image1.setImageDrawable(null);
            imagePlaceholder.setVisibility(View.GONE);
            pageTitleText.setVisibility(View.GONE);
            pageDescriptionText.setVisibility(View.GONE);
        } else if (!leadImagesEnabled) {
            // ok, we're not going to show lead images, so we need to make some
            // adjustments to our layout:
            // make the WebView padding be just the height of the title text, plus a fixed offset
            titleContainerHeight = (int) ((pageTitleContainer.getHeight() / displayDensity))
                    + DISABLED_OFFSET_DP;
            imageContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) ((titleContainerHeight) * displayDensity)));
            // reset the background on the lead image, in case we previously set it to white.
            image1.setBackgroundColor(Color.TRANSPARENT);
            // hide the lead image
            image1.setVisibility(View.GONE);
            image1.setImageDrawable(null);
            imagePlaceholder.setVisibility(View.GONE);
            pageTitleText.setVisibility(View.INVISIBLE);
            pageDescriptionText.setVisibility(View.INVISIBLE);
            // set the color of the title
            pageTitleText.setTextColor(context.getResources()
                    .getColor(Utils.getThemedAttributeId(parentFragment.getActivity(),
                            R.attr.lead_disabled_text_color)));
            // remove bottom padding from the description
            ViewUtil.setBottomPaddingDp(pageDescriptionText, 0);
            // and give it no drop shadow
            pageTitleText.setShadowLayer(0, 0, 0, 0);
            // do the same for the description...
            pageDescriptionText.setTextColor(context.getResources()
                    .getColor(Utils.getThemedAttributeId(parentFragment.getActivity(),
                                                         R.attr.lead_disabled_text_color)));
            pageDescriptionText.setShadowLayer(0, 0, 0, 0);
            // remove any background from the title container
            pageTitleContainer.setBackgroundColor(Color.TRANSPARENT);
            // set the correct to padding on the container
            pageTitleContainer.setPadding(0, 0, 0, 0);
        } else {
            // we're going to show the lead image, so make some adjustments to the
            // layout, in case we were previously not showing it:
            // make the WebView padding be a proportion of the total screen height
            titleContainerHeight = (int) (displayHeightDp * IMAGES_CONTAINER_RATIO);
            imageContainer.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (titleContainerHeight * displayDensity)));
            // prepare the lead image to be populated
            image1.setVisibility(View.INVISIBLE);
            imagePlaceholder.setVisibility(View.VISIBLE);
            pageTitleText.setVisibility(View.INVISIBLE);
            pageDescriptionText.setVisibility(View.INVISIBLE);
            // set the color of the title
            pageTitleText.setTextColor(context.getResources().getColor(R.color.lead_text_color));
            // give default padding to the description
            final int bottomPadding = 16;
            ViewUtil.setBottomPaddingDp(pageDescriptionText, bottomPadding);
            // and give it a nice drop shadow!
            pageTitleText.setShadowLayer(2, 1, 1, context.getResources().getColor(R.color.lead_text_shadow));
            // do the same for the description...
            pageDescriptionText.setTextColor(context.getResources().getColor(R.color.lead_text_color));
            pageDescriptionText.setShadowLayer(2, 1, 1, context.getResources().getColor(R.color.lead_text_shadow));
            // set the title container background to be a gradient
            ViewUtil.setBackgroundDrawable(pageTitleContainer, pageTitleGradient);
            // set the correct padding on the container
            pageTitleContainer.setPadding(0, (int) (TITLE_GRADIENT_HEIGHT_DP * displayDensity), 0, 0);
        }
        if (ApiUtil.hasHoneyComb()) {
            // for API >10, decrease line spacing and boost bottom padding to account for it.
            // (in API 10, decreased line spacing cuts off the bottom of the text)
            final float lineSpacing = 0.8f;
            pageTitleText.setLineSpacing(0, lineSpacing);

        }
        // pad the webview contents, to account for the lead image view height that we've
        // ended up with
        JSONObject payload = new JSONObject();
        try {
            final int paddingExtra = 8;
            payload.put("paddingTop", titleContainerHeight + paddingExtra);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingTop", payload);

        // and start fetching the lead image, if we have one
        String thumbUrl = parentFragment.getPage().getPageProperties().getLeadImageUrl();
        if (!isMainPage && thumbUrl != null && leadImagesEnabled) {
            thumbUrl = WikipediaApp.getInstance().getNetworkProtocol() + ":" + thumbUrl;
            Picasso.with(parentFragment.getActivity())
                    .load(thumbUrl)
                    .noFade()
                    .into((Target)image1);
        }

        // tell our listener that it's ok to start loading the rest of the WebView content
        listener.onLayoutComplete();

        // trigger a scroll event so that the visibility of the lead image component is updated.
        onScrollChanged(webView.getScrollY(), webView.getScrollY());

        if (!isMainPage) {
            // make everything visible!
            imageContainer.setVisibility(View.VISIBLE);
            // kick off loading of the WikiData description
            layoutWikiDataDescription(parentFragment.getTitle().getDescription());
        }
    }

    /**
     * Final step in the WikiData description process: lay out the description, and animate it
     * into place, along with the page title.
     * @param description WikiData description to be shown.
     */
    private void layoutWikiDataDescription(@Nullable final String description) {
        // set the text of the description...
        pageDescriptionText.setText(description);
        // and wait for it to lay out, so that we know the height of the description text.
        pageDescriptionText.post(new Runnable() {
            @Override
            public void run() {
                if (!parentFragment.isAdded()) {
                    return;
                }
                // only show the description if it's two lines or less, and nonempty,
                // and adjust title padding based on whether the description is shown
                int bottomPadding = 0;
                if (TextUtils.isEmpty(description) || pageDescriptionText.getLineCount() > 2) {
                    final int blankPadding = 16;
                    bottomPadding += blankPadding;
                    pageDescriptionText.setVisibility(View.GONE);
                } else {
                    pageDescriptionText.setVisibility(View.VISIBLE);
                }
                if (ApiUtil.hasHoneyComb() && !ApiUtil.hasLollipop()) {
                    // boost the title padding a bit more, because these API versions don't
                    // automatically apply correct padding when line spacing is decreased.
                    final int extraPadding = 10;
                    bottomPadding += extraPadding;
                }
                ViewUtil.setBottomPaddingDp(pageTitleText, bottomPadding);
                pageTitleText.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Determines and sets displayDensity and displayHeightDp for the lead images layout.
     */
    private void initDisplayDimensions() {
        // preload the display density, since it will be used in a lot of places
        displayDensity = DimenUtil.getDensityScalar();

        int displayHeightPx = DimenUtil.getDisplayHeight(
                parentFragment.getActivity().getWindowManager().getDefaultDisplay());

        displayHeightDp = (int) (displayHeightPx / displayDensity);
    }
}
