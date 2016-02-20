package org.wikipedia.database;

import java.util.ArrayList;
import java.util.Collection;

public final class DbUtil {
    public static Collection<String> names(Collection<? extends DatabaseTable.Column> cols) {
        Collection<String> strs = new ArrayList<>(cols.size());
        for (DatabaseTable.Column col : cols) {
            strs.add(col.getName());
        }
        return strs;
    }

    private DbUtil() { }
}
