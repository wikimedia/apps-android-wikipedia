package org.wikipedia.concurrency;

import androidx.annotation.NonNull;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.CheckReturnValue;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class RxBus {

    private final Subject<Object> bus = PublishSubject.create().toSerialized();
    private final Observable<Object> observable = bus.observeOn(AndroidSchedulers.mainThread());

    public void post(Object o) {
        bus.onNext(o);
    }

    @CheckReturnValue
    public Disposable subscribe(@NonNull Consumer<Object> consumer) {
        return observable.subscribe(consumer);
    }
}
