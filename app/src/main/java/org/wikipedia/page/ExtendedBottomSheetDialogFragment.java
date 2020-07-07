package org.wikipedia.page;

import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    protected void disableBackgroundDim() {
        getDialog().getWindow().setDimAmount(0f);
    }
}
