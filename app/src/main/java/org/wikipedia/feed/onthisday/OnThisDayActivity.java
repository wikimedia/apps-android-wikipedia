package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.UtcDate;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;

public class OnThisDayActivity extends SingleFragmentActivity<OnThisDayFragment> {
    protected static final String EXTRA_PAGES = "pages";
    protected static final String EXTRA_WIKI = "wiki";
    protected static final String EXTRA_DATE = "date";

    public static Intent newIntent(@NonNull Context context, @NonNull OnThisDay onThisDay,
                                   @NonNull WikiSite wiki, @NonNull UtcDate date) {
        return new Intent(context, OnThisDayActivity.class)
                .putExtra(EXTRA_PAGES, GsonMarshaller.marshal(onThisDay))
                .putExtra(EXTRA_WIKI, GsonMarshaller.marshal(wiki))
                .putExtra(EXTRA_DATE, GsonMarshaller.marshal(date));
    }

    @Override
    protected OnThisDayFragment createFragment() {
        return OnThisDayFragment.newInstance(GsonUnmarshaller.unmarshal(OnThisDay.class, getIntent().getStringExtra(EXTRA_PAGES)),
                GsonUnmarshaller.unmarshal(WikiSite.class, getIntent().getStringExtra(EXTRA_WIKI)), getIntent().getStringExtra(EXTRA_DATE));
    }
}
