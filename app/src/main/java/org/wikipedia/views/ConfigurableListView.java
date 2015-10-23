package org.wikipedia.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.BaseAdapter;
import android.widget.ListView;

import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class ConfigurableListView extends ListView {
    public ConfigurableListView(Context context) {
        super(context);
    }

    public ConfigurableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfigurableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setAdapter(BaseAdapter adapter, String languageCode) {
        super.setAdapter(adapter);
        setLocale(languageCode);
    }

    public void setLocale(String languageCode) {
        setConditionalLayoutDirection(this, languageCode);
    }
}
