package org.wikimedia.wikipedia;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.wikimedia.wikipedia.history.HistoryActivity;

public class NavDrawerFragment extends Fragment implements View.OnClickListener {
    private static final int[] ACTION_ITEMS = {
            R.id.nav_item_history
    };
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.fragment_navdrawer, container, false);
        for (int id: ACTION_ITEMS) {
            parentView.findViewById(id).setOnClickListener(this);
        }
        return parentView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nav_item_history:
                Intent intent = new Intent();
                intent.setClass(this.getActivity(), HistoryActivity.class);
                getActivity().startActivity(intent);
                break;
            default:
                throw new RuntimeException("Unknown ID clicked!");
        }
    }
}