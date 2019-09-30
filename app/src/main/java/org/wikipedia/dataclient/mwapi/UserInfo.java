package org.wikipedia.dataclient.mwapi;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import org.wikipedia.util.DateUtil;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class UserInfo {
    private String name;
    private int id;
    @Nullable private List<String> groups;
    private int blockid;
    @Nullable private String blockreason;
    @Nullable private String blockedby;
    @Nullable private String blockedtimestamp;
    @Nullable private String blockexpiry;

    // Object type is any JSON type.
    @Nullable private Map<String, ?> options;

    public int id() {
        return id;
    }

    @NonNull public Set<String> getGroups() {
        return groups != null ? new ArraySet<>(groups) : Collections.emptySet();
    }

    public boolean isBlocked() {
        if (TextUtils.isEmpty(blockexpiry)) {
            return false;
        }
        try {
            Date now = new Date();
            Date expiry = DateUtil.iso8601DateParse(blockexpiry);
            if (expiry.after(now)) {
                return true;
            }
        } catch (ParseException e) {
            // ignore
        }
        return false;
    }

    @NonNull public Map<String, String> userjsOptions() {
        Map<String, String> map = new HashMap<>();
        if (options != null) {
            for (Map.Entry<String, ?> entry : options.entrySet()) {
                if (entry.getKey().startsWith("userjs-")) {
                    // T161866 entry.valueOf() should always return a String but doesn't
                    map.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                }
            }
        }
        return map;
    }
}
