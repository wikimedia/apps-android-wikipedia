package org.wikipedia.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.NotificationFunnel;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.history.SearchActionModeCallback;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.settings.NotificationSettingsActivity;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.DrawableItemDecoration;
import org.wikipedia.views.MultiSelectActionModeCallback;
import org.wikipedia.views.SwipeableItemTouchHelperCallback;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.util.ResourceUtil.getThemedColor;

public class NotificationActivity extends BaseActivity implements NotificationItemActionsDialog.Callback {

    @BindView(R.id.notifications_refresh_view) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.notifications_recycler_view) RecyclerView recyclerView;
    @BindView(R.id.notifications_progress_bar) View progressBarView;
    @BindView(R.id.notifications_error_view) WikiErrorView errorView;
    @BindView(R.id.notifications_empty_container) View emptyContainerView;
    @BindView(R.id.notifications_view_archived_button) View archivedButtonView;

    private List<Notification> notificationList = new ArrayList<>();
    private List<NotificationListItemContainer> notificationContainerList = new ArrayList<>();
    private CompositeDisposable disposables = new CompositeDisposable();

    private Map<String, WikiSite> dbNameMap = new HashMap<>();
    @Nullable private String currentContinueStr;

    @Nullable private ActionMode actionMode;
    private MultiSelectCallback multiSelectActionModeCallback = new MultiSelectCallback();
    private SearchCallback searchActionModeCallback = new SearchCallback();
    @Nullable String currentSearchQuery;

    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private boolean displayArchived;

    public static Intent newIntent(@NonNull Context context) {
        return new Intent(context, NotificationActivity.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        ButterKnife.bind(this);

        errorView.setRetryClickListener((v) -> beginUpdateList());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DrawableItemDecoration(this, R.attr.list_separator_drawable));

        ItemTouchHelper.Callback touchCallback = new SwipeableItemTouchHelperCallback(this,
                ResourceUtil.getThemedAttributeId(this, R.attr.chart_shade5),
                R.drawable.ic_archive_white_24dp,
                ResourceUtil.getThemedAttributeId(this, R.attr.secondary_text_color));
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(touchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
            beginUpdateList();
        });

        beginUpdateList();

        NotificationSettingsActivity.promptEnablePollDialog(this);
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notifications, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemArchived = menu.findItem(R.id.menu_notifications_view_archived);
        MenuItem itemUnread = menu.findItem(R.id.menu_notifications_view_unread);
        itemArchived.setVisible(!displayArchived);
        itemUnread.setVisible(displayArchived);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_notifications_view_archived:
                onViewArchivedClick(null);
                return true;
            case R.id.menu_notifications_view_unread:
                displayArchived = false;
                beginUpdateList();
                return true;
            case R.id.menu_notifications_prefs:
                startActivity(NotificationSettingsActivity.newIntent(this));
                return true;
            case R.id.menu_notifications_search:
                startSupportActionMode(searchActionModeCallback);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (displayArchived) {
            displayArchived = false;
            beginUpdateList();
            return;
        }
        super.onBackPressed();
    }

    @OnClick(R.id.notifications_view_archived_button)
    void onViewArchivedClick(View v) {
        displayArchived = true;
        beginUpdateList();
    }


    private void beginUpdateList() {
        errorView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyContainerView.setVisibility(View.GONE);
        progressBarView.setVisibility(View.VISIBLE);
        getSupportActionBar().setTitle(displayArchived ? R.string.notifications_activity_title_archived : R.string.notifications_activity_title);

        currentContinueStr = null;
        disposables.clear();

        // if we're not checking for unread notifications, then short-circuit straight to fetching them.
        if (displayArchived) {
            getOrContinueNotifications();
            return;
        }

        disposables.add(ServiceFactory.get(WikipediaApp.getInstance().getWikiSite()).getUnreadNotificationWikis()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    Map<String, Notification.UnreadNotificationWikiItem> wikiMap = response.query().unreadNotificationWikis();
                    dbNameMap.clear();
                    for (String key : wikiMap.keySet()) {
                        if (wikiMap.get(key).getSource() != null) {
                            dbNameMap.put(key, new WikiSite(wikiMap.get(key).getSource().getBase()));
                        }
                    }
                    getOrContinueNotifications();
                }, this::setErrorState));
    }

    private void getOrContinueNotifications() {
        progressBarView.setVisibility(View.VISIBLE);
        disposables.add(ServiceFactory.get(WikipediaApp.getInstance().getWikiSite()).getAllNotifications("*", displayArchived ? "read" : "!read", currentContinueStr)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    onNotificationsComplete(response.query().notifications().list(), !TextUtils.isEmpty(currentContinueStr));
                    currentContinueStr = response.query().notifications().getContinue();
                }, this::setErrorState));
    }

    private void setErrorState(Throwable t) {
        L.e(t);
        progressBarView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyContainerView.setVisibility(View.GONE);
        errorView.setError(t);
        errorView.setVisibility(View.VISIBLE);
    }

    private void setSuccessState() {
        progressBarView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private void onNotificationsComplete(@NonNull List<Notification> notifications, boolean fromContinuation) {
        setSuccessState();
        if (!fromContinuation) {
            notificationList.clear();
            recyclerView.setAdapter(new NotificationItemAdapter());
        }
        for (Notification n : notifications) {
            boolean exists = false;
            for (Notification nExisting : notificationList) {
                if (nExisting.id() == n.id()) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                notificationList.add(n);
            }
        }
        postprocessAndDisplay();
    }

    private void postprocessAndDisplay() {
        // Sort them by descending date...
        Collections.sort(notificationList, (n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()));

        // Build the container list, and punctuate it by date granularity, while also applying the
        // current search query.
        notificationContainerList.clear();
        long millis = Long.MAX_VALUE;
        for (Notification n : notificationList) {

            // TODO: remove this condition when the time is right.
            if ((n.type().equals(Notification.TYPE_WELCOME) && Prefs.notificationWelcomeEnabled())
                    || (n.type().equals(Notification.TYPE_EDIT_THANK) && Prefs.notificationThanksEnabled())
                    || (n.type().equals(Notification.TYPE_EDIT_MILESTONE) && Prefs.notificationMilestoneEnabled())
                    || Prefs.showAllNotifications()) {

                if (!TextUtils.isEmpty(currentSearchQuery) && n.getContents() != null
                        && !n.getContents().getHeader().contains(currentSearchQuery)) {
                    continue;
                }
                if (millis - n.getTimestamp().getTime() > TimeUnit.DAYS.toMillis(1)) {
                    notificationContainerList.add(new NotificationListItemContainer(n.getTimestamp()));
                    millis = n.getTimestamp().getTime();
                }
                notificationContainerList.add(new NotificationListItemContainer(n));

            }
        }
        recyclerView.getAdapter().notifyDataSetChanged();

        if (notificationContainerList.isEmpty()) {
            emptyContainerView.setVisibility(View.VISIBLE);
            archivedButtonView.setVisibility(displayArchived ? View.GONE : View.VISIBLE);
        } else {
            emptyContainerView.setVisibility(View.GONE);
        }
    }

    private void deleteItems(List<NotificationListItemContainer> items, boolean markUnread) {
        Map<WikiSite, List<Notification>> notificationsPerWiki = new HashMap<>();
        Long selectionKey = items.size() > 1 ? new Random().nextLong() : null;

        for (NotificationListItemContainer item : items) {
            WikiSite wiki = dbNameMap.containsKey(item.notification.wiki())
                    ? dbNameMap.get(item.notification.wiki()) : WikipediaApp.getInstance().getWikiSite();
            if (!notificationsPerWiki.containsKey(wiki)) {
                notificationsPerWiki.put(wiki, new ArrayList<>());
            }
            notificationsPerWiki.get(wiki).add(item.notification);
            if (markUnread && !displayArchived) {
                notificationList.add(item.notification);
            } else {
                notificationList.remove(item.notification);
                new NotificationFunnel(WikipediaApp.getInstance(), item.notification).logMarkRead(selectionKey);
            }
        }

        for (WikiSite wiki : notificationsPerWiki.keySet()) {
            if (markUnread) {
                NotificationPollBroadcastReceiver.markRead(wiki, notificationsPerWiki.get(wiki), true);
            } else {
                NotificationPollBroadcastReceiver.markRead(wiki, notificationsPerWiki.get(wiki), false);
                showDeleteItemsUndoSnackbar(items);
            }
        }
        postprocessAndDisplay();
    }

    private void showDeleteItemsUndoSnackbar(final List<NotificationListItemContainer> items) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(this, getResources().getQuantityString(R.plurals.notification_archive_message, items.size(), items.size()),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.history_item_delete_undo, v -> deleteItems(items, true));
        snackbar.show();
    }


    private void finishActionMode() {
        if (actionMode != null) {
            actionMode.finish();
        }
    }

    private void beginMultiSelect() {
        if (SearchCallback.is(actionMode)) {
            finishActionMode();
        }
        if (!MultiSelectCallback.is(actionMode)) {
            startSupportActionMode(multiSelectActionModeCallback);
        }
    }

    private void toggleSelectItem(@NonNull NotificationListItemContainer container) {
        container.selected = !container.selected;
        int selectedCount = getSelectedItemCount();
        if (selectedCount == 0) {
            finishActionMode();
        } else if (actionMode != null) {
            actionMode.setTitle(getString(R.string.multi_select_items_selected, selectedCount));
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    private int getSelectedItemCount() {
        int selectedCount = 0;
        for (NotificationListItemContainer item : notificationContainerList) {
            if (item.selected) {
                selectedCount++;
            }
        }
        return selectedCount;
    }

    private void unselectAllItems() {
        for (NotificationListItemContainer item : notificationContainerList) {
            item.selected = false;
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    @NonNull
    private List<NotificationListItemContainer> getSelectedItems() {
        List<NotificationListItemContainer> result = new ArrayList<>();
        for (NotificationListItemContainer item : notificationContainerList) {
            if (item.selected) {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public void onArchive(@NonNull Notification notification) {
        bottomSheetPresenter.dismiss(getSupportFragmentManager());
        for (NotificationListItemContainer c : notificationContainerList) {
            if (c.notification != null && c.notification.key() == notification.key()) {
                deleteItems(Collections.singletonList(c), displayArchived);
                break;
            }
        }
    }

    @Override
    public void onActionPageTitle(@NonNull PageTitle pageTitle) {
        startActivity(PageActivity.newIntentForNewTab(this,
                new HistoryEntry(pageTitle, HistoryEntry.SOURCE_NOTIFICATION), pageTitle));
    }

    @Override
    public boolean isShowingArchived() {
        return displayArchived;
    }


    private class NotificationItemHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        private NotificationListItemContainer container;
        private TextView titleView;
        private TextView descriptionView;
        private TextView wikiCodeView;
        private TextView secondaryActionHintView;
        private TextView tertiaryActionHintView;
        private AppCompatImageView wikiCodeBackgroundView;
        private AppCompatImageView wikiCodeImageView;
        private View imageContainerView;
        private View imageSelectedView;
        private AppCompatImageView imageView;
        private AppCompatImageView imageBackgroundView;

        NotificationItemHolder(View view) {
            super(view);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            titleView = view.findViewById(R.id.notification_item_title);
            descriptionView = view.findViewById(R.id.notification_item_description);
            secondaryActionHintView = view.findViewById(R.id.notification_item_secondary_action_hint);
            tertiaryActionHintView = view.findViewById(R.id.notification_item_tertiary_action_hint);
            wikiCodeView = view.findViewById(R.id.notification_wiki_code);
            wikiCodeImageView = view.findViewById(R.id.notification_wiki_code_image);
            wikiCodeBackgroundView = view.findViewById(R.id.notification_wiki_code_background);
            imageContainerView = view.findViewById(R.id.notification_item_image_container);
            imageBackgroundView = view.findViewById(R.id.notification_item_image_background);
            imageSelectedView = view.findViewById(R.id.notification_item_selected_image);
            imageView = view.findViewById(R.id.notification_item_image);
        }

        protected NotificationListItemContainer getContainer() {
            return container;
        }

        void bindItem(NotificationListItemContainer container) {
            this.container = container;
            Notification n = container.notification;

            String description = n.type();
            int iconResId = R.drawable.ic_wikipedia_w;
            int iconBackColor = R.color.base0;

            switch (n.type()) {
                case Notification.TYPE_EDIT_USER_TALK:
                    iconResId = R.drawable.ic_chat_white_24dp;
                    break;
                case Notification.TYPE_REVERTED:
                    iconResId = R.drawable.ic_rotate_left_white_24dp;
                    iconBackColor = R.color.red50;
                    break;
                case Notification.TYPE_EDIT_THANK:
                    iconResId = R.drawable.ic_usertalk_constructive;
                    iconBackColor = R.color.green50;
                    break;
                case Notification.TYPE_EDIT_MILESTONE:
                    iconResId = R.drawable.ic_mode_edit_white_24dp;
                    iconBackColor = R.color.accent50;
                    break;
                default:
                    break;
            }
            imageView.setImageResource(iconResId);
            DrawableCompat.setTint(imageBackgroundView.getDrawable(),
                    ContextCompat.getColor(NotificationActivity.this, iconBackColor));

            if (n.getContents() != null) {
                description = n.getContents().getHeader();
            }

            titleView.setText(StringUtil.fromHtml(description));

            if (n.getContents() != null && !TextUtils.isEmpty(n.getContents().getBody().trim())) {
                descriptionView.setText(StringUtil.fromHtml(n.getContents().getBody()));
                descriptionView.setVisibility(View.VISIBLE);
            } else {
                descriptionView.setVisibility(View.GONE);
            }

            if (n.getContents() != null && n.getContents().getLinks() != null && n.getContents().getLinks().getSecondary() != null
                    && n.getContents().getLinks().getSecondary().size() > 0) {
                secondaryActionHintView.setText(n.getContents().getLinks().getSecondary().get(0).getLabel());
                secondaryActionHintView.setVisibility(View.VISIBLE);
                if (n.getContents().getLinks().getSecondary().size() > 1) {
                    tertiaryActionHintView.setText(n.getContents().getLinks().getSecondary().get(1).getLabel());
                    tertiaryActionHintView.setVisibility(View.VISIBLE);
                } else {
                    tertiaryActionHintView.setVisibility(View.GONE);
                }
            } else {
                secondaryActionHintView.setVisibility(View.GONE);
                tertiaryActionHintView.setVisibility(View.GONE);
            }

            String wikiCode = n.wiki();
            if (wikiCode.contains("wikidata")) {
                wikiCodeView.setVisibility(View.GONE);
                wikiCodeBackgroundView.setVisibility(View.GONE);
                wikiCodeImageView.setVisibility(View.VISIBLE);
                wikiCodeImageView.setImageResource(R.drawable.ic_wikidata_logo);
            } else if (wikiCode.contains("commons")) {
                wikiCodeView.setVisibility(View.GONE);
                wikiCodeBackgroundView.setVisibility(View.GONE);
                wikiCodeImageView.setVisibility(View.VISIBLE);
                wikiCodeImageView.setImageResource(R.drawable.ic_commons_logo);
            } else {
                wikiCodeBackgroundView.setVisibility(View.VISIBLE);
                wikiCodeView.setVisibility(View.VISIBLE);
                wikiCodeImageView.setVisibility(View.GONE);
                wikiCodeView.setText(n.wiki().replace("wiki", ""));
            }

            if (container.selected) {
                imageSelectedView.setVisibility(View.VISIBLE);
                imageContainerView.setVisibility(View.INVISIBLE);
                itemView.setBackgroundColor(getThemedColor(NotificationActivity.this, R.attr.multi_select_background_color));
            } else {
                imageSelectedView.setVisibility(View.INVISIBLE);
                imageContainerView.setVisibility(View.VISIBLE);
                itemView.setBackgroundColor(getThemedColor(NotificationActivity.this, R.attr.paper_color));
            }
        }

        @Override public void onClick(View v) {
            if (MultiSelectCallback.is(actionMode)) {
                toggleSelectItem(container);
            } else {
                bottomSheetPresenter.show(getSupportFragmentManager(),
                        NotificationItemActionsDialog.newInstance(container.notification));
            }
        }

        @Override public boolean onLongClick(View v) {
            beginMultiSelect();
            toggleSelectItem(container);
            return true;
        }
    }

    private class NotificationItemHolderSwipeable extends NotificationItemHolder
            implements SwipeableItemTouchHelperCallback.Callback {
        NotificationItemHolderSwipeable(View v) {
            super(v);
        }

        @Override public void onSwipe() {
            deleteItems(Collections.singletonList(getContainer()), false);
        }
    }

    private class NotificationDateHolder extends RecyclerView.ViewHolder {
        private TextView dateView;

        NotificationDateHolder(View view) {
            super(view);
            dateView = view.findViewById(R.id.notification_date_text);
        }

        void bindItem(Date date) {
            dateView.setText(DateUtil.getFeedCardDateString(date));
        }
    }

    private final class NotificationItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @Override
        public int getItemCount() {
            return notificationContainerList.size();
        }

        @Override
        public int getItemViewType(int position) {
            return notificationContainerList.get(position).type;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            if (type == NotificationListItemContainer.ITEM_DATE_HEADER) {
                return new NotificationDateHolder(getLayoutInflater().inflate(R.layout.item_notification_date, parent, false));
            }
            if (displayArchived) {
                return new NotificationItemHolder(getLayoutInflater().inflate(R.layout.item_notification, parent, false));
            } else {
                return new NotificationItemHolderSwipeable(getLayoutInflater().inflate(R.layout.item_notification, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
            if (holder instanceof NotificationDateHolder) {
                ((NotificationDateHolder) holder).bindItem(notificationContainerList.get(pos).date);
            } else if (holder instanceof NotificationItemHolderSwipeable) {
                ((NotificationItemHolderSwipeable) holder).bindItem(notificationContainerList.get(pos));
            } else if (holder instanceof NotificationItemHolder) {
                ((NotificationItemHolder) holder).bindItem(notificationContainerList.get(pos));
            }

            // if we're at the bottom of the list, and we have a continuation string, then execute it.
            if (pos == notificationContainerList.size() - 1 && !TextUtils.isEmpty(currentContinueStr)) {
                getOrContinueNotifications();
            }
        }
    }

    private class SearchCallback extends SearchActionModeCallback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            actionMode = mode;
            return super.onCreateActionMode(mode, menu);
        }

        @Override
        protected void onQueryChange(String s) {
            currentSearchQuery = s.trim();
            postprocessAndDisplay();
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            super.onDestroyActionMode(mode);
            actionMode = null;
            currentSearchQuery = null;
            postprocessAndDisplay();
        }

        @Override
        protected String getSearchHintString() {
            return getString(R.string.notifications_search);
        }

        @Override
        protected boolean finishActionModeIfKeyboardHiding() {
            return true;
        }
    }

    private class MultiSelectCallback extends MultiSelectActionModeCallback {
        @Override public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            super.onCreateActionMode(mode, menu);
            mode.getMenuInflater().inflate(R.menu.menu_action_mode_notifications, menu);
            menu.findItem(R.id.menu_delete_selected).setVisible(!displayArchived);
            menu.findItem(R.id.menu_unarchive_selected).setVisible(displayArchived);
            actionMode = mode;
            return true;
        }

        @Override public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_delete_selected:
                case R.id.menu_unarchive_selected:
                    onDeleteSelected();
                    finishActionMode();
                    return true;
                default:
            }
            return false;
        }

        @Override protected void onDeleteSelected() {
            deleteItems(getSelectedItems(), displayArchived);
        }

        @Override public void onDestroyActionMode(ActionMode mode) {
            unselectAllItems();
            actionMode = null;
            super.onDestroyActionMode(mode);
        }
    }

    private static class NotificationListItemContainer {
        public static final int ITEM_DATE_HEADER = 0;
        public static final int ITEM_NOTIFICATION = 1;

        private final int type;
        private Notification notification;
        private Date date;
        private boolean selected;

        NotificationListItemContainer(@NonNull Date date) {
            this.date = date;
            type = ITEM_DATE_HEADER;
        }

        NotificationListItemContainer(@NonNull Notification notification) {
            this.notification = notification;
            type = ITEM_NOTIFICATION;
        }
    }
}
