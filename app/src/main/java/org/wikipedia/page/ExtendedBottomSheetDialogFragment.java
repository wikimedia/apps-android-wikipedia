package org.wikipedia.page;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
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

    private void setWindowLayout() {
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(dialogWidthPx(), ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private int dialogWidthPx() {
        return Math.min(DimenUtil.getDisplayWidthPx(),
                (int) getResources().getDimension(R.dimen.bottomSheetMaxWidth));
    }
}
