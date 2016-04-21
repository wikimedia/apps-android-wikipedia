package org.wikipedia.readinglist.database;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.model.BaseModel;
import org.wikipedia.util.ValidateUtil;

public class ReadingListRow extends BaseModel {
    public static final ReadingListTable DATABASE_TABLE = new ReadingListTable();

    @NonNull private final String key;
    @NonNull private final String title;
    private final long mtime;
    private final long atime;
    @Nullable private final String description;

    public static Builder<?> builder() {
        //noinspection rawtypes
        return new Builder();
    }

    @NonNull public String key() {
        return key;
    }

    @NonNull public String getTitle() {
        return title;
    }

    public long mtime() {
        return mtime;
    }

    public long atime() {
        return atime;
    }

    @Nullable public String getDescription() {
        return description;
    }

    protected ReadingListRow(@NonNull Builder<?> builder) {
        key = builder.key;
        title = builder.title;
        mtime = builder.mtime;
        atime = builder.atime;
        description = builder.description;
    }

    @SuppressWarnings("unchecked")
    public static class Builder<Clazz extends Builder<Clazz>> {
        private String key;
        private String title;
        private Long mtime;
        private Long atime;
        private String description;

        public Clazz copy(@NonNull ReadingListRow copy) {
            return key(copy.key)
                    .title(copy.title)
                    .mtime(copy.mtime)
                    .atime(copy.atime)
                    .description(copy.description);
        }

        public Clazz key(@NonNull String key) {
            this.key = key;
            return (Clazz) this;
        }

        public Clazz title(@NonNull String title) {
            this.title = title;
            return (Clazz) this;
        }

        public Clazz mtime(long mtime) {
            this.mtime = mtime;
            return (Clazz) this;
        }

        public Clazz atime(long atime) {
            this.atime = atime;
            return (Clazz) this;
        }

        public Clazz description(@Nullable String description) {
            this.description = description;
            return (Clazz) this;
        }

        public ReadingListRow build() {
            validate();
            return new ReadingListRow(this);
        }

        protected void validate() {
            ValidateUtil.noNullElements(key, title, mtime, atime);
        }
    }
}
