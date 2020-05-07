package org.wikipedia.feed.news;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.FaceAndColorDetectImageView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.Constants.InvokeSource.NEWS_ACTIVITY;
import static org.wikipedia.feed.news.NewsActivity.EXTRA_NEWS_ITEM;
import static org.wikipedia.feed.news.NewsActivity.EXTRA_WIKI;
import static org.wikipedia.richtext.RichTextUtil.stripHtml;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class NewsFragment extends Fragment {
    @BindView(R.id.view_news_fullscreen_header_image) FaceAndColorDetectImageView image;
    @BindView(R.id.view_news_fullscreen_story_text) TextView text;
    @BindView(R.id.view_news_fullscreen_link_card_list) RecyclerView links;
    @BindView(R.id.view_news_fullscreen_toolbar) Toolbar toolbar;
    @BindView(R.id.news_toolbar_container) CollapsingToolbarLayout toolBarLayout;
    @BindView(R.id.news_app_bar) AppBarLayout appBarLayout;
    @BindView(R.id.view_news_fullscreen_gradient) View gradientView;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private Unbinder unbinder;

    @NonNull
    public static NewsFragment newInstance(@NonNull NewsItem item, @NonNull WikiSite wiki) {
        NewsFragment instance = new NewsFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_NEWS_ITEM, GsonMarshaller.marshal(item));
        args.putString(EXTRA_WIKI, GsonMarshaller.marshal(wiki));
        instance.setArguments(args);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_news, container, false);
        unbinder = ButterKnife.bind(this, view);

        gradientView.setBackground(GradientUtil.getPowerGradient(R.color.black54, Gravity.TOP));
        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        NewsItem item = GsonUnmarshaller.unmarshal(NewsItem.class, requireActivity().getIntent().getStringExtra(EXTRA_NEWS_ITEM));
        WikiSite wiki = GsonUnmarshaller.unmarshal(WikiSite.class, requireActivity().getIntent().getStringExtra(EXTRA_WIKI));

        setConditionalLayoutDirection(view, wiki.languageCode());

        Uri imageUri = item.featureImage();
        if (imageUri == null) {
            appBarLayout.setExpanded(false, false);
        }

        DeviceUtil.updateStatusBarTheme(requireActivity(), toolbar, true);
        appBarLayout.addOnOffsetChangedListener((layout, offset) -> {
            DeviceUtil.updateStatusBarTheme(requireActivity(), toolbar,
                    (layout.getTotalScrollRange() + offset) > layout.getTotalScrollRange() / 2);
            ((NewsActivity) requireActivity()).updateNavigationBarColor();
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            toolBarLayout.setStatusBarScrimColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color));
        }


        image.loadImage(imageUri);
        text.setText(stripHtml(item.story()));
        initRecycler();
        links.setAdapter(new RecyclerAdapter(item.linkCards(wiki), new Callback()));
        return view;
    }

    @Override public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) requireActivity();
    }

    private void initRecycler() {
        links.setLayoutManager(new LinearLayoutManager(requireContext()));
        links.addItemDecoration(new DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable));
        links.setNestedScrollingEnabled(false);
    }

    private static class RecyclerAdapter extends DefaultRecyclerAdapter<NewsLinkCard, ListCardItemView> {
        @Nullable private Callback callback;

        RecyclerAdapter(@NonNull List<NewsLinkCard> items, @NonNull Callback callback) {
            super(items);
            this.callback = callback;
        }

        @NonNull
        @Override public DefaultViewHolder<ListCardItemView> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new DefaultViewHolder<>(new ListCardItemView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull DefaultViewHolder<ListCardItemView> holder, int position) {
            NewsLinkCard card = item(position);
            holder.getView().setCard(card)
                    .setHistoryEntry(new HistoryEntry(card.pageTitle(), HistoryEntry.SOURCE_NEWS))
                    .setCallback(callback);
        }
    }

    private class Callback implements ListCardItemView.Callback {
        @Override
        public void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry) {
            startActivity(PageActivity.newIntentForCurrentTab(requireContext(), entry, entry.getTitle()));
        }

        @Override
        public void onAddPageToList(@NonNull HistoryEntry entry) {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    AddToReadingListDialog.newInstance(entry.getTitle(), NEWS_ACTIVITY));
        }

        @Override
        public void onRemovePageFromList(@NonNull HistoryEntry entry) {
            FeedbackUtil.showMessage(requireActivity(),
                    getString(R.string.reading_list_item_deleted, entry.getTitle().getDisplayText()));
        }

        @Override
        public void onSharePage(@NonNull HistoryEntry entry) {
            ShareUtil.shareText(requireActivity(), entry.getTitle());
        }
    }

}
