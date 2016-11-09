package org.wikipedia.descriptions;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DeviceUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import retrofit2.Call;

public class DescriptionEditFragment extends Fragment {
    private static final String ARG_TITLE = "title";

    @BindView(R.id.fragment_description_edit_view) DescriptionEditView editView;
    private Unbinder unbinder;
    private PageTitle pageTitle;
    @Nullable private Call<DescriptionEdit> call;

    @NonNull
    public static DescriptionEditFragment newInstance(@NonNull PageTitle title) {
        DescriptionEditFragment instance = new DescriptionEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, GsonMarshaller.marshal(title));
        instance.setArguments(args);
        return instance;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = GsonUnmarshaller.unmarshal(PageTitle.class, getArguments().getString(ARG_TITLE));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_description_edit, container, false);
        unbinder = ButterKnife.bind(this, view);

        editView.setPageTitle(pageTitle);
        editView.setCallback(new EditViewCallback());
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override public void onDestroyView() {
        editView.setCallback(null);
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override public void onDestroy() {
        if (call != null) {
            call.cancel();
            call = null;
        }
        pageTitle = null;
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_description_edit, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_description_edit_help:
                // TODO: show tutorial!
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class EditViewCallback implements DescriptionEditView.Callback {
        @Override
        public void onSaveClick() {
            editView.setError(null);
            editView.setSaveState(true);
            if (call != null) {
                call.cancel();
            }
            call = new DescriptionEditClient().request(pageTitle, editView.getDescription(),
                    new DescriptionEditClient.Callback() {
                        @Override
                        public void success(@NonNull Call<DescriptionEdit> call) {
                            if (getActivity() != null) {
                                DeviceUtil.hideSoftKeyboard(getActivity());
                                // TODO: go to success fragment
                                getActivity().finish();
                            }
                        }

                        @Override
                        public void failure(@NonNull Call<DescriptionEdit> call,
                                            @NonNull Throwable caught) {
                            editView.setSaveState(false);
                            editView.setError(caught.getMessage());
                        }
                    });
        }
    }
}