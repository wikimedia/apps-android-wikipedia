package org.wikipedia.readinglist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;

import org.wikipedia.R;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.readinglist.database.ReadingListPage;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LongPressMenu {
    public interface Callback {
        void onOpenLink(@NonNull HistoryEntry entry);
        void onOpenInNewTab(@NonNull HistoryEntry entry);
        void onAddRequest(@NonNull HistoryEntry entry, boolean addToDefault);
        void onMoveRequest(@Nullable ReadingListPage page, @NonNull HistoryEntry entry);
        void onDeleted(@Nullable ReadingListPage page, @NonNull HistoryEntry entry);
        void onCopyLink(@NonNull HistoryEntry entry);
        void onShareLink(@NonNull HistoryEntry entry);
    }

    @NonNull private final View anchorView;
    @Nullable private final Callback callback;
    @MenuRes private final int menuRes;
    private final boolean existsInAnyList;
    @Nullable private List<ReadingList> listsContainingPage;
    @Nullable private HistoryEntry entry;


    public LongPressMenu(@NonNull View anchorView, @Nullable Callback callback) {
        this(anchorView, false, callback);
    }

    public LongPressMenu(@NonNull View anchorView, boolean existsInAnyList, @Nullable Callback callback) {
        this.anchorView = anchorView;
        this.callback = callback;
        this.existsInAnyList = existsInAnyList;
        this.menuRes = existsInAnyList ? R.menu.menu_long_press : R.menu.menu_reading_list_page_toggle;
    }

    @SuppressLint("CheckResult")
    public void show(@Nullable HistoryEntry entry) {
        if (entry == null) {
            return;
        }
        Completable.fromAction(() -> {
            List<ReadingListPage> pageOccurrences = ReadingListDbHelper.instance().getAllPageOccurrences(entry.getTitle());
            listsContainingPage = ReadingListDbHelper.instance().getListsFromPageOccurrences(pageOccurrences);
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    if (!ViewCompat.isAttachedToWindow(anchorView)) {
                        return;
                    }
                    this.entry = entry;
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
            MenuItem removeItem = menu.getMenu().findItem(R.id.menu_long_press_remove_from_lists);
            removeItem.setTitle(context.getString(R.string.reading_list_remove_from_list, listsContainingPage.get(0).title()));

            MenuItem moveItem = menu.getMenu().findItem(R.id.menu_long_press_move_from_list_to_another_list);
            moveItem.setTitle(context.getString(R.string.reading_list_move_from_to_other_list, listsContainingPage.get(0).title()));
            moveItem.setVisible(true);
            moveItem.setEnabled(true);
        }

        if (existsInAnyList) {
            menu.setGravity(Gravity.END);

            MenuItem addToOtherItem = menu.getMenu().findItem(R.id.menu_long_press_add_to_another_list);
            addToOtherItem.setVisible(listsContainingPage.size() > 0);
            addToOtherItem.setEnabled(listsContainingPage.size() > 0);

            MenuItem removeItem = menu.getMenu().findItem(R.id.menu_long_press_remove_from_lists);
            removeItem.setVisible(listsContainingPage.size() > 0);
            removeItem.setEnabled(listsContainingPage.size() > 0);

            MenuItem saveItem = menu.getMenu().findItem(R.id.menu_long_press_add_to_default_list);
            saveItem.setVisible(listsContainingPage.size() == 0);
            saveItem.setEnabled(listsContainingPage.size() == 0);
        }

        menu.show();
    }

    private void deleteOrShowDialog(@NonNull Context context) {
        if (isListsContainingPageEmpty()) {
            return;
        }
        new RemoveFromReadingListsDialog(listsContainingPage).deleteOrShowDialog(context, (lists, page) -> {
                    if (callback != null && entry != null) {
                        callback.onDeleted(page, entry);
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
                case R.id.menu_long_press_open_page:
                    if (callback != null && entry != null) {
                        callback.onOpenLink(entry);
                    }
                    return true;

                case R.id.menu_long_press_open_in_new_tab:
                    if (callback != null && entry != null) {
                        callback.onOpenInNewTab(entry);
                    }
                    return true;

                case R.id.menu_long_press_add_to_default_list:
                    if (callback != null && entry != null) {
                        callback.onAddRequest(entry, true);
                    }
                    return true;

                case R.id.menu_long_press_add_to_another_list:
                    if (callback != null && entry != null && !isListsContainingPageEmpty()) {
                        callback.onAddRequest(entry, false);
                    }
                    return true;

                case R.id.menu_long_press_move_from_list_to_another_list:
                    if (callback != null && entry != null && !isListsContainingPageEmpty()) {
                        callback.onMoveRequest(listsContainingPage.get(0).pages().get(0), entry);
                    }
                    return true;

                case R.id.menu_long_press_remove_from_lists:
                    deleteOrShowDialog(anchorView.getContext());
                    return true;

                case R.id.menu_long_press_share_page:
                    if (callback != null && entry != null) {
                        callback.onShareLink(entry);
                    }
                    return true;

                case R.id.menu_long_press_copy_page:
                    if (callback != null && entry != null) {
                        callback.onCopyLink(entry);
                    }
                    return true;

                default:
                    return false;
            }
        }
    }
}
