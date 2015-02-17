package org.wikipedia.pagehistory.usercontributions;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.wikipedia.R;
import org.wikipedia.ThemedActionBarActivity;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;
import org.wikipedia.pagehistory.PageHistoryItem;

import java.util.ArrayList;

public class UserContribsActivity extends ThemedActionBarActivity {
    private static final int NUMBER_TO_FETCH = 24;

    private View moreContainer;
    private TextView moreText;
    private ProgressBar moreProgress;

    private String lastContinue = null;
    private ArrayList<PageHistoryItem> contribs = new ArrayList<PageHistoryItem>();

    private UserContribsAdapter adapter;

    private WikipediaApp app;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_contribs);

        app = (WikipediaApp)getApplicationContext();

        moreContainer = getLayoutInflater().inflate(R.layout.group_load_more, null, false);
        moreText = (TextView) moreContainer.findViewById(R.id.load_more_text);
        moreProgress = (ProgressBar) moreContainer.findViewById(R.id.load_more_progress);

        moreContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchMore();
            }
        });

        adapter = new UserContribsAdapter();

        ListView userContribsList = (ListView) findViewById(R.id.user_contribs_list);
        userContribsList.setAdapter(adapter);
        userContribsList.addFooterView(moreContainer);

        if (savedInstanceState != null) {
            lastContinue = savedInstanceState.getString("lastContinue");
            contribs = savedInstanceState.getParcelableArrayList("contribs");
            adapter.notifyDataSetChanged();
        } else {
            fetchMore();
        }
    }

    private boolean isFetching = false;
    private void fetchMore() {
        if (isFetching) {
            return;
        }
        new FetchUserContribsTask(this, app.getPrimarySite(), app.getUserInfoStorage().getUser().getUsername(), NUMBER_TO_FETCH, lastContinue) {
            @Override
            public void onBeforeExecute() {
                isFetching = true;
                moreProgress.setVisibility(View.VISIBLE);
                moreText.setVisibility(View.GONE);
            }

            @Override
            public void onFinish(UserContributionsList result) {
                lastContinue = result.getQueryContinue();
                contribs.addAll(result.getContribs());
                adapter.notifyDataSetChanged();
                isFetching = false;

                moreProgress.setVisibility(View.GONE);
                moreText.setVisibility(View.VISIBLE);

                if (lastContinue == null) {
                    // We got no continue back, so that means we have loaded all the things!
                    moreContainer.setVisibility(View.GONE);
                }
            }
        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("lastContinue", lastContinue);
        outState.putParcelableArrayList("contribs", contribs);
    }

    private class UserContribsAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            return contribs.size();
        }

        @Override
        public Object getItem(int i) {
            return contribs.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_usercontribs_entry, parent, false);
            }

            TextView titleText = (TextView) convertView.findViewById(R.id.user_contrib_item_page_name);
            TextView summaryText = (TextView) convertView.findViewById(R.id.user_contrib_item_edit_summary);
            TextView timeAgoText = (TextView) convertView.findViewById(R.id.user_contrib_item_time_ago);

            PageHistoryItem item = (PageHistoryItem) getItem(pos);
            titleText.setText(item.getTitle().getDisplayText());
            summaryText.setText(item.getSummary());
            timeAgoText.setText(Utils.formatDateRelative(item.getTimestamp()));

            if (summaryText.getText().length() == 0) {
                summaryText.setVisibility(View.GONE);
            } else {
                summaryText.setVisibility(View.VISIBLE);
            }

            return convertView;
        }
    }
}