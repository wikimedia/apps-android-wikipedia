package org.wikipedia.offline;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.annotation.NonNull;

import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.util.log.L;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

class CompilationSearchTask extends SaneAsyncTask<List<Compilation>> {
    private List<Compilation> compilations = new ArrayList<>();

    @Override
    public List<Compilation> performTask() throws Throwable {
        List<String> pathList = new ArrayList<>();
        StorageManager sm = (StorageManager) WikipediaApp.getInstance().getSystemService(Context.STORAGE_SERVICE);
        try {
            String[] volumes = (String[]) sm.getClass().getMethod("getVolumePaths").invoke(sm);
            if (volumes != null && volumes.length > 0) {
                pathList.addAll(Arrays.asList(volumes));
            }
        } catch (Exception e) {
            L.e(e);
        }
        if (pathList.size() == 0 && Environment.getExternalStorageDirectory() != null) {
            pathList.add(Environment.getExternalStorageDirectory().getAbsolutePath());
        }
        for (String path : pathList) {
            findCompilations(new File(path), 0);
            if (isCancelled()) {
                break;
            }
        }
        return compilations;
    }

    private void findCompilations(@NonNull File parentDir, int level) {
        if (level > 10) {
            return;
        }
        File[] files = parentDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (isCancelled()) {
                return;
            }
            if (file.isDirectory()) {
                findCompilations(file, level + 1);
            } else {
                if (isCompilation(file)) {
                    add(file);
                }
            }
        }
    }

    private void add(@NonNull File file) {
        try {
            compilations.add(new Compilation(file));
            L.d("Found compilation: " + file.getAbsolutePath());
        } catch (IOException e) {
            L.e("Error opening compilation: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private boolean isCompilation(@NonNull File f) {
        return f.getName().toLowerCase(Locale.ROOT).endsWith(".zim");
    }
}
