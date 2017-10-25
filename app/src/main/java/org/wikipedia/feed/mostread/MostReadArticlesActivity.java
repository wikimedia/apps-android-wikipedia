package org.wikipedia.feed.mostread;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.json.GsonUnmarshaller;


public class MostReadArticlesActivity extends SingleFragmentActivity<MostReadFragment> {
    protected static final String MOST_READ_CARD = "item";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementTransitions();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    @Override
    public MostReadFragment createFragment() {
        return MostReadFragment.newInstance(GsonUnmarshaller.unmarshal(MostReadItemCard.class, getIntent().getStringExtra(MOST_READ_CARD)));
    }


}
