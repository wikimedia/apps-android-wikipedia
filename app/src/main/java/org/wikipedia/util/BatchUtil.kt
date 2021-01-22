package org.wikipedia.util

import org.wikipedia.Constants
import org.wikipedia.page.PageTitle
import kotlin.math.min

object BatchUtil {
    // Useful for API requests in which we want a number of results that exceeds the limit for at
    // least one of the modules being used in the query (e.g., 50 for PageImages).
    //
    // TODO: This function does not yet handle batchcomplete.  For requests using a generator
    // together with properties, the API result may signal to continue because there are more
    // properties to retrieve for the pages so far, or because there are more pages from the
    // generator, or both.
    //
    // https://www.mediawiki.org/wiki/API:Query#batchcomplete
    //
    // Implement continuation/batchcomplete handling if we want to batch requests for a query in
    // which we are using a generator.
    //
    // Bug: T162497
    @JvmStatic
    fun <T> makeBatches(titles: List<PageTitle?>, handler: Handler<T>, callback: Callback<T>?) {
        var i = 0
        while (i < titles.size) {
            handler.handleBatch(titles.subList(i, i + min(Constants.API_QUERY_MAX_TITLES, titles.size - i)),
                    titles.size, object : Callback<T> {
                override fun success(results: List<T>) {
                    callback?.success(results)
                }

                override fun failure(caught: Throwable) {
                    callback?.failure(caught)
                }
            })
            i += min(Constants.API_QUERY_MAX_TITLES, titles.size - i)
        }
    }

    interface Handler<T> {
        fun handleBatch(batchTitles: List<PageTitle?>, total: Int, cb: Callback<T>?)
    }

    interface Callback<T> {
        fun success(results: List<T>)
        fun failure(caught: Throwable)
    }
}
