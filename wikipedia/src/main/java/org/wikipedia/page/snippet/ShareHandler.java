package org.wikipedia.page.snippet;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.page.BottomDialog;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageViewFragmentInternal;
import org.wikipedia.util.ShareUtils;

/**
 * Let user chose between sharing as text or as image.
 */
public class ShareHandler {
    private final PageActivity activity;
    private final ShareAFactFunnel funnel;

    public ShareHandler(PageActivity activity, ShareAFactFunnel funnel) {
        this.activity = activity;
        this.funnel = funnel;
    }

    public void shareSnippet(CharSequence input) {
        final int minTextSnippetLength = 1;
        String selectedText = sanitizeText(input.toString());
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

    private String sanitizeText(String selectedText) {
        return selectedText.replaceAll("\\[\\d+\\]", "") // [1]
                .replaceAll("\\(\\s*;\\s*", "\\(") // (; -> (    hacky way for IPA remnants
                .replaceAll("\\s{2,}", " ")
                .trim();
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
