package org.wikipedia.edit.richtext;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.widget.EditText;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class SyntaxHighlighter {
    public interface OnSyntaxHighlightListener {
        void syntaxHighlightResults(List<SpanExtents> spanExtents);
        void findTextMatches(List<SpanExtents> spanExtents);
    }

    private Context context;
    private EditText textBox;
    private List<SyntaxRule> syntaxRules;
    private String searchText;
    private int selectedMatchResultPosition;

    private Handler handler;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Nullable private OnSyntaxHighlightListener syntaxHighlightListener;

    private Runnable syntaxHighlightCallback = new Runnable() {
        private SyntaxHighlightTask currentTask;
        private SyntaxHighlightSearchMatchesTask searchTask;

        @Override public void run() {
            if (context != null) {
                if (currentTask != null) {
                    currentTask.cancel();
                }
                currentTask = new SyntaxHighlightTask(textBox.getText());
                searchTask = new SyntaxHighlightSearchMatchesTask(textBox.getText(), searchText, selectedMatchResultPosition);
                disposables.clear();
                disposables.add(Observable.zip(Observable.fromCallable(currentTask),
                        Observable.fromCallable(searchTask), (f, s) -> {
                            f.addAll(s);
                            return f;
                        })
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(result -> {
                            if (syntaxHighlightListener != null) {
                                syntaxHighlightListener.syntaxHighlightResults(result);
                            }

                            // TODO: probably possible to make this more efficient...
                            // Right now, on longer articles, this is quite heavy on the UI thread.
                            // remove any of our custom spans from the previous cycle...
                            long time = System.currentTimeMillis();
                            Object[] prevSpans = textBox.getText().getSpans(0, textBox.getText().length(), SpanExtents.class);
                            for (Object sp : prevSpans) {
                                textBox.getText().removeSpan(sp);
                            }

                            List<SpanExtents> findTextList = new ArrayList<>();

                            // and add our new spans
                            for (SpanExtents spanEx : result) {
                                textBox.getText().setSpan(spanEx, spanEx.getStart(), spanEx.getEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                                if (spanEx.getSyntaxRule().getSpanStyle().equals(SyntaxRuleStyle.SEARCH_MATCHES)) {
                                    findTextList.add(spanEx);
                                }
                            }

                            if ((searchText != null && searchText.length() > 0) && syntaxHighlightListener != null) {
                                syntaxHighlightListener.findTextMatches(findTextList);
                            }
                            time = System.currentTimeMillis() - time;
                            L.d("That took " + time + "ms");
                        }, L::e));
            }
        }
    };

    public SyntaxHighlighter(Context context, EditText textBox) {
        this(context, textBox, null);
    }

    public SyntaxHighlighter(Context parentContext, EditText textBox, @Nullable OnSyntaxHighlightListener listener) {
        this.context = parentContext;
        this.textBox = textBox;
        this.syntaxHighlightListener = listener;
        syntaxRules = new ArrayList<>();

        // create our list of syntax rules for Wikipedia markup:

        syntaxRules.add(new SyntaxRule("{{", "}}", SyntaxRuleStyle.TEMPLATE));
        syntaxRules.add(new SyntaxRule("[[", "]]", SyntaxRuleStyle.INTERNAL_LINK));
        syntaxRules.add(new SyntaxRule("[", "]", SyntaxRuleStyle.EXTERNAL_LINK));
        syntaxRules.add(new SyntaxRule("<", ">", SyntaxRuleStyle.REF));
        syntaxRules.add(new SyntaxRule("'''''", "'''''", SyntaxRuleStyle.BOLD_ITALIC));
        syntaxRules.add(new SyntaxRule("'''", "'''", SyntaxRuleStyle.BOLD));
        syntaxRules.add(new SyntaxRule("''", "''", SyntaxRuleStyle.ITALIC));

        // TODO: reevaluate colors/styles for other syntax elements.

        /*
        // section level 4:
        syntaxRules.add(new SyntaxRule("====", "====", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new ColorSpanEx(parentActivity.getResources().getColor(R.color.syntax_highlight_sectiontext),
                                                 parentActivity.getResources().getColor(R.color.syntax_highlight_sectionbgd),
                                                 spanStart, syntaxItem);
            }
        }));

        // section level 3:
        syntaxRules.add(new SyntaxRule("===", "===", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new ColorSpanEx(parentActivity.getResources().getColor(R.color.syntax_highlight_sectiontext),
                                                 parentActivity.getResources().getColor(R.color.syntax_highlight_sectionbgd),
                                                 spanStart, syntaxItem);
            }
        }));

        // section level 2:
        syntaxRules.add(new SyntaxRule("==", "==", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new ColorSpanEx(parentActivity.getResources().getColor(R.color.syntax_highlight_sectiontext),
                                                 parentActivity.getResources().getColor(R.color.syntax_highlight_sectionbgd),
                                                 spanStart, syntaxItem);
            }
        }));
        */

        handler = new Handler(Looper.getMainLooper());

        // add a text-change listener that will trigger syntax highlighting
        // whenever text is modified.
        textBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }
            @Override
            public void afterTextChanged(final Editable editable) {
                postHighlightCallback();
            }
        });
    }

    public void applyFindTextSyntax(@Nullable String searchText, @Nullable OnSyntaxHighlightListener listener) {
        this.searchText = searchText;
        this.syntaxHighlightListener = listener;
        setSelectedMatchResultPosition(0);
        postHighlightCallback();
    }

    public void setSelectedMatchResultPosition(int selectedMatchResultPosition) {
        this.selectedMatchResultPosition = selectedMatchResultPosition;
        postHighlightCallback();
    }

    private void postHighlightCallback() {
        // queue up syntax highlighting.
        // if the user adds more text within 1/2 second, the previous request
        // is cancelled, and a new one is placed.
        handler.removeCallbacks(syntaxHighlightCallback);
        handler.postDelayed(syntaxHighlightCallback, TextUtils.isEmpty(searchText) ? DateUtils.SECOND_IN_MILLIS / 2 : 0);
    }

    public void cleanup() {
        if (context != null) {
            handler.removeCallbacks(syntaxHighlightCallback);
            textBox.getText().clearSpans();
            textBox = null;
            context = null;
        }
        disposables.clear();
    }

    private class SyntaxHighlightTask implements Callable<List<SpanExtents>> {
        SyntaxHighlightTask(Editable text) {
            this.text = text;
        }

        private Editable text;
        private boolean cancelled;

        public void cancel() {
            cancelled = true;
        }

        @Override
        public List<SpanExtents> call() {
            Stack<SpanExtents> spanStack = new Stack<>();
            List<SpanExtents> spansToSet = new ArrayList<>();

            /*
            The (naïve) algorithm:
            Iterate through the text string, and maintain a stack of matched syntax rules.
            When the "start" and "end" symbol of a rule are matched in sequence, create a new
            Span to be added to the EditText at the corresponding location.
             */

            for (int i = 0; i < text.length();) {
                SpanExtents newSpanInfo;
                boolean incrementDone = false;

                for (SyntaxRule syntaxItem : syntaxRules) {

                    if (i + syntaxItem.getStartSymbol().length() > text.length()) {
                        continue;
                    }

                    if (syntaxItem.isStartEndSame()) {
                        boolean pass = true;
                        for (int j = 0; j < syntaxItem.getStartSymbol().length(); j++) {
                            if (text.charAt(i + j) != syntaxItem.getStartSymbol().charAt(j)) {
                                pass = false;
                                break;
                            }
                        }
                        if (pass) {
                            if (spanStack.size() > 0 && spanStack.peek().getSyntaxRule().equals(syntaxItem)) {
                                newSpanInfo = spanStack.pop();
                                newSpanInfo.setEnd(i + syntaxItem.getStartSymbol().length());
                                spansToSet.add(newSpanInfo);
                            } else {
                                SpanExtents sp = syntaxItem.getSpanStyle().createSpan(context, i, syntaxItem);
                                spanStack.push(sp);
                            }
                            i += syntaxItem.getStartSymbol().length();
                            incrementDone = true;
                        }
                    } else {

                        boolean pass = true;
                        for (int j = 0; j < syntaxItem.getStartSymbol().length(); j++) {
                            if (text.charAt(i + j) != syntaxItem.getStartSymbol().charAt(j)) {
                                pass = false;
                                break;
                            }
                        }
                        if (pass) {
                            SpanExtents sp = syntaxItem.getSpanStyle().createSpan(context, i, syntaxItem);
                            spanStack.push(sp);
                            i += syntaxItem.getStartSymbol().length();
                            incrementDone = true;
                        }
                        //skip the check of end symbol when start symbol is found at end of the text
                        if (i + syntaxItem.getStartSymbol().length() > text.length()) {
                            continue;
                        }

                        pass = true;
                        for (int j = 0; j < syntaxItem.getEndSymbol().length(); j++) {
                            if (text.charAt(i + j) != syntaxItem.getEndSymbol().charAt(j)) {
                                pass = false;
                                break;
                            }
                        }
                        if (pass) {
                            if (spanStack.size() > 0 && spanStack.peek().getSyntaxRule().equals(syntaxItem)) {
                                newSpanInfo = spanStack.pop();
                                newSpanInfo.setEnd(i + syntaxItem.getEndSymbol().length());
                                spansToSet.add(newSpanInfo);
                            }
                            i += syntaxItem.getEndSymbol().length();
                            incrementDone = true;
                        }

                    }
                }

                if (cancelled) {
                    break;
                }

                if (!incrementDone) {
                    i++;
                }
            }

            return spansToSet;
        }
    }

    private class SyntaxHighlightSearchMatchesTask implements Callable<List<SpanExtents>> {
        SyntaxHighlightSearchMatchesTask(Editable text, String searchText, int selectedMatchResultPosition) {
            this.text = StringUtils.lowerCase(text.toString());
            this.searchText = StringUtils.lowerCase(searchText);
            this.selectedMatchResultPosition = selectedMatchResultPosition;
        }

        private String searchText;
        private int selectedMatchResultPosition;
        private String text;
        private boolean cancelled;

        public void cancel() {
            cancelled = true;
        }

        @Override
        public List<SpanExtents>  call() {
            List<SpanExtents> spansToSet = new ArrayList<>();
            if (TextUtils.isEmpty(searchText)) {
                return spansToSet;
            }

            SyntaxRule syntaxItem = new SyntaxRule("", "", SyntaxRuleStyle.SEARCH_MATCHES);
            int position = 0;
            int matches = 0;
            do {
                position = text.indexOf(searchText, position);
                if (position >= 0) {
                    SpanExtents newSpanInfo;
                    if (matches == selectedMatchResultPosition) {
                        newSpanInfo = SyntaxRuleStyle.SEARCH_MATCH_SELECTED.createSpan(context, position, syntaxItem);
                    } else {
                        newSpanInfo = SyntaxRuleStyle.SEARCH_MATCHES.createSpan(context, position, syntaxItem);
                    }
                    newSpanInfo.setStart(position);
                    newSpanInfo.setEnd(position + searchText.length());
                    spansToSet.add(newSpanInfo);
                    position += searchText.length();
                    matches++;
                }
                if (cancelled) {
                    break;
                }
            } while(position >= 0);

            return spansToSet;
        }
    }
}
