package org.wikipedia.page;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import org.wikipedia.R;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;

public class FindInPageFragment extends Fragment {

    private PageActivity parentActivity;

    private View findInPageContainer;
    private EditText findInPageInput;
    private View findInPageNext;
    private View findInPagePrev;
    private View findInPageClose;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_find_in_page, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        parentActivity = (PageActivity)getActivity();

        findInPageContainer = getView().findViewById(R.id.find_in_page_container);

        findInPageNext = getView().findViewById(R.id.find_in_page_next);
        findInPageNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideSoftKeyboard(parentActivity);
                if (!pageFragmentValid()) {
                    return;
                }
                parentActivity.getCurPageFragment().getWebView().findNext(true);
            }
        });

        findInPagePrev = getView().findViewById(R.id.find_in_page_prev);
        findInPagePrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.hideSoftKeyboard(parentActivity);
                if (!pageFragmentValid()) {
                    return;
                }
                parentActivity.getCurPageFragment().getWebView().findNext(false);
            }
        });

        findInPageClose = getView().findViewById(R.id.find_in_page_close);
        findInPageClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide();
            }
        });

        findInPageInput = (EditText) getView().findViewById(R.id.find_in_page_input);
        findInPageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                findInPageNext.setEnabled(s.length() > 0);
                findInPagePrev.setEnabled(s.length() > 0);
                if (!pageFragmentValid()) {
                    return;
                }
                if (s.length() > 0) {
                    findInPage(s.toString());
                } else{
                    parentActivity.getCurPageFragment().getWebView().clearMatches();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey("findinpage_text")) {
            findInPageInput.setText(savedInstanceState.getString("findinpage_text"));
        }
    }

    private boolean pageFragmentValid() {
        if (parentActivity.getCurPageFragment() == null) {
            // could happen when we restore state
            return false;
        }
        if (parentActivity.getCurPageFragment().getWebView() == null) {
            // fragment instantiated, but not yet bound to activity
            return false;
        }
        return true;
    }

    public void hide() {
        if (findInPageContainer != null && findInPageContainer.getVisibility() == View.VISIBLE) {
            parentActivity.getCurPageFragment().getWebView().clearMatches();
            Utils.hideSoftKeyboard(parentActivity);
            ViewAnimations.fadeOut(findInPageContainer);
        }
    }

    public void show() {
        if (findInPageContainer != null && findInPageContainer.getVisibility() != View.VISIBLE) {
            ViewAnimations.fadeIn(findInPageContainer, new Runnable() {
                @Override
                public void run() {
                    // give focus to the input field, and force the keyboard to be shown
                    findInPageInput.requestFocus();
                    ((InputMethodManager) parentActivity.getSystemService(Context.INPUT_METHOD_SERVICE))
                            .toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
                    // if the input field is already populated, then search it
                    if (findInPageInput.getText().length() > 0) {
                        if (!pageFragmentValid()) {
                            return;
                        }
                        findInPage(findInPageInput.getText().toString());
                    }
                }
            });
        }
    }

    public void clear() {
        if (findInPageContainer != null) {
            findInPageInput.setText("");
        }
    }

    public boolean handleBackPressed() {
        if (findInPageContainer.getVisibility() == View.VISIBLE) {
            hide();
            return true;
        }
        return false;
    }

    public void findInPage(String s) {
        // to make it stop complaining
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            parentActivity.getCurPageFragment().getWebView().findAllAsync(s);
        } else {
            parentActivity.getCurPageFragment().getWebView().findAll(s);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("findinpage_text", findInPageInput.getText().toString());
    }

}
