package org.wikipedia.firebase;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.wikipedia.util.Callback;

public final class FirebaseStateManager {

    public static void getCurrentToken(@NonNull Callback<String, Exception> callback) {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(
                (@NonNull Task<InstanceIdResult> task) -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(task.getResult().getToken());
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    private FirebaseStateManager() { }

}
