package org.wikipedia.page.leadimages;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.text.Html;
import android.util.TypedValue;
import android.graphics.PointF;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.PageViewFragment;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.wikidata.WikidataCache;

import java.util.Map;

public class LeadImagesHandler implements ObservableWebView.OnScrollChangeListener, ImageViewWithFace.OnImageLoadListener {
    private final PageViewFragment parentFragment;
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
    private static final int TITLE_GRADIENT_HEIGHT_DP = 48;

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
    private boolean leadImagesEnabled = true;

    private final ViewGroup imageContainer;
    private ImageView imagePlaceholder;
    private ImageViewWithFace image1;
    private View pageTitleContainer;
    private TextView pageTitleText;
    private TextView pageDescriptionText;

    private int displayHeight;
    private int imageBaseYOffset = 0;
    private float displayDensity;

    public interface OnLeadImageLayoutListener {
        void onLayoutComplete();
    }

    public LeadImagesHandler(final PageViewFragment parentFragment, CommunicationBridge bridge,
                             ObservableWebView webview, ViewGroup hidingView) {
        this.parentFragment = parentFragment;
        this.imageContainer = hidingView;
        this.bridge = bridge;
        this.webView = webview;
        displayDensity = parentFragment.getResources().getDisplayMetrics().density;

        imagePlaceholder = (ImageView)imageContainer.findViewById(R.id.page_image_placeholder);
        image1 = (ImageViewWithFace)imageContainer.findViewById(R.id.page_image_1);
        pageTitleContainer = imageContainer.findViewById(R.id.page_title_container);
        pageTitleText = (TextView)imageContainer.findViewById(R.id.page_title_text);
        pageDescriptionText = (TextView)imageContainer.findViewById(R.id.page_description_text);
        webview.addOnScrollChangeListener(this);

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

        // hide ourselves by default
        hide();

        imagePlaceholder.setImageResource(Utils.getThemedAttributeId(parentFragment.getActivity(),
                R.attr.lead_image_drawable));
        image1.setOnImageLoadListener(this);
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        if (scrollY > imageContainer.getHeight()) {
            ViewHelper.setTranslationY(imageContainer, -imageContainer.getHeight());
            ViewHelper.setTranslationY(image1, 0);
        } else {
            ViewHelper.setTranslationY(imageContainer, -scrollY);
            ViewHelper.setTranslationY(image1, imageBaseYOffset + scrollY / 2); //parallax, baby
        }
    }

    /**
     * Completely hide the lead image view. Useful in case of network errors, etc.
     * The only way to "show" the lead image view is by calling the beginLayout function.
     */
    public void hide() {
        imageContainer.setVisibility(View.INVISIBLE);
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
                if (faceLocation.y > 0) {
                    int faceY = (int)(faceLocation.y * scale);
                    // if we have a face, then offset to the face location
                    imageBaseYOffset = -(faceY - (imagePlaceholder.getHeight() / 2));
                    // give it a slight artificial boost, so that it appears slightly
                    // above the page title...
                    final int faceBoost = 24;
                    imageBaseYOffset -= (faceBoost * displayDensity);
                } else {
                    // if we don't have a face, then center on the midpoint of the image
                    imageBaseYOffset = -(newHeight / 2
                            - (imagePlaceholder.getHeight() / 2));
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
                    // make it fill-parent
                    image1.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT));
                    imageBaseYOffset = 0;
                } else {
                    image1.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                            newHeight));
                }

                // and force a refresh of its position within the container view.
                onScrollChanged(webView.getScrollY(), webView.getScrollY());

                // fade in the new image!
                ViewAnimations.crossFade(imagePlaceholder, image1);
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
        String thumbUrl = parentFragment.getFragment().getPage().getPageProperties().getLeadImageUrl();

        if (displayHeight < MIN_SCREEN_HEIGHT_DP) {
            // disable the lead image completely
            leadImagesEnabled = false;
        } else {
            // enable the lead image, only if we actually have a url for it
            leadImagesEnabled = thumbUrl != null;
        }

        // set the page title text, and honor any HTML formatting in the title
        pageTitleText.setText(Html.fromHtml(parentFragment.getFragment().getPage().getDisplayTitle()));
        // hide the description text...
        pageDescriptionText.setVisibility(View.INVISIBLE);

        // kick off the (asynchronous) laying out of the page title text
        layoutPageTitle((int)(parentFragment.getResources().getDimension(R.dimen.titleTextSize)
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
        boolean isMainPage = parentFragment.getFragment().getPage().getPageProperties().isMainPage();
        int titleContainerHeight;
        int titleBottomPadding = 0;

        if (isMainPage) {
            titleContainerHeight = (int)(Utils.getActionBarSize(parentFragment.getActivity()) / displayDensity);
            // hide everything
            image1.setVisibility(View.GONE);
            imagePlaceholder.setVisibility(View.GONE);
            pageTitleText.setVisibility(View.GONE);
            pageDescriptionText.setVisibility(View.GONE);
        } else if (!leadImagesEnabled) {
            // ok, we're not going to show lead images, so we need to make some
            // adjustments to our layout:
            // make the WebView padding be just the height of the title text, plus a fixed offset
            titleContainerHeight = (int) ((pageTitleContainer.getHeight() / displayDensity))
                    + DISABLED_OFFSET_DP;
            imageContainer.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    (int) ((titleContainerHeight) * displayDensity)));
            // hide the lead image
            image1.setVisibility(View.GONE);
            imagePlaceholder.setVisibility(View.GONE);
            // set the color of the title
            pageTitleText.setTextColor(parentFragment
                    .getResources()
                    .getColor(Utils.getThemedAttributeId(parentFragment.getActivity(), R.attr.lead_disabled_text_color)));
            // remove bottom padding from the description
            pageDescriptionText.setPadding(pageDescriptionText.getPaddingLeft(),
                    pageDescriptionText.getPaddingTop(), pageDescriptionText.getPaddingRight(), 0);
            // and give it no drop shadow
            pageTitleText.setShadowLayer(0, 0, 0, 0);
            // do the same for the description...
            pageDescriptionText.setTextColor(parentFragment
                    .getResources()
                    .getColor(Utils.getThemedAttributeId(parentFragment.getActivity(), R.attr.lead_disabled_text_color)));
            pageDescriptionText.setShadowLayer(0, 0, 0, 0);
            // remove any background from the title container
            pageTitleContainer.setBackgroundColor(Color.TRANSPARENT);
            // set the correct to padding on the container
            pageTitleContainer.setPadding(0, 0, 0, 0);
        } else {
            // we're going to show the lead image, so make some adjustments to the
            // layout, in case we were previously not showing it:
            // make the WebView padding be a proportion of the total screen height
            titleContainerHeight = (int) (displayHeight * IMAGES_CONTAINER_RATIO);
            imageContainer.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    (int) (titleContainerHeight * displayDensity)));
            // prepare the lead image to be populated
            image1.setVisibility(View.INVISIBLE);
            imagePlaceholder.setVisibility(View.VISIBLE);

            // set the color of the title
            pageTitleText.setTextColor(parentFragment.getResources().getColor(R.color.lead_text_color));
            final int bottomPaddingNominal = 16;
            titleBottomPadding = (int)(bottomPaddingNominal * displayDensity);
            // and give it a nice drop shadow!
            pageTitleText.setShadowLayer(2, 1, 1, parentFragment.getResources().getColor(R.color.lead_text_shadow));
            // do the same for the description...
            pageDescriptionText.setTextColor(parentFragment.getResources().getColor(R.color.lead_text_color));
            pageDescriptionText.setShadowLayer(2, 1, 1, parentFragment.getResources().getColor(R.color.lead_text_shadow));
            // set the title container background to be a gradient
            pageTitleContainer.setBackgroundResource(R.drawable.lead_title_gradient);
            // set the correct padding on the container
            pageTitleContainer.setPadding(0, (int) (TITLE_GRADIENT_HEIGHT_DP * displayDensity), 0, 0);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // for API >10, decrease line spacing and boost bottom padding to account for it.
            // (in API 10, decreased line spacing cuts off the bottom of the text)
            final float lineSpacing = 0.8f;
            final int lineSpacePadding = (int)(12 * displayDensity);
            pageTitleText.setLineSpacing(0, lineSpacing);
            // however, if it's Lollipop or greater, then don't boost the bottom padding of the
            // title text, since it now correctly does it automatically.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                titleBottomPadding += lineSpacePadding;
            }
        }
        pageTitleText.setPadding(pageTitleText.getPaddingLeft(), pageTitleText.getPaddingTop(),
                pageTitleText.getPaddingRight(), titleBottomPadding);
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
        String thumbUrl = parentFragment.getFragment().getPage().getPageProperties().getLeadImageUrl();
        if (!isMainPage && thumbUrl != null && leadImagesEnabled) {
            thumbUrl = WikipediaApp.getInstance().getNetworkProtocol() + ":" + thumbUrl;
            Picasso.with(parentFragment.getActivity())
                    .load(thumbUrl)
                    .noFade()
                    .into((Target)image1);
        }

        // tell our listener that it's ok to start loading the rest of the WebView content
        listener.onLayoutComplete();

        if (!isMainPage) {
            // make everything visible!
            imageContainer.setVisibility(View.VISIBLE);

            // kick off loading of the WikiData description, if we have one
            fetchWikiDataDescription();
        }
    }

    /**
     * Start the task of fetching the WikiData description for our page, if it has one.
     * This should be done after the lead image view is laid out, but can be done independently
     * of loading the WebView contents.
     */
    private void fetchWikiDataDescription() {
        final String wikiDataId = parentFragment.getFragment().getPage().getPageProperties().getWikiDataId();
        final String language = parentFragment.getFragment().getTitle().getSite().getLanguage();

        if (wikiDataId != null) {
            WikipediaApp.getInstance().getWikidataCache().get(wikiDataId, language,
                new WikidataCache.OnWikidataReceiveListener() {
                    @Override
                    public void onWikidataReceived(Map<String, String> result) {
                        if (!parentFragment.isAdded()) {
                            return;
                        }
                        if (result.containsKey(wikiDataId)) {
                            layoutWikiDataDescription(result.get(wikiDataId));
                        }
                    }
                    @Override
                    public void onWikidataFailed(Throwable caught) {
                        // don't care
                    }
                });
        }
    }

    /**
     * Final step in the WikiData description process: lay out the description, and animate it
     * into place, along with the page title.
     * @param description WikiData description to be shown.
     */
    private void layoutWikiDataDescription(String description) {
        // set the text of the description...
        pageDescriptionText.setText(description);
        // and wait for it to lay out, so that we know the height of the description text.
        pageDescriptionText.post(new Runnable() {
            @Override
            public void run() {
                if (!parentFragment.isAdded()) {
                    return;
                }
                // only show the description if it's two lines or less
                if (pageDescriptionText.getLineCount() > 2) {
                    return;
                }
                final int animDuration = 500;
                // adjust the space between the title and the description...
                // for >2.3 and <5.0, the space needs to be a little different, because it doesn't
                // correctly adjust the bottom padding of the page title.
                final int marginSpL = 16;
                final int marginSpH = 20;
                int marginSp = marginSpL;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
                    && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    marginSp = marginSpH;
                }
                final int newMargin = pageDescriptionText.getHeight()
                        - (leadImagesEnabled ? (int)(marginSp * displayDensity) : 0);
                final int origPadding = pageTitleText.getPaddingBottom();
                // create an animation that will grow the bottom margin of the Title text,
                // pushing it upward, and creating sufficient space for the Description.
                Animation anim = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) pageTitleText.getLayoutParams();
                            params.bottomMargin = (int) (newMargin * interpolatedTime);
                            pageTitleText.setLayoutParams(params);
                        } else {
                            // for API 10, setting bottom margin doesn't work, so use padding
                            // instead. (For API >10, setting padding works too, but looks a little
                            // choppy)
                            pageTitleText.setPadding(pageTitleText.getPaddingLeft(),
                                    pageTitleText.getPaddingTop(),
                                    pageTitleText.getPaddingRight(),
                                    origPadding + (int)(newMargin * interpolatedTime));
                        }
                    }
                };
                anim.setDuration(animDuration);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // when the animation finishes, fade in the description!
                        ViewAnimations.fadeIn(pageDescriptionText);
                    }
                });
                pageTitleText.startAnimation(anim);
            }
        });
    }

}
