package org.wikipedia.concurrency;

import androidx.annotation.NonNull;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.CheckReturnValue;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

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
