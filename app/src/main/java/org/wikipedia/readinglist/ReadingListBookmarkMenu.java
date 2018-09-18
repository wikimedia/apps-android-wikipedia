package org.wikipedia.readinglist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ReadingListBookmarkMenu {
    public interface Callback {
        void onAddRequest(@Nullable ReadingListPage page);
        void onDeleted(@Nullable ReadingListPage page);
        void onShare();
    }

    @NonNull private final View anchorView;
    @Nullable private final Callback callback;
    @MenuRes private final int menuRes;
    private boolean existsInAnyList;
    @Nullable private List<ReadingList> listsContainingPage;

    public ReadingListBookmarkMenu(@NonNull View anchorView, @Nullable Callback callback) {
        this(anchorView, false, callback);
    }

    public ReadingListBookmarkMenu(@NonNull View anchorView, boolean existsInAnyList, @Nullable Callback callback) {
        this.anchorView = anchorView;
        this.callback = callback;
        this.existsInAnyList = existsInAnyList;
        this.menuRes = existsInAnyList ? R.menu.menu_feed_card_item : R.menu.menu_reading_list_page_toggle;
    }

    @SuppressLint("CheckResult")
    public void show(@NonNull PageTitle title) {
        Completable.fromAction(() -> {
            List<ReadingListPage> pageOccurrences = ReadingListDbHelper.instance().getAllPageOccurrences(title);
            listsContainingPage = ReadingListDbHelper.instance().getListsFromPageOccurrences(pageOccurrences);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    if (!ViewCompat.isAttachedToWindow(anchorView)) {
                        return;
                    }
                    showMenu();
                });
    }

    private void showMenu() {
        if (!existsInAnyList && isListsContainingPageEmpty()) {
            return;
        }

        Context context = anchorView.getContext();
        PopupMenu menu = new PopupMenu(context, anchorView);
        menu.getMenuInflater().inflate(menuRes, menu.getMenu());
        menu.setOnMenuItemClickListener(new PageSaveMenuClickListener());

        if (listsContainingPage.size() == 1) {
            MenuItem removeItem = menu.getMenu().findItem(R.id.menu_remove_from_lists);
            removeItem.setTitle(context.getString(R.string.reading_list_remove_from_list, listsContainingPage.get(0).title()));
        }

        if (existsInAnyList) {
            menu.setGravity(Gravity.END);

            MenuItem addToOtherItem = menu.getMenu().findItem(R.id.menu_add_to_other_list);
            addToOtherItem.setVisible(listsContainingPage.size() > 0);
            addToOtherItem.setEnabled(listsContainingPage.size() > 0);

            MenuItem removeItem = menu.getMenu().findItem(R.id.menu_remove_from_lists);
            removeItem.setVisible(listsContainingPage.size() > 0);
            removeItem.setEnabled(listsContainingPage.size() > 0);

            MenuItem saveItem = menu.getMenu().findItem(R.id.menu_feed_card_item_save);
            saveItem.setVisible(listsContainingPage.size() == 0);
            saveItem.setEnabled(listsContainingPage.size() == 0);
        }

        menu.show();
    }

    private void deleteOrShowDialog(@NonNull Context context) {
        if (isListsContainingPageEmpty()) {
            return;
        }
        new RemoveFromReadingListsDialog(listsContainingPage).deleteOrShowDialog(context,
                page -> {
                    if (callback != null) {
                        callback.onDeleted(page);
                    }
                });
    }

    private boolean isListsContainingPageEmpty() {
        return listsContainingPage == null || listsContainingPage.isEmpty();
    }

    private class PageSaveMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_feed_card_item_save:
                    if (callback != null) {
                        callback.onAddRequest(null);
                    }
                    return true;

                case R.id.menu_feed_card_item_share:
                    if (callback != null) {
                        callback.onShare();
                    }
                    return true;

                case R.id.menu_add_to_other_list:
                    if (callback != null && !isListsContainingPageEmpty()) {
                        callback.onAddRequest(listsContainingPage.get(0).pages().get(0));
                    }
                    return true;

                case R.id.menu_remove_from_lists:
                    deleteOrShowDialog(anchorView.getContext());
                    return true;

                default:
                    return false;
            }
        }
    }
}
