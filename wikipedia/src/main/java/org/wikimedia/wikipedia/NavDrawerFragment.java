package org.wikimedia.wikipedia;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.wikimedia.wikipedia.history.HistoryActivity;

public class NavDrawerFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final int[] ACTION_ITEMS_TEXT = {
            R.string.nav_item_history
    };
    private static final int[] ACTION_ITEM_IMAGES = {
            android.R.drawable.ic_menu_recent_history
    };

    private ListView navList;
    private NavListAdapter adapter;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_navdrawer, container, false);
        navList = (ListView) parentView.findViewById(R.id.nav_list);
        adapter = new NavListAdapter();

        navList.setAdapter(adapter);
        navList.setOnItemClickListener(this);

        return parentView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch ((Integer)view.getTag()) {
            case R.string.nav_item_history:
                Intent intent = new Intent();
                intent.setClass(this.getActivity(), HistoryActivity.class);
                getActivity().startActivity(intent);
                break;
            default:
                throw new RuntimeException("Unknown ID clicked!");
        }
    }

    private class NavListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return ACTION_ITEMS_TEXT.length;
        }

        @Override
        public Object getItem(int position) {
            return ACTION_ITEMS_TEXT[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.item_nav_item, parent, false);
            }
            TextView navText = (TextView)convertView.findViewById(R.id.nav_item_text);
            ImageView navImage = (ImageView)convertView.findViewById(R.id.nav_item_image);
            navText.setText(ACTION_ITEMS_TEXT[position]);
            navImage.setImageResource(ACTION_ITEM_IMAGES[position]);
            convertView.setTag(ACTION_ITEMS_TEXT[position]);

            return convertView;
        }
    }
}