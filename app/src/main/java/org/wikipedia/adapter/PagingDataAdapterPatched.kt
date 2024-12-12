package org.wikipedia.adapter

import androidx.lifecycle.LifecycleCoroutineScope
import androidx.paging.PagingData
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

abstract class PagingDataAdapterPatched<T : Any, VH : RecyclerView.ViewHolder>
(
    diffCallback: DiffUtil.ItemCallback<T>,
    mainDispatcher: CoroutineContext = Dispatchers.Main,
    workerDispatcher: CoroutineContext = Dispatchers.Default,
) : PagingDataAdapter<T, VH>(diffCallback, mainDispatcher, workerDispatcher) {
    private var submitCompleted = true

    // HACK: This is a workaround for a race condition that seems to be present in PagingDataAdapter,
    // where submitting data too quickly in succession can cause the adapter to throw an exception.
    // This method ensures that the adapter is not in the process of submitting data before attempting
    // to submit more. This method should be used in place of the normal submitData method.
    // NOTE: This may mean that when submitData is called multiple times quickly, certain calls
    // may be "dropped", i.e. ignored. This is a tradeoff to prevent crashes.
    // TODO: File issue with AOSP and link here.
    fun submitData(scope: LifecycleCoroutineScope, pagingData: PagingData<T>) {
        scope.launch {
            if (!submitCompleted) {
                return@launch
            }
            async {
                submitCompleted = false
                submitData(pagingData)
                submitCompleted = true
            }
        }
    }
}
