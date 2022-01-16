package org.wikipedia.util.log

import kotlinx.coroutines.CoroutineExceptionHandler

val WARNING_HANDLER = CoroutineExceptionHandler { _, exception -> L.w(exception) }
