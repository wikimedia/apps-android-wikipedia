package org.wikipedia.page.linkpreview;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GestureDetectorCompat;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.text.method.Touch;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class LinkPreviewDialog extends DialogFragment {
    private static final String TAG = "LinkPreviewDialog";
    private static final int DIALOG_HEIGHT = 196;

    // TODO: remove when we finalize the layout to be used.
    private int layoutToggle;

    private View previewContainer;
    private ProgressBar progressBar;
    private TextView titleText;
    private TextView descriptionText;
    private TextView extractText;
    private ImageView previewImage;

    private WikipediaApp app;
    private PageTitle pageTitle;

    private GestureDetectorCompat gestureDetector;

    public static LinkPreviewDialog newInstance(PageTitle title, int layoutToggle) {
        LinkPreviewDialog dialog = new LinkPreviewDialog();
        Bundle args = new Bundle();
        args.putParcelable("title", title);
        args.putInt("layoutToggle", layoutToggle);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        layoutToggle = getArguments().getInt("layoutToggle");
        View rootView = inflater.inflate(layoutToggle == 0 ? R.layout.dialog_link_preview : R.layout.dialog_link_preview_2, container);
        previewContainer = rootView.findViewById(R.id.link_preview_container);
        progressBar = (ProgressBar) rootView.findViewById(R.id.link_preview_progress);
        titleText = (TextView) rootView.findViewById(R.id.link_preview_title);
        previewImage = (ImageView) rootView.findViewById(R.id.link_preview_image);

        descriptionText = (TextView) rootView.findViewById(R.id.link_preview_description);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) descriptionText.getLayoutParams();
            final float marginScale = 0.7f;
            params.topMargin = (int) (params.topMargin * marginScale);
            descriptionText.setLayoutParams(params);
        }

        extractText = (TextView) rootView.findViewById(R.id.link_preview_extract);
        extractText.setMovementMethod(new ScrollingMovementMethod() {
            private float startX;
            private float startY;
            private float touchSlop = ViewConfiguration.get(getDialog().getContext()).getScaledTouchSlop();

            @Override
            public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getX();
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getX() - startX) <= touchSlop
                                && Math.abs(event.getY() - startY) <= touchSlop) {
                            goToLinkedPage();
                        }
                        break;
                    default:
                        break;
                }
                return Touch.onTouchEvent(widget, buffer, event);
            }
        });

        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });

        // show the progress bar while we load content...
        progressBar.setVisibility(View.VISIBLE);

        pageTitle = getArguments().getParcelable("title");

        // and kick off the task to load all the things...
        app = WikipediaApp.getInstance();
        new PreviewFetchTask(app.getAPIForSite(pageTitle.getSite()), pageTitle) {
            @Override
            public void onFinish(Map<PageTitle, LinkPreviewContents> result) {
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                if (result.size() > 0) {
                    layoutPreview((LinkPreviewContents) result.values().toArray()[0]);
                } else {
                    abortWithError(getString(R.string.error_network_error));
                }
            }
            @Override
            public void onCatch(Throwable caught) {
                Log.e(TAG, "caught " + caught.getMessage());
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                abortWithError(getString(R.string.error_network_error));
            }
        }.execute();

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(inflater.getContext().getResources().getDisplayMetrics().widthPixels, (int)(inflater.getContext().getResources().getDisplayMetrics().density * DIALOG_HEIGHT));
        previewContainer.setLayoutParams(lp);

        return rootView;
    }

    public void goToLinkedPage() {
        HistoryEntry historyEntry = new HistoryEntry(pageTitle, HistoryEntry.SOURCE_INTERNAL_LINK);
        getDialog().dismiss();
        ((PageActivity) getActivity()).displayNewPage(pageTitle, historyEntry);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.gravity = Gravity.BOTTOM;
        dialog.getWindow().setAttributes(lp);

        gestureDetector = new GestureDetectorCompat(dialog.getContext(), new PreviewGestureListener());
        return dialog;
    }

    private void abortWithError(String error) {
        if (getActivity() != null && getActivity().getWindow().getDecorView() != null) {
            Crouton.makeText(getActivity(), error, Style.ALERT).show();
        }
        dismiss();
    }

    private class PreviewGestureListener extends GestureDetector.SimpleOnGestureListener {
        private final float flingThreshold = 100f;
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            goToLinkedPage();
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
            Log.d(TAG, "onFling: x: " + velocityX + ", y: " + velocityY);
            if (velocityY > flingThreshold) {
                dismiss();
            } else if (velocityY < -flingThreshold) {
                goToLinkedPage();
            }
            return true;
        }
    }

    private void layoutPreview(final LinkPreviewContents contents) {
        previewContainer.setVisibility(View.VISIBLE);
        titleText.setText(contents.getTitle().getDisplayText());
        if (descriptionText.getVisibility() != View.GONE) {
            if (TextUtils.isEmpty(contents.getTitle().getDescription())) {
                descriptionText.setVisibility(View.GONE);
            } else {
                descriptionText.setText(contents.getTitle().getDescription());
                descriptionText.setVisibility(View.VISIBLE);
            }
        }
        if (contents.getExtract().size() > 0) {
            // By default, use only a single sentence for the preview...
            extractText.setText(contents.getExtract().get(0));
            extractText.post(new Runnable() {
                @Override
                public void run() {
                    final int minLineCount = 4;
                    // ...however, if the extract text turns out to be smaller than x lines,
                    // then add one more sentence to it!
                    if (extractText.getLineCount() < minLineCount
                            && contents.getExtract().size() > 1) {
                        extractText.setText(contents.getExtract().get(0) + " " + contents.getExtract().get(1));
                    }
                }
            });
        }
        if (!TextUtils.isEmpty(contents.getTitle().getThumbUrl()) && app.showImages()) {
            Picasso.with(getActivity())
                   .load(contents.getTitle().getThumbUrl())
                   .placeholder(layoutToggle == 0 ? Utils.getThemedAttributeId(getActivity(), R.attr.lead_image_drawable) : R.drawable.link_preview_gradient)
                   .error(Utils.getThemedAttributeId(getActivity(), R.attr.lead_image_drawable))
                    .into(previewImage, new Callback() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) {
                                return;
                            }
                            // and perform a subtle Ken Burns animation...
                            Animation anim = AnimationUtils.loadAnimation(getActivity(),
                                    R.anim.lead_image_zoom);
                            anim.setFillAfter(true);
                            previewImage.startAnimation(anim);
                        }

                        @Override
                        public void onError() {
                        }
                    });
        }
    }

}
