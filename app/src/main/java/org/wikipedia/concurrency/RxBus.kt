package org.wikipedia.concurrency

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.annotations.CheckReturnValue
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Consumer
import io.reactivex.rxjava3.subjects.PublishSubject

class RxBus {
    private val bus = PublishSubject.create<Any>().toSerialized()
    private val observable = bus.observeOn(AndroidSchedulers.mainThread())

    fun post(o: Any) {
        bus.onNext(o)
    }

    @CheckReturnValue
    fun subscribe(consumer: Consumer<Any>): Disposable {
        return observable.subscribe(consumer)
    }
}
