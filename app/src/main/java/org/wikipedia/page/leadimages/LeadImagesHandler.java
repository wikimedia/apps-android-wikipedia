package org.wikipedia.page.leadimages;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.analytics.LoginFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.descriptions.DescriptionEditClient;
import org.wikipedia.descriptions.DescriptionEditTutorialActivity;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.login.LoginActivity;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ObservableWebView;

import static org.wikipedia.settings.Prefs.isDescriptionEditTutorialEnabled;
import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;
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

    @NonNull private final PageHeaderView pageHeaderView;
    private View image;

    private int displayHeightDp;

    public LeadImagesHandler(@NonNull final PageFragment parentFragment,
                             @NonNull CommunicationBridge bridge,
                             @NonNull ObservableWebView webView,
                             @NonNull PageHeaderView pageHeaderView) {
        this.parentFragment = parentFragment;

        this.pageHeaderView = pageHeaderView;
        this.pageHeaderView.setWebView(webView);

        this.bridge = bridge;
        webView.addOnScrollChangeListener(pageHeaderView);

        image = pageHeaderView.getImage();

        initDisplayDimensions();

        initArticleHeaderView();

        // hide ourselves by default
        hide();
    }

    /**
     * Completely hide the lead image view. Useful in case of network errors, etc.
     * The only way to "show" the lead image view is by calling the beginLayout function.
     */
    public void hide() {
        pageHeaderView.hide();
    }

    @Nullable public Bitmap getLeadImageBitmap() {
        return isLeadImageEnabled() ? pageHeaderView.copyBitmap() : null;
    }

    public boolean isLeadImageEnabled() {
        return isImageDownloadEnabled()
                && displayHeightDp >= MIN_SCREEN_HEIGHT_DP
                && !isMainPage()
                && !TextUtils.isEmpty(getLeadImageUrl());
    }

    public void setAnimationPaused(boolean paused) {
        pageHeaderView.setAnimationPaused(paused);
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
        pageHeaderView.setTitle(StringUtil.fromHtml(getPage().getDisplayTitle()));
        pageHeaderView.setSubtitle(StringUtils.capitalize(getTitle().getDescription()));
        pageHeaderView.setLocale(getPage().getTitle().getWikiSite().languageCode());
        pageHeaderView.setPronunciation(getPage().getTitlePronunciationUrl());
        pageHeaderView.setProtected(getPage().isProtected());
        pageHeaderView.setAllowDescriptionEdit(DescriptionEditClient.isEditAllowed(getPage()));

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
            pageHeaderView.hide();
            // explicitly set WebView padding, since onLayoutChange will not be called.
            setWebViewPaddingTop();
        } else {
            if (!isLeadImageEnabled()) {
                pageHeaderView.showText();
            } else {
                pageHeaderView.showTextImage();
            }
        }

        // tell our listener that it's ok to start loading the rest of the WebView content
        listener.onLayoutComplete(sequence);
    }

    private void setWebViewPaddingTop() {
        JSONObject payload = new JSONObject();
        try {
            payload.put("paddingTop", isMainPage()
                    ? Math.round(getContentTopOffsetPx(getActivity()) / DimenUtil.getDensityScalar())
                    : Math.round(pageHeaderView.getHeight() / DimenUtil.getDensityScalar()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        bridge.sendMessage("setPaddingTop", payload);
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

    private void loadLeadImage(@Nullable String url) {
        if (!isMainPage() && !TextUtils.isEmpty(url) && isLeadImageEnabled()) {
            pageHeaderView.loadImage(url);
        } else {
            pageHeaderView.loadImage(null);
        }
    }

    @Nullable private String getLeadImageUrl() {
        String url = getPage() == null ? null : getPage().getPageProperties().getLeadImageUrl();
        if (url == null) {
            return null;
        }

        // Conditionally add the PageTitle's URL scheme and authority if these are missing from the
        // PageProperties' URL.
        Uri fullUri = Uri.parse(url);
        String scheme = getTitle().getWikiSite().scheme();
        String authority = getTitle().getWikiSite().authority();

        if (fullUri.getScheme() != null) {
            scheme = fullUri.getScheme();
        }
        if (fullUri.getAuthority() != null) {
            authority = fullUri.getAuthority();
        }
        return new Uri.Builder()
                .scheme(scheme)
                .authority(authority)
                .path(fullUri.getPath())
                .toString();
    }

    private void startKenBurnsAnimation() {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.lead_image_zoom);
        image.startAnimation(anim);
    }

    private void initArticleHeaderView() {
        pageHeaderView.setOnImageLoadListener(new ImageLoadListener());
        pageHeaderView.addOnLayoutChangeListener((View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) -> setWebViewPaddingTop());
        pageHeaderView.setCallback(new PageHeaderView.Callback() {
            @Override
            public void onImageClicked() {
                if (getPage() != null && isLeadImageEnabled()) {
                    String imageName = getPage().getPageProperties().getLeadImageName();
                    if (imageName != null) {
                        String filename = "File:" + imageName;
                        WikiSite wiki = getTitle().getWikiSite();
                        getActivity().startActivityForResult(GalleryActivity.newIntent(getActivity(),
                                parentFragment.getTitleOriginal(), filename, wiki,
                                GalleryFunnel.SOURCE_LEAD_IMAGE),
                                Constants.ACTIVITY_REQUEST_GALLERY);
                    }
                }
            }

            @Override
            public void onDescriptionClicked() {
                verifyDescriptionEditable();
            }

            @Override
            public void onEditDescription() {
                verifyDescriptionEditable();
            }

            @Override
            public void onEditLeadSection() {
                parentFragment.getEditHandler().startEditingSection(0, null);
            }
        });
    }

    private void verifyDescriptionEditable() {
        if (getPage() != null && getPage().getPageProperties().canEdit()) {
            verifyLoggedInForDescriptionEdit();
        } else {
            parentFragment.getEditHandler().showUneditableDialog();
        }
    }

    private void verifyLoggedInForDescriptionEdit() {
        if (!AccountUtil.isLoggedIn() && Prefs.getTotalAnonDescriptionsEdited() >= parentFragment.getResources().getInteger(R.integer.description_max_anon_edits)) {
            new AlertDialog.Builder(parentFragment.getContext())
                    .setMessage(R.string.description_edit_anon_limit)
                    .setPositiveButton(R.string.menu_login, (DialogInterface dialogInterface, int i) ->
                            parentFragment.startActivity(LoginActivity.newIntent(parentFragment.getContext(), LoginFunnel.SOURCE_EDIT)))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        } else {
            startDescriptionEditActivity();
        }
    }

    private void startDescriptionEditActivity() {
        if (isDescriptionEditTutorialEnabled()) {
            parentFragment.startActivityForResult(DescriptionEditTutorialActivity.newIntent(parentFragment.getContext()),
                    Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_TUTORIAL);
        } else {
            parentFragment.startDescriptionEditActivity();
        }
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
            pageHeaderView.post(() -> {
                if (isFragmentAdded()) {
                    if (faceLocation != null) {
                        pageHeaderView.setImageFocus(faceLocation);
                    }
                    startKenBurnsAnimation();
                }
            });
        }

        @Override
        public void onImageFailed() {
        }
    }
}
