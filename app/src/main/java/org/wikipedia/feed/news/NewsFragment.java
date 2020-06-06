package org.wikipedia.feed.news;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.databinding.FragmentNewsBinding;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.ListCardItemView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.MoveToReadingListDialog;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.views.DefaultRecyclerAdapter;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;

import java.util.List;

import static org.wikipedia.Constants.InvokeSource.NEWS_ACTIVITY;
import static org.wikipedia.feed.news.NewsActivity.EXTRA_NEWS_ITEM;
import static org.wikipedia.feed.news.NewsActivity.EXTRA_WIKI;
import static org.wikipedia.richtext.RichTextUtil.stripHtml;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class NewsFragment extends Fragment {
    private FragmentNewsBinding binding;

    private RecyclerView links;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

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
        binding = FragmentNewsBinding.inflate(inflater, container, false);

        links = binding.viewNewsFullscreenLinkCardList;

        binding.viewNewsFullscreenGradient.setBackground(GradientUtil.getPowerGradient(R.color.black54, Gravity.TOP));
        getAppCompatActivity().setSupportActionBar(binding.viewNewsFullscreenToolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        NewsItem item = GsonUnmarshaller.unmarshal(NewsItem.class, requireActivity().getIntent().getStringExtra(EXTRA_NEWS_ITEM));
        WikiSite wiki = GsonUnmarshaller.unmarshal(WikiSite.class, requireActivity().getIntent().getStringExtra(EXTRA_WIKI));

        setConditionalLayoutDirection(binding.getRoot(), wiki.languageCode());

        Uri imageUri = item.featureImage();
        if (imageUri == null) {
            binding.newsAppBar.setExpanded(false, false);
        }

        DeviceUtil.updateStatusBarTheme(requireActivity(), binding.viewNewsFullscreenToolbar, true);
        binding.newsAppBar.addOnOffsetChangedListener((layout, offset) -> {
            DeviceUtil.updateStatusBarTheme(requireActivity(), binding.viewNewsFullscreenToolbar,
                    (layout.getTotalScrollRange() + offset) > layout.getTotalScrollRange() / 2);
            ((NewsActivity) requireActivity()).updateNavigationBarColor();
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.newsToolbarContainer.setStatusBarScrimColor(ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color));
        }


        binding.viewNewsFullscreenHeaderImage.loadImage(imageUri);
        binding.viewNewsFullscreenStoryText.setText(stripHtml(item.story()));
        initRecycler();
        links.setAdapter(new RecyclerAdapter(item.linkCards(wiki), new Callback()));
        return binding.getRoot();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        public void onMovePageToList(long sourceReadingListId, @NonNull HistoryEntry entry) {
            bottomSheetPresenter.show(getChildFragmentManager(),
                    MoveToReadingListDialog.newInstance(sourceReadingListId, entry.getTitle(), NEWS_ACTIVITY));
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
