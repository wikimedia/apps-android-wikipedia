package org.wikipedia.page.leadimages;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.nineoldandroids.view.ViewHelper;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.PageViewFragment;
import org.wikipedia.views.ObservableWebView;

public class LeadImagesHandler implements ObservableWebView.OnScrollChangeListener {
    private final PageViewFragment parentFragment;
    private final CommunicationBridge bridge;

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
     * Default font size of the page title, before any resizing.
     */
    private static final int TITLE_DEFAULT_TEXT_SIZE_SP = 28;

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
    private static final int DISABLED_OFFSET_DP = 32;

    /**
     * Whether lead images are enabled, overall.  They will be disabled automatically
     * if the screen height is less than a defined constant (above), or if the current article
     * doesn't have a lead image associated with it.
     */
    private boolean leadImagesEnabled = true;

    private final ViewGroup imageContainer;
    private ImageView image1;
    private View pageTitleContainer;
    private TextView pageTitleText;

    private int displayHeight;
    private float displayDensity;

    public interface OnLeadImageLayoutListener {
        void onLayoutComplete();
    }

    public LeadImagesHandler(PageViewFragment parentFragment, CommunicationBridge bridge,
                             ObservableWebView webview, ViewGroup hidingView) {
        this.parentFragment = parentFragment;
        this.imageContainer = hidingView;
        this.bridge = bridge;

        image1 = (ImageView)imageContainer.findViewById(R.id.page_image_1);
        pageTitleContainer = imageContainer.findViewById(R.id.page_title_container);
        pageTitleText = (TextView)imageContainer.findViewById(R.id.page_title_text);
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
    }

    @Override
    public void onScrollChanged(int oldScrollY, int scrollY) {
        if (scrollY > imageContainer.getHeight()) {
            ViewHelper.setTranslationY(imageContainer, -imageContainer.getHeight());
            ViewHelper.setTranslationY(image1, 0);
        } else {
            ViewHelper.setTranslationY(imageContainer, -scrollY);
            ViewHelper.setTranslationY(image1, scrollY / 2); //parallax, baby
        }
    }

    /**
     * Completely hide the lead image view. Useful in case of network errors, etc.
     * The only way to "show" the lead image view is by calling the beginLayout function.
     */
    public void hide() {
        imageContainer.setVisibility(View.INVISIBLE);
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

        // give the title to the measurer...
        pageTitleText.setText(parentFragment.getFragment().getPage().getTitle().getDisplayText());

        // kick off the (asynchronous) laying out of the page title text
        layoutPageTitle(TITLE_DEFAULT_TEXT_SIZE_SP, listener);

        // and start fetching the lead image, if we have one
        if (thumbUrl != null && leadImagesEnabled) {
            thumbUrl = WikipediaApp.getInstance().getNetworkProtocol() + ":" + thumbUrl;
            Picasso.with(parentFragment.getActivity())
                    .load(thumbUrl)
                    .placeholder(Utils.getThemedAttributeId(parentFragment.getActivity(), R.attr.lead_image_drawable))
                    .error(Utils.getThemedAttributeId(parentFragment.getActivity(), R.attr.lead_image_drawable))
                    .into(image1);
        }
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
        final int webViewPadding;
        if (!leadImagesEnabled) {
            // ok, we're not going to show lead images, so we need to make some
            // adjustments to our layout:
            // make the WebView padding be just the height of the title text, plus a fixed offset
            webViewPadding = (int) ((pageTitleContainer.getHeight() / displayDensity))
                    + DISABLED_OFFSET_DP;
            imageContainer.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT));
            // hide the lead image
            image1.setVisibility(View.GONE);
            // set the color of the title
            pageTitleText.setTextColor(parentFragment
                    .getResources()
                    .getColor(Utils.getThemedAttributeId(parentFragment.getActivity(), R.attr.lead_disabled_text_color)));
            // remove any background from the title container
            pageTitleContainer.setBackgroundColor(Color.TRANSPARENT);
            // set the correct to padding on the container
            pageTitleContainer.setPadding(0, 0, 0, 0);
            // and give it no drop shadow
            pageTitleText.setShadowLayer(0, 0, 0, 0);
        } else {
            // we're going to show the lead image, so make some adjustments to the
            // layout, in case we were previously not showing it:
            // make the WebView padding be a proportion of the total screen height
            webViewPadding = (int) (displayHeight * IMAGES_CONTAINER_RATIO);
            imageContainer.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    (int) (webViewPadding * displayDensity)));
            // show the lead image
            image1.setVisibility(View.VISIBLE);
            // set the color of the title
            pageTitleText.setTextColor(parentFragment.getResources().getColor(R.color.lead_text_color));
            // set the title container background to be a gradient
            pageTitleContainer.setBackgroundResource(R.drawable.lead_title_gradient);
            // set the correct padding on the container
            pageTitleContainer.setPadding(0, (int) (TITLE_GRADIENT_HEIGHT_DP * displayDensity), 0, 0);
            // and give it a nice drop shadow!
            pageTitleText.setShadowLayer(2, 1, 1, parentFragment.getResources().getColor(R.color.lead_text_shadow));
        }
        // pad the webview contents, to account for the lead image view height that we've
        // ended up with
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingTop", webViewPadding);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingTop", payload);

        // make everything visible!
        imageContainer.setVisibility(View.VISIBLE);

        listener.onLayoutComplete();
    }

}
