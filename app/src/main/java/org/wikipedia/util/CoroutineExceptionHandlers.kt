package org.wikipedia.util

import kotlinx.coroutines.CoroutineExceptionHandler
import org.wikipedia.util.log.L

val ERROR_LOG_HANDLER = CoroutineExceptionHandler { _, throwable -> L.e(throwable) }
val WARNING_LOG_HANDLER = CoroutineExceptionHandler { _, throwable -> L.w(throwable) }
