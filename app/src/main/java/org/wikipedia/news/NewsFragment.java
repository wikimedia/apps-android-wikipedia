package org.wikipedia.news;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.MainActivity;
import org.wikipedia.PageTitleListCardItemCallback;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.activity.CallbackFragment;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.feed.news.NewsItem;
import org.wikipedia.feed.view.PageTitleListCardItemView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ViewUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.richtext.RichTextUtil.stripHtml;
import static org.wikipedia.util.DimenUtil.newsFeatureImageHeightForDevice;

import static org.wikipedia.news.NewsActivity.EXTRA_NEWS_ITEM;
import static org.wikipedia.news.NewsActivity.EXTRA_SITE;

public class NewsFragment extends Fragment implements CallbackFragment<CallbackFragment.Callback> {
    @BindView(R.id.view_news_fullscreen_header_image) FaceAndColorDetectImageView image;
    @BindView(R.id.view_news_fullscreen_story_text) TextView text;
    @BindView(R.id.view_news_fullscreen_link_card_list) RecyclerView links;
    @BindView(R.id.view_news_fullscreen_toolbar) Toolbar toolbar;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter;
    @SuppressWarnings("NullableProblems") @NonNull private Unbinder unbinder;

    @NonNull
    public static NewsFragment newInstance(@NonNull NewsItem item, @NonNull Site site) {
        NewsFragment instance = new NewsFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_NEWS_ITEM, GsonMarshaller.marshal(item));
        args.putString(EXTRA_SITE, GsonMarshaller.marshal(site));
        instance.setArguments(args);
        return instance;
    }

    @Nullable
    @Override
    public CallbackFragment.Callback getCallback() {
        return FragmentUtil.getCallback(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        unbinder = ButterKnife.bind(this, view);
        bottomSheetPresenter = new ExclusiveBottomSheetPresenter(getActivity());

        ViewUtil.setTopPaddingDp(toolbar, (int) DimenUtil.getTranslucentStatusBarHeight(getContext()));
        ViewUtil.setBackgroundDrawable(toolbar, GradientUtil.getCubicGradient(
                getResources().getColor(R.color.lead_gradient_start), Gravity.TOP));
        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        NewsItem item = GsonUnmarshaller.unmarshal(NewsItem.class, getActivity().getIntent().getStringExtra(EXTRA_NEWS_ITEM));
        Site site = GsonUnmarshaller.unmarshal(Site.class, getActivity().getIntent().getStringExtra(EXTRA_SITE));

        Uri imageUri = item.featureImage();
        int height = imageUri == null ? 0 : newsFeatureImageHeightForDevice();
        DimenUtil.setViewHeight(image, height);
        image.setImageURI(imageUri);
        text.setText(stripHtml(item.story()));
        initRecycler();
        links.setAdapter(new RecyclerAdapter(item.linkCards(site), new Callback()));
        return view;
    }

    @Override public void onDestroyView() {
        unbinder.unbind();
        super.onDestroyView();
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private void initRecycler() {
        links.setLayoutManager(new LinearLayoutManager(getContext()));
        links.addItemDecoration(new DrawableItemDecoration(getContext(),
                ResourceUtil.getThemedAttributeId(getContext(), R.attr.list_separator_drawable), true));
        links.setNestedScrollingEnabled(false);
    }

    protected static class RecyclerAdapter extends DefaultRecyclerAdapter<NewsLinkCard, PageTitleListCardItemView> {
        @Nullable private Callback callback;

        protected RecyclerAdapter(@NonNull List<NewsLinkCard> items, @NonNull Callback callback) {
            super(items);
            this.callback = callback;
        }

        @Override public DefaultViewHolder<PageTitleListCardItemView> onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DefaultViewHolder<>(new PageTitleListCardItemView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(DefaultViewHolder<PageTitleListCardItemView> holder, int position) {
            NewsLinkCard card = item(position);
            holder.getView().setHistoryEntry(new HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_NEWS));
            holder.getView().setCallback(callback);
        }
    }

    private class Callback implements PageTitleListCardItemCallback {
        @Override
        public void onSelectPage(@NonNull HistoryEntry entry) {
            startActivity(MainActivity.newIntent(getContext(), entry, entry.getTitle()));
        }

        @Override
        public void onAddPageToList(@NonNull HistoryEntry entry) {
            FeedbackUtil.showAddToListDialog(entry.getTitle(),
                    AddToReadingListDialog.InvokeSource.NEWS_ACTIVITY, bottomSheetPresenter, null);
        }

        @Override
        public void onSharePage(@NonNull HistoryEntry entry) {
            ShareUtil.shareText(getActivity(), entry.getTitle());
        }
    }

}
