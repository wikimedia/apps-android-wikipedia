package org.wikipedia.theme;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;

public class ThemeFittingRoomActivity extends SingleFragmentActivity<ThemeFittingRoomFragment>
        implements ThemeChooserDialog.Callback {
    private ThemeChooserDialog themeChooserDialog;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, ThemeFittingRoomActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            themeChooserDialog = new ThemeChooserDialog();
            bottomSheetPresenter.show(getSupportFragmentManager(), themeChooserDialog);
        }
    }

    @Override
    protected ThemeFittingRoomFragment createFragment() {
        return ThemeFittingRoomFragment.newInstance();
    }

    @Override
    public void onToggleDimImages() {
        recreate();
    }

    @Override
    public void onCancel() {
        finish();
    }
}
