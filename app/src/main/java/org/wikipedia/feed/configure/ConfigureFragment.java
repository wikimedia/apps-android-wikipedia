package org.wikipedia.feed.configure;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.FeedConfigureFunnel;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.FeedContentType;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DefaultViewHolder;
import org.wikipedia.views.DrawableItemDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class ConfigureFragment extends Fragment implements ConfigureItemView.Callback {
    @BindView(R.id.content_types_recycler) RecyclerView recyclerView;
    private Unbinder unbinder;
    private ItemTouchHelper itemTouchHelper;
    private List<FeedContentType> orderedContentTypes = new ArrayList<>();
    @Nullable private FeedConfigureFunnel funnel;
    private CompositeDisposable disposables = new CompositeDisposable();

    @NonNull public static ConfigureFragment newInstance() {
        return new ConfigureFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed_configure, container, false);
        unbinder = ButterKnife.bind(this, view);
        setupRecyclerView();

        funnel = new FeedConfigureFunnel(WikipediaApp.getInstance(), WikipediaApp.getInstance().getWikiSite(),
                requireActivity().getIntent().getIntExtra(INTENT_EXTRA_INVOKE_SOURCE, -1));

        disposables.add(ServiceFactory.getRest(new WikiSite("wikimedia.org")).getFeedAvailability()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(this::prepareContentTypeList)
                .subscribe(result -> {
                    // apply the new availability rules to our content types
                    FeedContentType.NEWS.getLangCodesSupported().clear();
                    if (isLimitedToDomains(result.news())) {
                        addDomainNamesAsLangCodes(FeedContentType.NEWS.getLangCodesSupported(), result.news());
                    }
                    FeedContentType.ON_THIS_DAY.getLangCodesSupported().clear();
                    if (isLimitedToDomains(result.onThisDay())) {
                        addDomainNamesAsLangCodes(FeedContentType.ON_THIS_DAY.getLangCodesSupported(), result.onThisDay());
                    }
                    FeedContentType.TRENDING_ARTICLES.getLangCodesSupported().clear();
                    if (isLimitedToDomains(result.mostRead())) {
                        addDomainNamesAsLangCodes(FeedContentType.TRENDING_ARTICLES.getLangCodesSupported(), result.mostRead());
                    }
                    FeedContentType.FEATURED_ARTICLE.getLangCodesSupported().clear();
                    if (isLimitedToDomains(result.featuredArticle())) {
                        addDomainNamesAsLangCodes(FeedContentType.FEATURED_ARTICLE.getLangCodesSupported(), result.featuredArticle());
                    }
                    FeedContentType.FEATURED_IMAGE.getLangCodesSupported().clear();
                    if (isLimitedToDomains(result.featuredPicture())) {
                        addDomainNamesAsLangCodes(FeedContentType.FEATURED_IMAGE.getLangCodesSupported(), result.featuredPicture());
                    }
                    FeedContentType.saveState();
                }, L::e));

        return view;
    }

    private static boolean isLimitedToDomains(@NonNull List<String> domainNames) {
        return !domainNames.isEmpty() && !domainNames.get(0).contains("*");
    }

    private static void addDomainNamesAsLangCodes(@NonNull List<String> outList, @NonNull List<String> domainNames) {
        for (String domainName : domainNames) {
            outList.add(new WikiSite(domainName).languageCode());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        FeedContentType.saveState();
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        unbinder.unbind();
        unbinder = null;
        if (funnel != null && !orderedContentTypes.isEmpty()) {
            funnel.done(orderedContentTypes);
        }
        funnel = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_feed_configure, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_feed_configure_select_all:
                for (FeedContentType type : FeedContentType.values()) {
                    type.setEnabled(true);
                }
                touch();
                recyclerView.getAdapter().notifyDataSetChanged();
                return true;
            case R.id.menu_feed_configure_deselect_all:
                for (FeedContentType type : FeedContentType.values()) {
                    type.setEnabled(false);
                }
                touch();
                recyclerView.getAdapter().notifyDataSetChanged();
                return true;
            case R.id.menu_feed_configure_reset:
                Prefs.resetFeedCustomizations();
                FeedContentType.restoreState();
                prepareContentTypeList();
                touch();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void prepareContentTypeList() {
        orderedContentTypes.clear();
        orderedContentTypes.addAll(Arrays.asList(FeedContentType.values()));
        Collections.sort(orderedContentTypes, (FeedContentType a, FeedContentType b)
                -> a.getOrder().compareTo(b.getOrder()));
        // Remove items for which there are no available languages
        List<String> appLanguages = WikipediaApp.getInstance().language().getAppLanguageCodes();
        Iterator<FeedContentType> i = orderedContentTypes.iterator();
        while (i.hasNext()) {
            FeedContentType feedContentType = i.next();
            if (!feedContentType.showInConfig()) {
                i.remove();
                continue;
            }
            List<String> supportedLanguages = feedContentType.getLangCodesSupported();
            if (supportedLanguages.isEmpty()) {
                continue;
            }
            boolean atLeastOneSupported = false;
            for (String lang : appLanguages) {
                if (supportedLanguages.contains(lang)) {
                    atLeastOneSupported = true;
                    break;
                }
            }
            if (!atLeastOneSupported) {
                i.remove();
            }
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        ConfigureItemAdapter adapter = new ConfigureItemAdapter();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.addItemDecoration(new DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable));

        itemTouchHelper = new ItemTouchHelper(new RearrangeableItemTouchHelperCallback(adapter));
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    public void onCheckedChanged(FeedContentType contentType, boolean checked) {
        touch();
        contentType.setEnabled(checked);
    }

    @Override
    public void onLanguagesChanged(FeedContentType contentType) {
        touch();
    }

    private void updateItemOrder() {
        touch();
        for (int i = 0; i < orderedContentTypes.size(); i++) {
            orderedContentTypes.get(i).setOrder(i);
        }
    }

    private void touch() {
        requireActivity().setResult(ConfigureActivity.CONFIGURATION_CHANGED_RESULT);
    }

    private class ConfigureItemHolder extends DefaultViewHolder<ConfigureItemView> {
        ConfigureItemHolder(ConfigureItemView itemView) {
            super(itemView);
        }

        void bindItem(FeedContentType contentType) {
            getView().setContents(contentType);
        }
    }

    private final class ConfigureItemAdapter extends RecyclerView.Adapter<ConfigureItemHolder> {
        @Override
        public int getItemCount() {
            return orderedContentTypes.size();
        }

        @Override
        public ConfigureItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new ConfigureItemHolder(new ConfigureItemView(getContext()));
        }

        @Override
        public void onBindViewHolder(ConfigureItemHolder holder, int pos) {
            holder.bindItem(orderedContentTypes.get(pos));
        }

        @Override public void onViewAttachedToWindow(@NonNull ConfigureItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setDragHandleTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        itemTouchHelper.startDrag(holder);
                        break;
                    case MotionEvent.ACTION_UP:
                        v.performClick();
                        break;
                    default:
                        break;
                }
                return false;
            });
            holder.getView().setCallback(ConfigureFragment.this);
        }

        @Override public void onViewDetachedFromWindow(ConfigureItemHolder holder) {
            holder.getView().setCallback(null);
            holder.getView().setDragHandleTouchListener(null);
            super.onViewDetachedFromWindow(holder);
        }

        void onMoveItem(int oldPosition, int newPosition) {
            Collections.swap(orderedContentTypes, oldPosition, newPosition);
            updateItemOrder();
            notifyItemMoved(oldPosition, newPosition);
        }
    }

    private final class RearrangeableItemTouchHelperCallback extends ItemTouchHelper.Callback {
        private final ConfigureItemAdapter adapter;

        RearrangeableItemTouchHelperCallback(ConfigureItemAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
            adapter.onMoveItem(source.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }
    }
}
