package org.wikipedia.descriptions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DescriptionEditSuccessFragment extends Fragment implements DescriptionEditSuccessView.Callback {
    @BindView(R.id.fragment_description_edit_success_view) DescriptionEditSuccessView successView;
    private Unbinder unbinder;

    public interface Callback {
        void onDismissClick();
    }

    @NonNull public static DescriptionEditSuccessFragment newInstance() {
        return new DescriptionEditSuccessFragment();
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_description_edit_success, container, false);
        unbinder = ButterKnife.bind(this, view);
        successView.setCallback(this);
        return view;
    }

    @Override public void onDismissClick() {
        if (callback() != null) {
            callback().onDismissClick();
        }
    }

    @Override public void onDestroyView() {
        successView.setCallback(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
