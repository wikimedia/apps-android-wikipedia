package org.wikipedia.descriptions;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.util.UriUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DescriptionEditHelpFragment extends Fragment {
    @BindView(R.id.fragment_description_edit_help_view) DescriptionEditHelpView helpView;
    private Unbinder unbinder;

    @NonNull
    public static DescriptionEditHelpFragment newInstance() {
        return new DescriptionEditHelpFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_description_edit_help, container, false);
        unbinder = ButterKnife.bind(this, view);

        helpView.setCallback(new HelpViewCallback());
        return view;
    }

    @Override public void onDestroyView() {
        helpView.setCallback(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private class HelpViewCallback implements DescriptionEditHelpView.Callback {
        @Override
        public void onAboutClick() {
            UriUtil.handleExternalLink(getContext(),
                    Uri.parse(getString(R.string.wikidata_about_url)));
        }

        @Override
        public void onGuideClick() {
            UriUtil.handleExternalLink(getContext(),
                    Uri.parse(getString(R.string.wikidata_description_guide_url)));
        }
    }
}
