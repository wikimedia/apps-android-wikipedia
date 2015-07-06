package org.wikipedia.page.snippet;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IntegerRes;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.appenguin.onboarding.ToolTip;

import org.wikipedia.drawable.ExposedPorterDuffColorFilterDrawableProperty;
import org.wikipedia.page.ImageLicense;
import org.wikipedia.page.ImageLicenseFetchTask;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.page.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ShareAFactFunnel;
import org.wikipedia.page.BottomDialog;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageViewFragmentInternal;
import org.wikipedia.tooltip.ToolTipUtil;
import org.wikipedia.util.ActivityUtil;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.ShareUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.wikipedia.analytics.ShareAFactFunnel.ShareMode;

/**
 * Let user choose between sharing as text or as image.
 */
public class ShareHandler {
    public static final String TAG = "ShareHandler";

    @ColorRes private static final int SHARE_TOOL_TIP_COLOR = R.color.blue_progressive;
    @ColorInt private static final int DEFAULT_ICON_COLOR = Color.WHITE;
    private static final int SHARE_TOOL_TIP_DELAY_DURATION = 3;
    private static final TimeUnit SHARE_TOOL_TIP_DELAY_UNIT = TimeUnit.SECONDS;

    private final PageActivity activity;
    private final CommunicationBridge bridge;
    private ActionMode webViewActionMode;
    private Dialog shareDialog;
    private ShareAFactFunnel funnel;

    private void createFunnel() {
        WikipediaApp app = (WikipediaApp) activity.getApplicationContext();
        final Page page = activity.getCurPageFragment().getPage();
        final PageProperties pageProperties = page.getPageProperties();
        funnel = new ShareAFactFunnel(app, page.getTitle(), pageProperties.getPageId(),
                pageProperties.getRevisionId());
    }

    public ShareHandler(PageActivity activity, CommunicationBridge bridge) {
        this.activity = activity;
        this.bridge = bridge;

        bridge.addListener("onGetTextSelection", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                String purpose = messagePayload.optString("purpose", "");
                String text = messagePayload.optString("text", "");
                if (purpose.equals("share")) {
                    createFunnel();
                    shareSnippet(text, false);
                    funnel.logShareTap(text);
                }
            }
        });
    }

    private void requestTextSelection() {
        // send an event to the WebView that will make it return the
        // selected text (or first paragraph) back to us...
        try {
            JSONObject payload = new JSONObject();
            payload.put("purpose", "share");
            bridge.sendMessage("getTextSelection", payload);
        } catch (JSONException e) {
            //nope
        }
    }

    public void shareWithoutSelection() {
        requestTextSelection();
    }

    public void onDestroy() {
        if (shareDialog != null) {
            shareDialog.dismiss();
            shareDialog = null;
        }
    }

    /** Call #setFunnel before #shareSnippet. */
    private void shareSnippet(CharSequence input, final boolean preferUrl) {
        final PageViewFragmentInternal curPageFragment = activity.getCurPageFragment();
        if (curPageFragment == null) {
            return;
        }

        final String selectedText = sanitizeText(input.toString());
        final int minTextSnippetLength = 2;
        final PageTitle title = curPageFragment.getTitle();
        if (selectedText.length() >= minTextSnippetLength) {
            final String introText = activity.getString(R.string.snippet_share_intro,
                    title.getDisplayText(),
                    title.getCanonicalUri() + "?wprov=sfia1");
            // For wprov=sfia1: see https://www.mediawiki.org/wiki/Provenance;
            // introText is only used for image share

            (new ImageLicenseFetchTask(WikipediaApp.getInstance().getAPIForSite(title.getSite()),
                    title.getSite(),
                    new PageTitle("File:" + curPageFragment.getPage().getPageProperties().getLeadImageName(), title.getSite())) {
                @Override
                public void onFinish(Map<PageTitle, ImageLicense> result) {
                    if (result.size() > 0) {
                        ImageLicense leadImageLicense = new ImageLicense("", "", "");
                        if (result.values().toArray()[0] != null) {
                            leadImageLicense = (ImageLicense) result.values().toArray()[0];
                        }

                        final SnippetImage snippetImage = new SnippetImage(activity,
                                curPageFragment.getLeadImageBitmap(),
                                curPageFragment.getLeadImageFocusY(),
                                title.getDisplayText(),
                                curPageFragment.getPage().isMainPage() ? "" : title.getDescription(),
                                selectedText,
                                leadImageLicense);

                        final Bitmap snippetBitmap = snippetImage.drawBitmap();
                        if (shareDialog != null) {
                            shareDialog.dismiss();
                        }
                        shareDialog = new PreviewDialog(activity, snippetBitmap, title.getDisplayText(), introText,
                                selectedText, preferUrl ? title.getCanonicalUri() : selectedText, funnel);
                        shareDialog.show();
                    }
                }

                @Override
                public void onCatch(Throwable caught) {
                    Log.d(TAG, "Error fetching image license info for " + title.getDisplayText() + ": " + caught.getMessage(), caught);
                }
            }).execute();
        } else {
            // only share the URL
            ShareUtils.shareText(activity, title.getDisplayText(), title.getCanonicalUri());
        }
    }

    private static String sanitizeText(String selectedText) {
        return selectedText.replaceAll("\\[\\d+\\]", "") // [1]
                .replaceAll("\\(\\s*;\\s*", "\\(") // (; -> (    hacky way for IPA remnants
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    /**
     * @param mode ActionMode under which this context is starting.
     */
    public void onTextSelected(ActionMode mode) {
        webViewActionMode = mode;
        Menu menu = mode.getMenu();

        // Find the "share" context menu item from the WebView's action mode.
        MenuItem shareItem = getSystemMenuItemByName(menu, "share");

        // if we were unable to find the Share button, then inject our own!
        if (shareItem == null) {
            shareItem = mode.getMenu().add(Menu.NONE, Menu.NONE, Menu.NONE,
                                           activity.getString(R.string.menu_share_page));
            shareItem.setIcon(R.drawable.ic_share_dark);
            MenuItemCompat.setShowAsAction(shareItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS
                                                      | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        }

        if (WikipediaApp.getInstance().getOnboardingStateMachine().isShareTutorialEnabled()) {
            showShareOnboarding(shareItem);
            WikipediaApp.getInstance().getOnboardingStateMachine().setShareTutorial();
        }

        // provide our own listener for the Share button...
        shareItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                requestTextSelection();

                // leave context mode...
                if (webViewActionMode != null) {
                    webViewActionMode.finish();
                    webViewActionMode = null;
                }
                return true;
            }
        });

        createFunnel();
        funnel.logHighlight();
    }

    /**
     * Retrieve a specific menu item from a context menu that is controlled by the system
     * by searching for the item by its actual resource name.
     * @param menu Menu to search.
     * @param nameSubstring Portion of the resource name to match.
     * @return The requested menu item, or null if it wasn't found.
     */
    private MenuItem getSystemMenuItemByName(Menu menu, String nameSubstring) {
        MenuItem foundItem = null;
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            String resourceName = null;
            try {
                resourceName = activity.getResources().getResourceName(item.getItemId());
            } catch (Resources.NotFoundException e) {
                // Looks like some devices don't provide access to these menu items through
                // the context of the app, in which case, there's nothing we can do...
            }
            if (resourceName != null && resourceName.contains(nameSubstring)) {
                foundItem = item;
            }
            // In APIs lower than 21, some of the action mode icons may not respect the
            // current theme, so we need to manually tint those icons.
            if (!ApiUtil.hasLollipop()) {
                fixMenuItemTheme(item);
            }
        }
        return foundItem;
    }

    private void fixMenuItemTheme(MenuItem item) {
        if (item != null && item.getIcon() != null) {
            WikipediaApp.getInstance().setDrawableTint(item.getIcon(), DEFAULT_ICON_COLOR);
        }
    }

    private void showShareOnboarding(MenuItem shareItem) {
        startShareIconAnimation(shareItem);
        postShowShareToolTip(shareItem);
    }

    private void postShowShareToolTip(MenuItem shareItem) {
        // There doesn't seem to be good lifecycle event accessible at the time this called to
        // ensure the tool tip is shown after CAB animation.

        final View shareItemView = ActivityUtil.getMenuItemView(activity, shareItem);
        int delay = getInteger(android.R.integer.config_longAnimTime);
        shareItemView.postDelayed(new Runnable() {
            @Override
            public void run() {
                showShareToolTip(shareItemView);
            }
        }, delay);
    }

    private void showShareToolTip(View shareItemView) {
        ToolTipUtil.showToolTip(activity, shareItemView, R.layout.inflate_tool_tip_share,
                getColor(SHARE_TOOL_TIP_COLOR), ToolTip.Position.CENTER);
    }

    private void startShareIconAnimation(MenuItem shareItem) {
        startIconAnimation(shareItem.getIcon(), getColor(SHARE_TOOL_TIP_COLOR), DEFAULT_ICON_COLOR,
                SHARE_TOOL_TIP_DELAY_UNIT, SHARE_TOOL_TIP_DELAY_DURATION);
    }

    private void startIconAnimation(Drawable icon,
                                    @ColorInt int fromColor,
                                    @ColorInt int toColor,
                                    TimeUnit unit,
                                    int duration) {
        ExposedPorterDuffColorFilterDrawableProperty
                .objectAnimator("iconColorFilter", icon, fromColor, toColor)
                .setDuration(unit.toMillis(duration))
                .start();
    }

    @ColorInt
    private int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    private int getInteger(@IntegerRes int id) {
        return getResources().getInteger(id);
    }

    private Resources getResources() {
        return activity.getResources();
    }
}

/**
 * A dialog to be displayed before sharing with two action buttons:
 * "Share as image", "Share as text".
 */
class PreviewDialog extends BottomDialog {
    private boolean completed = false;

    public PreviewDialog(final PageActivity activity, final Bitmap resultBitmap,
                         final String title, final String introText, final String selectedText,
                         final String alternativeText, final ShareAFactFunnel funnel) {
        super(activity, R.layout.dialog_share_preview);
        ImageView previewImage = (ImageView) getDialogLayout().findViewById(R.id.preview_img);
        previewImage.setImageBitmap(resultBitmap);
        getDialogLayout().findViewById(R.id.share_as_image_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShareUtils.shareImage(activity, resultBitmap, title, title, introText, false);
                        funnel.logShareIntent(selectedText, ShareMode.image);
                        completed = true;
                    }
                });
        getDialogLayout().findViewById(R.id.share_as_text_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShareUtils.shareText(activity, title, alternativeText);
                        funnel.logShareIntent(alternativeText, ShareMode.text);
                        completed = true;
                    }
                });
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                resultBitmap.recycle();
                if (!completed) {
                    funnel.logAbandoned(alternativeText);
                }
            }
        });
    }
}
