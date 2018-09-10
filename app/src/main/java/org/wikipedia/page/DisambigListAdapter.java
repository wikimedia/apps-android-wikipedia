package org.wikipedia.page;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwException;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.util.log.L;
import org.wikipedia.views.PageItemView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * ListAdapter for disambiguation items.
 */
class DisambigListAdapter extends ArrayAdapter<DisambigResult> {
    private static final int MAX_CACHE_SIZE_IMAGES = 24;
    @NonNull private final LruCache<String, String> pageImagesCache = new LruCache<>(MAX_CACHE_SIZE_IMAGES);
    private final DisambigResult[] items;
    private final WikiSite wiki = WikipediaApp.getInstance().getWikiSite();
    private final PageItemView.Callback<DisambigResult> callback;
    private CompositeDisposable disposables = new CompositeDisposable();

    DisambigListAdapter(@NonNull Context context, @NonNull DisambigResult[] items,
                        @NonNull PageItemView.Callback<DisambigResult> callback) {
        super(context, 0, items);
        this.items = items;
        this.callback = callback;
        requestPageImages();
        fetchDescriptions();
    }

    void dispose() {
        disposables.clear();
    }

    private void requestPageImages() {
        List<PageTitle> titleList = new ArrayList<>();
        for (DisambigResult r : items) {
            if (pageImagesCache.get(r.getTitle().getPrefixedText()) == null) {
                // not in our cache yet
                titleList.add(r.getTitle());
            }
        }
        if (titleList.isEmpty()) {
            return;
        }

        disposables.add(ServiceFactory.get(wiki).getPageImages(TextUtils.join("|", titleList))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(queryResponse -> PageImage.imageMapFromPages(wiki, titleList, queryResponse.query().pages()))
                .subscribe(map -> {
                    for (Map.Entry<PageTitle, PageImage> entry : map.entrySet()) {
                        if (entry.getValue() == null || entry.getValue().getImageName() == null) {
                            continue;
                        }
                        pageImagesCache.put(entry.getKey().getPrefixedText(), entry.getValue().getImageName());
                    }
                    notifyDataSetInvalidated();
                }, L::w));
    }

    /**
     * Start getting Wikidata descriptions (directly from the current Wikipedia wiki).
     */
    private void fetchDescriptions() {
        final List<PageTitle> titleList = new ArrayList<>();
        for (DisambigResult r : items) {
            titleList.add(r.getTitle());
        }
        if (titleList.isEmpty()) {
            return;
        }

        disposables.add(ServiceFactory.get(wiki).getDescription(TextUtils.join("|", titleList))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(response -> {
                    if (response.success()) {
                        // noinspection ConstantConditions
                        return response.query().pages();
                    }
                    throw new MwException(response.getError());
                })
                .subscribe(pages -> {
                    for (MwQueryPage page : pages) {
                        PageTitle pageTitle = new PageTitle(null, page.title(), wiki);
                        for (PageTitle title : titleList) {
                            if (title.getPrefixedText().equals(pageTitle.getPrefixedText())
                                    || title.getDisplayText().equals(pageTitle.getDisplayText())) {
                                title.setDescription(page.description());
                                break;
                            }
                        }
                    }
                    notifyDataSetChanged();
                }));
    }

    @Override @NonNull public View getView(int position, View convView, @NonNull ViewGroup parent) {
        PageItemView<DisambigResult> itemView = (PageItemView<DisambigResult>) convView;
        if (itemView == null) {
            itemView = new PageItemView<>(getContext());
            itemView.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        DisambigResult item = items[position];
        itemView.setItem(item);
        itemView.setCallback(callback);

        itemView.setTitle(item.getTitle().getDisplayText());
        itemView.setDescription(StringUtils.capitalize(item.getTitle().getDescription()));
        itemView.setImageUrl(pageImagesCache.get(item.getTitle().getPrefixedText()));
        return itemView;
    }
}
