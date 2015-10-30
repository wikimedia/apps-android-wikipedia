package org.wikipedia.util;

import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

/**
 * Common methods for dealing with runtime permissions.
 */
public final class PermissionUtil {

    public static boolean isPermitted(@NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        return grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    private PermissionUtil() { }
}
