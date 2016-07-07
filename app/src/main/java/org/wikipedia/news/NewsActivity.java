package org.wikipedia.news;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import org.wikipedia.Site;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.feed.news.NewsItem;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.util.ApiUtil;

public class NewsActivity extends SingleFragmentActivity<NewsFragment> {
    protected static final String EXTRA_NEWS_ITEM = "item";
    protected static final String EXTRA_SITE = "site";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ApiUtil.hasKitKat()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    public static Intent newIntent(@NonNull Context context, @NonNull NewsItem item, @NonNull Site site) {
        return new Intent(context, NewsActivity.class)
                .putExtra(EXTRA_NEWS_ITEM, GsonMarshaller.marshal(item))
                .putExtra(EXTRA_SITE, GsonMarshaller.marshal(site));
    }

    @Override
    public NewsFragment createFragment() {
        return NewsFragment.newInstance(GsonUnmarshaller.unmarshal(NewsItem.class, getIntent().getStringExtra(EXTRA_NEWS_ITEM)),
                GsonUnmarshaller.unmarshal(Site.class, getIntent().getStringExtra(EXTRA_SITE)));
    }
}
