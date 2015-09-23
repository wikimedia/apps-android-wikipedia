package org.wikipedia.page.tabs;

import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageBackStackItem;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ApiUtil;

import com.squareup.picasso.Picasso;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

public class TabsProvider {

    private PageActivity parentActivity;
    private float displayDensity;

    private View pageContentView;
    private View tabContainerView;
    private ListView tabListView;
    private TabListAdapter tabListAdapter;

    private ActionMode tabActionMode;
    // True when action mode is terminated by the side effect of creating a new tab or selecting an
    // existing tab.
    private boolean isActionModeDismissedIndirectly;

    private List<Tab> tabList;

    public interface TabsProviderListener {
        void onEnterTabView();
        void onCancelTabView();
        void onTabSelected(int position);
        void onNewTabRequested();
        void onCloseTabRequested(int position);
    }

    @NonNull
    private TabsProviderListener providerListener = new DefaultTabsProviderListener();
    public void setTabsProviderListener(TabsProviderListener listener) {
        providerListener = DefaultTabsProviderListener.defaultIfNull(listener);
    }

    public TabsProvider(PageActivity parentActivity, List<Tab> tabList) {
        this.parentActivity = parentActivity;
        this.tabList = tabList;
        displayDensity = parentActivity.getResources().getDisplayMetrics().density;

        pageContentView = parentActivity.getContentView();
        tabContainerView = parentActivity.getTabsContainerView();
        tabListView = (ListView) tabContainerView.findViewById(R.id.tabs_list);
        tabListAdapter = new TabListAdapter(parentActivity.getLayoutInflater());
        tabListView.setAdapter(tabListAdapter);

        tabContainerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                providerListener.onCancelTabView();
            }
        });

        tabListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                isActionModeDismissedIndirectly = true;
                providerListener.onTabSelected(position);
            }
        });

    }

    public boolean onBackPressed() {
        if (tabActionMode != null) {
            exitTabMode();
            providerListener.onCancelTabView();
            return true;
        }
        return false;
    }

    public void enterTabMode() {
        enterTabMode(null);
        providerListener.onEnterTabView();
    }

    private void enterTabMode(@Nullable Runnable onTabModeEntered) {
        if (tabActionMode != null) {
            // already inside action mode...
            // but make sure to update the list of tabs.
            tabListAdapter.notifyDataSetInvalidated();
            tabListView.smoothScrollToPosition(tabList.size() - 1);
            if (onTabModeEntered != null) {
                onTabModeEntered.run();
            }
            return;
        }
        parentActivity.startSupportActionMode(new TabActionModeCallback(onTabModeEntered));

    }

    private class TabActionModeCallback implements ActionMode.Callback {
        private static final String TAB_ACTION_MODE_TAG = "actionModeTabList";
        private final Runnable onTabModeEntered;

        public TabActionModeCallback(Runnable onTabModeEntered) {
            this.onTabModeEntered = onTabModeEntered;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            tabActionMode = mode;
            mode.getMenuInflater().inflate(R.menu.menu_tabs, menu);
            Animation anim = AnimationUtils.loadAnimation(parentActivity,
                                                          R.anim.tab_list_zoom_enter);
            parentActivity.getContentView().startAnimation(anim);
            layoutTabList(onTabModeEntered);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mode.setTag(TAB_ACTION_MODE_TAG);
            // find the action mode base view, and give it an empty click listener,
            // otherwise click events within the empty area of the action mode will be passed
            // down to the view beneath it, which is the Search bar, and we don't want to
            // unintentionally initiate Search.
            parentActivity.findViewById(R.id.action_mode_bar).setClickable(true);
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_new_tab:
                    providerListener.onNewTabRequested();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Animation anim = AnimationUtils.loadAnimation(parentActivity, R.anim.tab_list_zoom_exit);
            parentActivity.getContentView().startAnimation(anim);
            hideTabList();
            tabActionMode = null;
            parentActivity.showToolbar();
            if (!isActionModeDismissedIndirectly) {
                providerListener.onCancelTabView();
            }
            isActionModeDismissedIndirectly = false;
        }
    }

    public void exitTabMode() {
        if (tabActionMode != null) {
            tabActionMode.finish();
        }
    }

    public void showAndHideTabs() {
        enterTabMode(new Runnable() {
            private final int animDelay = 500;
            @Override
            public void run() {
                tabContainerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isActionModeDismissedIndirectly = true;
                        exitTabMode();
                    }
                }, animDelay);
            }
        });
    }

    private void layoutTabList(final Runnable onTabModeEntered) {
        tabContainerView.setVisibility(View.VISIBLE);
        tabListAdapter.notifyDataSetInvalidated();

        // size the listview to be the same width as the scaled-down webview...
        final float proportionHorz = 0.15f;
        final float proportionVert = 0.4f;
        final int heightOffset = 16;
        int contentOffset = Utils.getContentTopOffsetPx(parentActivity);
        int maxHeight = (int) (pageContentView.getHeight() * proportionVert
                               + pageContentView.getHeight() * proportionHorz
                               - contentOffset - heightOffset * displayDensity);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxHeight);
        float margin = pageContentView.getWidth() * proportionHorz / 2f;
        if (ApiUtil.hasHoneyComb()) {
            params.leftMargin = (int) margin;
            params.rightMargin = (int) margin;
            params.topMargin = contentOffset;
        } else {
            // for 2.3, use padding instead of margin, for some reason.
            tabListView.setPadding((int) margin, contentOffset, (int) margin, 0);
            params.height += contentOffset;
            params.leftMargin = 0;
            params.rightMargin = 0;
            params.topMargin = 0;
        }
        tabListView.setLayoutParams(params);

        Animation anim = AnimationUtils.loadAnimation(parentActivity,
                                                      R.anim.tab_list_items_enter);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (onTabModeEntered != null) {
                    onTabModeEntered.run();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        tabListView.startAnimation(anim);
        // scroll to the bottom of the tab list
        tabListView.smoothScrollToPosition(tabList.size() - 1);
    }

    private void hideTabList() {
        Animation anim = AnimationUtils.loadAnimation(parentActivity,
                                                      R.anim.tab_list_items_exit);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                tabContainerView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        tabListView.startAnimation(anim);
    }

    public void invalidate() {
        tabListAdapter.notifyDataSetInvalidated();
    }

    private View.OnClickListener onItemCloseButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ViewHolder holder = (ViewHolder) v.getTag();
            final int position = holder.position;
            // if we're closing the last tab, don't do the animation...
            if (tabList.size() == 1) {
                providerListener.onCloseTabRequested(position);
            } else {
                Animation anim = AnimationUtils
                        .loadAnimation(parentActivity, R.anim.slide_out_right);
                    anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override public void onAnimationStart(Animation animation) { }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        providerListener.onCloseTabRequested(position);
                    }

                    @Override public void onAnimationRepeat(Animation animation) { }
                });
                holder.container.startAnimation(anim);
            }
        }
    };


    private static class DefaultTabsProviderListener implements TabsProviderListener {
        public static TabsProviderListener defaultIfNull(TabsProviderListener listener) {
            return listener == null ? new DefaultTabsProviderListener() : listener;
        }

        @Override public void onEnterTabView() { }
        @Override public void onCancelTabView() { }
        @Override public void onTabSelected(int position) { }
        @Override public void onNewTabRequested() { }
        @Override public void onCloseTabRequested(int position) { }
    }

    private static class ViewHolder {
        private View container;
        private TextView title;
        private ImageView thumbnail;
        private View gradient;
        private View closeButton;
        private int position;
    }

    private final class TabListAdapter extends BaseAdapter {
        private final LayoutInflater inflater;

        private TabListAdapter(LayoutInflater inflater) {
            this.inflater = inflater;
        }

        @Override
        public int getCount() {
            return tabList.size();
        }

        @Override
        public Object getItem(int position) {
            return tabList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = inflater.inflate(R.layout.item_tab_entry, parent, false);
                convertView.setTag(viewHolder);
                viewHolder.container = convertView;
                viewHolder.title = (TextView) convertView.findViewById(R.id.tab_item_title);
                viewHolder.thumbnail = (ImageView) convertView.findViewById(R.id.tab_item_thumbnail);
                viewHolder.gradient = convertView.findViewById(R.id.tab_item_bottom_gradient);
                viewHolder.closeButton = convertView.findViewById(R.id.tab_item_close);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            viewHolder.position = position;
            viewHolder.closeButton.setTag(viewHolder);
            viewHolder.closeButton.setOnClickListener(onItemCloseButtonListener);
            // hide the shadow if this is the topmost tab
            viewHolder.gradient.setVisibility(
                    position == tabList.size() - 1 ? View.GONE : View.VISIBLE);

            // dynamically set the background color that will show through the rounded corners.
            // if it's the first last item in the tab list, we want the background to be the same
            // as the activity background, otherwise it should match the tab shadow color.
            convertView.setBackgroundColor(parentActivity.getResources().getColor(
                    position == 0
                            ? R.color.gallery_background
                            : Utils.getThemedAttributeId(parentActivity, R.attr.tab_shadow_color)));

            List<PageBackStackItem> backstack = tabList.get(position).getBackStack();
            if (backstack.size() > 0) {
                PageTitle title = backstack.get(backstack.size() - 1).getTitle();
                viewHolder.title.setText(title.getDisplayText());
                String thumbnail = title.getThumbUrl();
                if (WikipediaApp.getInstance().isImageDownloadEnabled() && thumbnail != null) {
                    Picasso.with(parentActivity)
                           .load(thumbnail)
                           .placeholder(R.drawable.ic_pageimage_placeholder)
                           .error(R.drawable.ic_pageimage_placeholder)
                           .noFade()
                           .into(viewHolder.thumbnail);
                } else {
                    Picasso.with(parentActivity)
                           .load(R.drawable.ic_pageimage_placeholder)
                           .into(viewHolder.thumbnail);
                }
            }
            return convertView;
        }
    }
}
