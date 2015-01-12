package org.wikipedia.page.snippet;

import android.annotation.TargetApi;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.BottomDialog;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageProperties;
import org.wikipedia.page.PageViewFragmentInternal;
import org.wikipedia.util.ShareUtils;

/**
 * Allows sharing selected text in the WebView.
 */
public class SnippetShareAdapter {
    private final PageActivity activity;
    private final WikipediaApp app;
    private ActionMode webViewActionMode;
    private static ClipboardManager.OnPrimaryClipChangedListener CLIP_LISTENER;
    private MenuItem copyMenuItem;
    private MenuItem shareItem;
    private ShareAFactFunnel funnel;

    public SnippetShareAdapter(PageActivity activity) {
        this.activity = activity;
        app = (WikipediaApp) activity.getApplicationContext();
    }

    public void finish() {
        if (webViewActionMode != null) {
            webViewActionMode.finish();
        }
    }

    /**
     * Since API <11 doesn't provide a long-press context for the WebView anyway, and we're
     * using clipboard features that are only supported in API 11+, we'll mark this whole
     * method as TargetApi(11), so that the IDE doesn't get upset.
     * @param mode ActionMode under which this context is starting.
     */
    @TargetApi(11)
    public void onTextSelected(final ActionMode mode) {
        webViewActionMode = mode;
        Menu menu = mode.getMenu();

        // Find the context menu item for copying text to the clipboard...
        // The most practical way to do this seems to be to get the resource name of the
        // menu item, and see if it resembles "action_menu_copy", which appears to remain
        // consistent throughout the various APIs.
        for (int i = 0; i < menu.size(); i++) {
            String resourceName
                    = activity.getResources().getResourceName(menu.getItem(i).getItemId());
            if (resourceName.contains("action_menu_copy")) {
                copyMenuItem = menu.getItem(i);
                break;
            }
        }

        // Find the context menu item for sharing text...
        // The most practical way to do this seems to be to get the resource name of the
        // menu item, and see if it resembles "action_menu_share", which appears to remain
        // consistent throughout the various APIs.
        for (int i = 0; i < menu.size(); i++) {
            String resourceName
                    = activity.getResources().getResourceName(menu.getItem(i).getItemId());
            if (resourceName.contains("action_menu_share")) {
                shareItem = menu.getItem(i);
                break;
            }
        }

        // add our clipboard listener, so that we'll get an event when the text
        // is copied onto it...
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                Context.CLIPBOARD_SERVICE);
        if (CLIP_LISTENER == null) {
            CLIP_LISTENER = new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    // get the text from the clipboard!
                    ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                            Context.CLIPBOARD_SERVICE);
                    if (clipboard.hasPrimaryClip()
                            && clipboard.getPrimaryClip().getItemCount() > 0) {

                        CharSequence selectedText = clipboard.getPrimaryClip().getItemAt(0)
                                .coerceToText(activity);
                        Log.d("Share", ">>> Clipboard text: " + selectedText);

                        String textSnippet = sanitizeText(selectedText.toString());

                        // Pass the clipboard text to the Share handler!
                        shareSnippet(textSnippet);
                    }
                    clipboard.removePrimaryClipChangedListener(CLIP_LISTENER);
                }
            };
        }
        // remove it first, just in case it was added from the last context, and
        // ended up not being used.
        clipboard.removePrimaryClipChangedListener(CLIP_LISTENER);
        // and add it again.
        clipboard.addPrimaryClipChangedListener(CLIP_LISTENER);

        // intercept share menu...
        shareItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (copyMenuItem != null) {
                    // programmatically invoke the copy-to-clipboard action...
                    webViewActionMode.getMenu()
                            .performIdentifierAction(copyMenuItem.getItemId(), 0);
                    // this will trigger a state-change event in the Clipboard, which we'll
                    // catch with our listener above.
                }
                // leave context mode...
                if (webViewActionMode != null) {
                    webViewActionMode.finish();
                }
                return true;
            }
        });

        final Page page = activity.getCurPageFragment().getPage();
        final PageProperties pageProperties = page.getPageProperties();
        funnel = new ShareAFactFunnel(app, page.getTitle(), pageProperties.getPageId(),
                pageProperties.getRevisionId());
        funnel.logHighlight();
    }

    private String sanitizeText(String selectedText) {
        return selectedText.replaceAll("\\[\\d+\\]", "") // [1]
                .replaceAll("\\(\\s*;\\s*", "\\(") // (; -> (    hacky way for IPA remnants
                .replaceAll("\\s{2,}", " ");
    }

    private void shareSnippet(CharSequence input) {
        final int minTextSnippetLength = 1;
        String selectedText = input.toString().trim();
        if (selectedText.length() < minTextSnippetLength) {
            return;
        }
        final PageViewFragmentInternal curPageFragment = activity.getCurPageFragment();
        if (curPageFragment == null) {
            return;
        }

        PageTitle title = curPageFragment.getTitle();
        String introText = activity.getString(R.string.snippet_share_intro,
                title.getDisplayText(),
                title.getCanonicalUri() + "?source=app");
        Bitmap resultBitmap = new SnippetImage(activity,
                curPageFragment.getLeadImageBitmap(),
                curPageFragment.getImageBaseYOffset(),
                title.getDisplayText(),
                title.getDescription(),
                selectedText).createImage();
        new PreviewDialog(activity, resultBitmap, title.getDisplayText(), introText,
                selectedText, funnel).show();
    }
}


/**
 * A dialog to be displayed before sharing with two action buttons:
 * "Share as image", "Share as text".
 */
class PreviewDialog extends BottomDialog {
    public PreviewDialog(final PageActivity activity, final Bitmap resultBitmap,
                         final String title, final String introText, final String selectedText,
                         final ShareAFactFunnel funnel) {
        super(activity, R.layout.dialog_share_preview);
        ImageView previewImage = (ImageView) getDialogLayout().findViewById(R.id.preview_img);
        previewImage.setImageBitmap(resultBitmap);
        getDialogLayout().findViewById(R.id.share_as_image_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ShareUtils.shareImage(activity, resultBitmap, "*/*",
                                title, title, introText, false);
                        funnel.logShareIntent(selectedText);
                    }
                });
        getDialogLayout().findViewById(R.id.share_as_text_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent shareIntent = new Intent();
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, selectedText);
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                        shareIntent.setType("text/plain");
                        Intent chooser = Intent.createChooser(shareIntent,
                                activity.getResources().getString(R.string.share_via));
                        activity.startActivity(chooser);
                    }
                });
        setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                resultBitmap.recycle();
            }
        });
    }
}
