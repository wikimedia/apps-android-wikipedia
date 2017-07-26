package org.wikipedia.offline;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.wikipedia.settings.Prefs;
import org.wikipedia.util.log.L;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public final class OfflineManager {
    private static OfflineManager INSTANCE;
    @Nullable private CompilationSearchTask searchTask;
    private long lastSearchTime;
    @NonNull private List<Compilation> compilations = new ArrayList<>();

    public interface Callback {
        void onCompilationsFound(@NonNull List<Compilation> compilations);
        void onError(@NonNull Throwable t);
    }

    public static OfflineManager instance() {
        if (INSTANCE == null) {
            INSTANCE = new OfflineManager();
            INSTANCE.restoreFromCache();
        }
        return INSTANCE;
    }

    public static boolean hasCompilation() {
        return instance().compilations().size() > 0;
    }

    public boolean shouldSearchAgain() {
        return System.currentTimeMillis() - lastSearchTime > TimeUnit.HOURS.toMillis(1);
    }

    @NonNull
    public List<Compilation> compilations() {
        return compilations;
    }

    public void searchForCompilations(@NonNull final Callback callback) {
        if (searchTask != null) {
            searchTask.cancel();
        }
        lastSearchTime = System.currentTimeMillis();
        searchTask = new CompilationSearchTask() {
            @Override public void onFinish(List<Compilation> results) {
                if (isCancelled()) {
                    return;
                }
                for (Compilation c : compilations) {
                    for (Compilation result : results) {
                        if (result.path().equals(c.path())) {
                            result.copyMetadataFrom(c);
                        }
                    }
                    c.close();
                }
                compilations.clear();
                compilations.addAll(results);
                Prefs.setCompilationCache(compilations);
                callback.onCompilationsFound(results);
            }

            @Override public void onCatch(Throwable caught) {
                L.e("Error while searching for compilations.", caught);
                callback.onError(caught);
            }
        };
        searchTask.execute();
    }

    public boolean titleExists(@NonNull String title) {
        for (Compilation c : compilations) {
            if (c.titleExists(title)) {
                return true;
            }
        }
        return false;
    }

    @NonNull public List<String> searchByPrefix(@NonNull String prefix, int maxResults) throws IOException {
        List<String> results = new ArrayList<>();
        for (Compilation c : compilations) {
            results.addAll(c.searchByPrefix(prefix, maxResults));
        }
        return results;
    }

    @Nullable public String getNormalizedTitle(@NonNull String title) {
        try {
            for (Compilation c : compilations) {
                String result = c.getNormalizedTitle(title);
                if (result != null && result.length() > 0) {
                    return result;
                }
            }
        } catch (Exception e) {
            L.e(e);
        }
        return null;
    }

    @NonNull public HtmlResult getHtmlForTitle(@NonNull String title) throws IOException {
        for (Compilation c : compilations) {
            ByteArrayOutputStream stream = c.getDataForTitle(title);
            if (stream != null) {
                return new HtmlResult(c, stream.toString("utf-8"));
            }
        }
        throw new IOException("Content not found in any compilation for " + title);
    }

    @Nullable public ByteArrayOutputStream getDataForUrl(@NonNull String url) throws IOException {
        if (url.startsWith("A/") || url.startsWith("I/")) {
            url = url.substring(2);
        }
        for (Compilation c : compilations) {
            ByteArrayOutputStream stream = c.getDataForUrl(url);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    @NonNull public String getRandomTitle() throws IOException {
        int compIndex = new Random().nextInt(compilations.size());
        return compilations.get(compIndex).getRandomTitle();
    }

    @NonNull public String getMainPageTitle() throws IOException {
        int compIndex = new Random().nextInt(compilations.size());
        return compilations.get(compIndex).getMainPageTitle();
    }

    @VisibleForTesting void setCompilations(@NonNull List<Compilation> compilations) {
        this.compilations = compilations;
    }

    public static class HtmlResult {
        @NonNull private String html;
        @NonNull private Compilation comp;

        public HtmlResult(@NonNull Compilation comp, @NonNull String html) {
            this.html = html;
            this.comp = comp;
        }

        public Compilation compilation() {
            return comp;
        }

        public String html() {
            return html;
        }
    }

    private void restoreFromCache() {
        for (Compilation cached : Prefs.getCompilationCache()) {
            try {
                Compilation c = new Compilation(new File(cached.path()));
                c.copyMetadataFrom(cached);
                L.d("Restoring compilation from cache: " + c.path());
                compilations.add(c);
            } catch (IOException e) {
                L.w("Cached compilation no longer available: " + cached.path(), e);
            }
        }
    }

    @VisibleForTesting
    static OfflineManager instanceNoCache() {
        if (INSTANCE == null) {
            INSTANCE = new OfflineManager();
        }
        return INSTANCE;
    }

    private OfflineManager() {
    }
}
