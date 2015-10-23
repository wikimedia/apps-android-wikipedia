package org.wikipedia.editing.richtext;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.EditText;
import org.wikipedia.R;
import org.wikipedia.concurrency.SaneAsyncTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static org.wikipedia.util.ResourceUtil.getThemedAttributeId;

public class SyntaxHighlighter {
    private static String TAG = "SyntaxHighlighter";

    private EditText textBox;
    private List<SyntaxRule> syntaxRules;

    private SyntaxHighlightTask currentTask = null;
    private Handler handler;

    private OnSyntaxHighlightListener syntaxHighlightListener;
    public interface OnSyntaxHighlightListener {
        void syntaxHighlightResults(List<SpanExtents> spanExtents);
    }

    public SyntaxHighlighter(final Activity parentActivity, final EditText textBox) {
        this(parentActivity, textBox, null);
    }

    public SyntaxHighlighter(final Activity parentActivity, final EditText textBox, OnSyntaxHighlightListener listener) {
        this.textBox = textBox;
        this.syntaxHighlightListener = listener;
        syntaxRules = new ArrayList<>();

        // create our list of syntax rules for Wikipedia markup:

        // TODO: for now, only highlight templates. Later on, we'll reevaluate colors/styles for other syntax elements.

        // template:
        syntaxRules.add(new SyntaxRule("{{", "}}", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new ColorSpanEx(parentActivity.getResources().getColor(getThemedAttributeId(parentActivity, R.attr.syntax_highlight_template_color)),
                                       Color.TRANSPARENT, spanStart, syntaxItem);
            }
        }));

        // internal link:
        syntaxRules.add(new SyntaxRule("[[", "]]", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new ColorSpanEx(parentActivity.getResources().getColor(getThemedAttributeId(parentActivity, R.attr.link_color)),
                                                 Color.TRANSPARENT, spanStart, syntaxItem);
            }
        }));

        // external link:
        syntaxRules.add(new SyntaxRule("[", "]", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new ColorSpanEx(parentActivity.getResources().getColor(getThemedAttributeId(parentActivity, R.attr.link_color)),
                                                 Color.TRANSPARENT, spanStart, syntaxItem);
            }
        }));

        // ref:
        syntaxRules.add(new SyntaxRule("<", ">", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new ColorSpanEx(parentActivity.getResources().getColor(R.color.syntax_highlight_htmltag),
                                                 Color.TRANSPARENT, spanStart, syntaxItem);
            }
        }));

        // bold italic:
        syntaxRules.add(new SyntaxRule("'''''", "'''''", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new StyleSpanEx(Typeface.BOLD_ITALIC, spanStart, syntaxItem);
            }
        }));

        // bold:
        syntaxRules.add(new SyntaxRule("'''", "'''", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new StyleSpanEx(Typeface.BOLD, spanStart, syntaxItem);
            }
        }));

        // italic:
        syntaxRules.add(new SyntaxRule("''", "''", new SyntaxRule.SyntaxRuleStyle() {
            @Override
            public SpanExtents createSpan(int spanStart, SyntaxRule syntaxItem) {
                return new StyleSpanEx(Typeface.ITALIC, spanStart, syntaxItem);
            }
        }));

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
                // queue up syntax highlighting.
                // if the user adds more text within 1/2 second, the previous request
                // is cancelled, and a new one is placed.
                handler.removeCallbacks(syntaxHighlightCallback);
                handler.postDelayed(syntaxHighlightCallback, DateUtils.SECOND_IN_MILLIS / 2);
            }
        });

        handler = new Handler(Looper.getMainLooper());
    }

    private Runnable syntaxHighlightCallback = new Runnable() {
        @Override
        public void run() {
            if (currentTask != null) {
                currentTask.cancel();
            }
            currentTask = new SyntaxHighlightTask(textBox.getText());
            currentTask.execute();
        }
    };

    private class SyntaxHighlightTask extends SaneAsyncTask<List<SpanExtents>> {
        SyntaxHighlightTask(Editable text) {
            super(SINGLE_THREAD);
            this.text = text;
        }

        private Editable text;

        @Override
        public List<SpanExtents> performTask() throws Throwable {
            Stack<SpanExtents> spanStack = new Stack<>();
            List<SpanExtents> spansToSet = new ArrayList<>();

            /*
            The (naïve) algorithm:
            Iterate through the text string, and maintain a stack of matched syntax rules.
            When the "start" and "end" symbol of a rule are matched in sequence, create a new
            Span to be added to the EditText at the corresponding location.
             */

            for (int i = 0; i < text.length(); i++) {
                SpanExtents newSpanInfo = null;

                for (SyntaxRule syntaxItem : syntaxRules) {

                    if (i + syntaxItem.getStartSymbol().length() >= text.length()) {
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
                            } else {
                                SpanExtents sp = syntaxItem.getSpanStyle().createSpan(i, syntaxItem);
                                spanStack.push(sp);
                            }
                            i += syntaxItem.getStartSymbol().length() - 1;
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
                            SpanExtents sp = syntaxItem.getSpanStyle().createSpan(i, syntaxItem);
                            spanStack.push(sp);
                            i += syntaxItem.getStartSymbol().length() - 1;
                        }

                        pass = true;
                        for (int j = 0; j < syntaxItem.getEndSymbol().length(); j++) {
                            if (text.charAt(i + j) != syntaxItem.getEndSymbol().charAt(j)) {
                                pass = false;
                                break;
                            }
                        }
                        if (pass) {
                            if (spanStack.size() > 0) {
                                newSpanInfo = spanStack.pop();
                                newSpanInfo.setEnd(i + syntaxItem.getEndSymbol().length());
                            }
                            i += syntaxItem.getEndSymbol().length() - 1;
                        }

                    }
                }

                if (newSpanInfo != null) {
                    spansToSet.add(newSpanInfo);
                }

                if (isCancelled()) {
                    break;
                }
            }

            if (syntaxHighlightListener != null) {
                syntaxHighlightListener.syntaxHighlightResults(spansToSet);
            }
            return spansToSet;
        }

        @Override
        public void onFinish(List<SpanExtents> result) {
            if (isCancelled()) {
                return;
            }
            // TODO: probably possible to make this more efficient...
            // Right now, on longer articles, this is quite heavy on the UI thread.
            // remove any of our custom spans from the previous cycle...
            long time = System.currentTimeMillis();
            Object[] prevSpans = textBox.getText().getSpans(0, text.length(), SpanExtents.class);
            for (Object sp : prevSpans) {
                textBox.getText().removeSpan(sp);
            }
            // and add our new spans
            for (SpanExtents spanEx : result) {
                textBox.getText().setSpan(spanEx, spanEx.getStart(), spanEx.getEnd(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }
            time = System.currentTimeMillis() - time;
            Log.d(TAG, "That took " + time + "ms");
        }

        @Override
        public void onCatch(Throwable caught) {
            Log.d(TAG, caught.getMessage());
        }
    }

}
