package org.wikipedia.database.column;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.StrMatcher;
import org.apache.commons.lang3.text.StrTokenizer;

import java.util.ArrayList;
import java.util.Collection;

// TODO: replace with table constraints when the database layer is more flexible.
public abstract class CsvColumn<T> extends Column<T> {
    public CsvColumn(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        super(tbl, name, type);
    }

    @Override public T val(@NonNull Cursor cursor) {
        return val(split(getString(cursor)));
    }

    public void put(@NonNull ContentValues values, @NonNull T row) {
        values.put(getName(), join(put(row)));
    }

    @NonNull protected abstract T val(@NonNull Collection<String> strs);
    @NonNull protected abstract Collection<String> put(@NonNull T row);

    private String join(@NonNull Collection<String> strs) {
        StringBuilder builder = new StringBuilder();
        for (String str : strs) {
            builder.append(StringEscapeUtils.escapeCsv(str));
            builder.append(',');
        }
        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    @NonNull private Collection<String> split(@Nullable String str) {
        Collection<String> strs = new ArrayList<>();
        for (String escapedStr : tokenizer(str).getTokenList()) {
            strs.add(StringEscapeUtils.unescapeCsv(escapedStr));
        }
        return strs;
    }

    @NonNull private StrTokenizer tokenizer(@Nullable String str) {
        StrTokenizer tokenizer = StrTokenizer.getCSVInstance(str);
        tokenizer.setTrimmerMatcher(StrMatcher.noneMatcher());
        tokenizer.setEmptyTokenAsNull(true);
        return tokenizer;
    }
}
