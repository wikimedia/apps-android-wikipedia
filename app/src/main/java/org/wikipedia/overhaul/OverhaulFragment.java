package org.wikipedia.overhaul;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.activity.CallbackFragment;
import org.wikipedia.activity.FragmentUtil;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public class OverhaulFragment extends Fragment implements CallbackFragment {
    public interface Callback extends CallbackFragment.Callback {
    }

    private Unbinder unbinder;

    public static OverhaulFragment newInstance() {
        return new OverhaulFragment();
    }

    @Nullable @Override public View onCreateView(LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_overhaul, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Nullable @Override public Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}