package org.wikipedia.page;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.DimenUtil;

/**
 * Descendant of BottomSheetDialogFragment that adds a few features and conveniences.
 */
public class ExtendedBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private boolean enableFullWidthDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        setWindowLayout();
        setPeekHeightPerDevice();
    }

    private void setPeekHeightPerDevice() {
        View view = getView();
        if (view != null) {
            view.post(() -> {
                View parent = (View) view.getParent();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) (parent).getLayoutParams();
                CoordinatorLayout.Behavior behavior = params.getBehavior();
                BottomSheetBehavior bottomSheetBehavior = (BottomSheetBehavior) behavior;
                int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
                if (bottomSheetBehavior != null) {
                    bottomSheetBehavior.setPeekHeight(screenHeight / 2);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WikipediaApp.getInstance().getRefWatcher().watch(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setWindowLayout();
    }

    protected void disableBackgroundDim() {
        getDialog().getWindow().setDimAmount(0f);
    }

    protected void enableFullWidthDialog() {
        enableFullWidthDialog = true;
    }

    private void setWindowLayout() {
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(dialogWidthPx(), ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private int dialogWidthPx() {
        return enableFullWidthDialog
                ? ViewGroup.LayoutParams.MATCH_PARENT
                : Math.min(DimenUtil.getDisplayWidthPx(), (int) getResources().getDimension(R.dimen.bottomSheetMaxWidth));
    }
}
