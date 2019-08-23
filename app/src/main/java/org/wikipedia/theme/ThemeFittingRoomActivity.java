package org.wikipedia.theme;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.events.ThemeChangeEvent;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

public class ThemeFittingRoomActivity extends SingleFragmentActivity<ThemeFittingRoomFragment>
        implements ThemeChooserDialog.Callback {
    private ThemeChooserDialog themeChooserDialog;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private CompositeDisposable disposables = new CompositeDisposable();

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, ThemeFittingRoomActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        disposables.add(WikipediaApp.getInstance().getBus().subscribe(new EventBusConsumer()));

        if (savedInstanceState == null) {
            themeChooserDialog = new ThemeChooserDialog();
            bottomSheetPresenter.show(getSupportFragmentManager(), themeChooserDialog);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
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


    private class EventBusConsumer implements Consumer<Object> {
        @Override
        public void accept(Object event) {
            if (event instanceof ThemeChangeEvent) {
                ThemeFittingRoomActivity.this.recreate();
            }
        }
    }
}
