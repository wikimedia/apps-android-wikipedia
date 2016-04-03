package org.wikipedia.page;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.wikipedia.R;
import org.wikipedia.util.DimenUtil;

/**
 * Descendant of BottomSheetDialogFragment that adds a few features and conveniences.
 */
public class ExtendedBottomSheetDialogFragment extends BottomSheetDialogFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        setWindowLayout();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setWindowLayout();
    }

    protected void disableBackgroundDim() {
        getDialog().getWindow().setDimAmount(0f);
    }

    protected void startExpanded() {
        /*
        HACK: We'd like some of our bottom sheets to be fully expanded when opened (as
        opposed to expanded to the peek height). In order to do this, however, we have to
        call setState() only *after* the dialog is created and laid out.
        https://code.google.com/p/android/issues/detail?id=202174
        TODO: remove when this is improved in the library.
        */
        getDialog().getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                FrameLayout bottomSheet = (FrameLayout) getDialog().getWindow().getDecorView().findViewById(android.support.design.R.id.design_bottom_sheet);
                BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    private void setWindowLayout() {
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(dialogWidthPx(), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private int dialogWidthPx() {
        return Math.min(DimenUtil.getDisplayWidthPx(),
                (int) getResources().getDimension(R.dimen.bottomSheetMaxWidth));
    }
}
