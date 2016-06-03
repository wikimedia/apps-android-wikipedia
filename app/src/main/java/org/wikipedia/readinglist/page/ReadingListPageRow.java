package org.wikipedia.readinglist.page;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.Site;
import org.wikipedia.model.BaseModel;
import org.wikipedia.page.Namespace;
import org.wikipedia.readinglist.page.database.ReadingListPageDiskTable;
import org.wikipedia.readinglist.page.database.ReadingListPageHttpTable;
import org.wikipedia.readinglist.page.database.ReadingListPageTable;
import org.wikipedia.util.ValidateUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReadingListPageRow extends BaseModel {
    public static final ReadingListPageTable DATABASE_TABLE = new ReadingListPageTable();
    public static final ReadingListPageHttpTable HTTP_DATABASE_TABLE = new ReadingListPageHttpTable();
    public static final ReadingListPageDiskTable DISK_DATABASE_TABLE = new ReadingListPageDiskTable();

    @NonNull private final String key;
    @NonNull private final Set<String> listKeys;
    @NonNull private final Site site;
    @NonNull private final Namespace namespace;
    @NonNull private final String title;
    @Nullable private Long diskPageRevision;
    private final long mtime;
    private long atime;
    @Nullable private String thumbnailUrl;
    @Nullable private String description;

    public static Builder<?> builder() {
        //noinspection rawtypes
        return new Builder();
    }

    @NonNull public String key() {
        return key;
    }

    @NonNull public Site site() {
        return site;
    }

    /** @return an empty list if the page is to be deleted. */
    @NonNull public Set<String> listKeys() {
        return Collections.unmodifiableSet(listKeys);
    }

    public void addListKey(@NonNull String listKey) {
        listKeys.add(listKey);
    }

    public void removeListKey(@NonNull String listKey) {
        listKeys.remove(listKey);
    }

    @NonNull public Namespace namespace() {
        return namespace;
    }

    @NonNull public String title() {
        return title;
    }

    @Nullable public Long diskPageRevision() {
        return diskPageRevision;
    }

    /** @return the time of last modification in seconds since epoch. */
    public long mtime() {
        return mtime;
    }

    /** @return the time of last access in seconds since epoch. */
    public long atime() {
        return atime;
    }

    public void touch() {
        atime = System.currentTimeMillis();
    }

    @Nullable public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(@Nullable String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable public String description() {
        return description;
    }

    protected ReadingListPageRow(@NonNull Builder<?> builder) {
        key = builder.key;
        listKeys = new HashSet<>(builder.listKeys);
        site = builder.site;
        namespace = builder.namespace;
        title = builder.title;
        diskPageRevision = builder.diskPageRevision;
        mtime = builder.mtime;
        atime = builder.atime;
        thumbnailUrl = builder.thumbnailUrl;
        description = builder.description;
    }

    @SuppressWarnings("unchecked")
    public static class Builder<Clazz extends Builder<Clazz>> {
        private String key;
        private Set<String> listKeys = new HashSet<>();
        private Site site;
        private Namespace namespace;
        private String title;
        private Long diskPageRevision;
        private Long mtime;
        private Long atime;
        private String thumbnailUrl;
        private String description;

        public Clazz copy(@NonNull ReadingListPageRow copy) {
            return key(copy.key)
                    .listKeys(copy.listKeys)
                    .site(copy.site)
                    .namespace(copy.namespace)
                    .title(copy.title)
                    .mtime(copy.mtime)
                    .atime(copy.atime)
                    .thumbnailUrl(copy.thumbnailUrl)
                    .description(copy.description);
        }

        public Clazz key(@NonNull String key) {
            this.key = key;
            return (Clazz) this;
        }

        public Clazz listKeys(@NonNull String listKey) {
            listKeys = new HashSet<>();
            listKeys.add(listKey);
            return (Clazz) this;
        }

        public Clazz listKeys(@NonNull Set<String> listKeys) {
            this.listKeys = new HashSet<>(listKeys);
            return (Clazz) this;
        }

        public Clazz site(@NonNull Site site) {
            this.site = site;
            return (Clazz) this;
        }

        public Clazz namespace(@NonNull Namespace namespace) {
            this.namespace = namespace;
            return (Clazz) this;
        }

        public Clazz title(@NonNull String title) {
            this.title = title;
            return (Clazz) this;
        }

        public Clazz diskPageRevision(long diskPageRevision) {
            this.diskPageRevision = diskPageRevision;
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

        public Clazz thumbnailUrl(@Nullable String thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl;
            return (Clazz) this;
        }

        public Clazz description(@Nullable String description) {
            this.description = description;
            return (Clazz) this;
        }

        public ReadingListPageRow build() {
            validate();
            return new ReadingListPageRow(this);
        }

        // TODO: listKeys allows empty currently. Should we permit it? It means delete.
        protected void validate() {
            ValidateUtil.noNullElements(key, site, namespace, title, mtime, atime);
        }
    }
}