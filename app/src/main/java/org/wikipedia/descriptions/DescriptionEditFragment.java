package org.wikipedia.descriptions;

import android.content.Intent;
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

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.edit.token.EditToken;
import org.wikipedia.edit.token.EditTokenClient;
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
    @Nullable private Call<EditToken> editTokenCall;
    @Nullable private Call<DescriptionEdit> descriptionEditCall;

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
        cancelCalls();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS
                && getActivity() != null) {
            getActivity().finish();
        }
    }

    private void cancelCalls() {
        // in reverse chronological order
        if (descriptionEditCall != null) {
            descriptionEditCall.cancel();
            descriptionEditCall = null;
        }
        if (editTokenCall != null) {
            editTokenCall.cancel();
            editTokenCall = null;
        }
    }

    private class EditViewCallback implements DescriptionEditView.Callback {
        private final WikiSite wikiData = new WikiSite("www.wikidata.org", "");

        @Override
        public void onSaveClick() {
            editView.setError(null);
            editView.setSaveState(true);

            cancelCalls();
            requestEditToken();
        }

        /** fetch edit token from Wikidata */
        private void requestEditToken() {
            editTokenCall = new EditTokenClient().request(wikiData,
                    new EditTokenClient.Callback() {
                        @Override public void success(@NonNull Call<EditToken> call,
                                                      @NonNull String editToken) {
                            postDescription(editToken);
                        }

                        @Override public void failure(@NonNull Call<EditToken> call,
                                                      @NonNull Throwable caught) {
                            editFailed(caught);
                        }
                    });
        }

        /* send updated description to Wikidata */
        private void postDescription(@NonNull String editToken) {
            descriptionEditCall = new DescriptionEditClient().request(wikiData, pageTitle,
                    editView.getDescription(), editToken,
                    new DescriptionEditClient.Callback() {
                        @Override public void success(@NonNull Call<DescriptionEdit> call) {
                            if (getActivity() != null) {
                                DeviceUtil.hideSoftKeyboard(getActivity());
                                editView.setSaveState(false);
                                startActivityForResult(DescriptionEditSuccessActivity.newIntent(getContext()),
                                        Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT_SUCCESS);
                            }
                        }

                        @Override public void abusefilter(@NonNull Call<DescriptionEdit> call,
                                                          String info) {
                            editView.setSaveState(false);
                            editView.setError(info);
                        }

                        @Override public void failure(@NonNull Call<DescriptionEdit> call,
                                                      @NonNull Throwable caught) {
                            editFailed(caught);
                        }
                    });
        }

        private void editFailed(@NonNull Throwable caught) {
            if (editView != null) {
                editView.setSaveState(false);
                editView.setError(caught.getMessage());
            }
        }
    }
}