package org.wikipedia.page;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ViewGroup;

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
