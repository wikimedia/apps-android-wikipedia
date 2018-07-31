package org.wikipedia.page;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesClient;
import org.wikipedia.views.PageItemView;
import org.wikipedia.wikidata.DescriptionClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

/**
 * ListAdapter for disambiguation items.
 */
class DisambigListAdapter extends ArrayAdapter<DisambigResult> {
    private static final int MAX_CACHE_SIZE_IMAGES = 24;
    @NonNull private final LruCache<String, String> pageImagesCache = new LruCache<>(MAX_CACHE_SIZE_IMAGES);
    private final DisambigResult[] items;
    private final WikiSite wiki = WikipediaApp.getInstance().getWikiSite();
    private final PageItemView.Callback<DisambigResult> callback;

    DisambigListAdapter(@NonNull Context context, @NonNull DisambigResult[] items,
                        @NonNull PageItemView.Callback<DisambigResult> callback) {
        super(context, 0, items);
        this.items = items;
        this.callback = callback;
        requestPageImages();
        fetchDescriptions();
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

        new PageImagesClient().request(wiki, titleList,
                new PageImagesClient.Callback() {
                    @Override public void success(@NonNull Call<MwQueryResponse> call,
                                                  @NonNull Map<PageTitle, PageImage> results) {
                        for (Map.Entry<PageTitle, PageImage> entry : results.entrySet()) {
                            if (entry.getValue() == null || entry.getValue().getImageName() == null) {
                                continue;
                            }
                            pageImagesCache.put(entry.getKey().getPrefixedText(), entry.getValue().getImageName());
                        }
                        notifyDataSetInvalidated();
                    }
                    @Override public void failure(@NonNull Call<MwQueryResponse> call,
                                                  @NonNull Throwable caught) {
                        // Don't actually do anything.
                        // Thumbnails are expendable
                    }
                });
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

        new DescriptionClient().request(wiki, titleList, new DescriptionClient.Callback() {
            @Override public void success(@NonNull Call<MwQueryResponse> call,
                                          @NonNull List<MwQueryPage> results) {
                for (MwQueryPage page : results) {
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
            }
            @Override public void failure(@NonNull Call<MwQueryResponse> call, @NonNull Throwable caught) {
                // descriptions are expendable
            }
        });
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
