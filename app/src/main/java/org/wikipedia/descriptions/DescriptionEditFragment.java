package org.wikipedia.descriptions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageTitle;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class DescriptionEditFragment extends Fragment {
    private static final String EXTRA_TITLE = "title";

    @BindView(R.id.description_edit_page_title) TextView pageTitleText;
    private Unbinder unbinder;

    private PageTitle pageTitle;

    @NonNull
    public static DescriptionEditFragment newInstance(@NonNull PageTitle title) {
        DescriptionEditFragment instance = new DescriptionEditFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_TITLE, GsonMarshaller.marshal(title));
        instance.setArguments(args);
        return instance;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_description_edit, container, false);
        unbinder = ButterKnife.bind(this, view);

        pageTitle = GsonUnmarshaller.unmarshal(PageTitle.class, getActivity().getIntent().getStringExtra(EXTRA_TITLE));

        pageTitleText.setText(pageTitle.getDisplayText());
        return view;
    }

    @Override public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }
}
