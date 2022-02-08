package org.wikipedia.util

open class Resource<T> {
    class Success<T>(val data: T) : Resource<T>()
    class Error<T>(val throwable: Throwable) : Resource<T>()
}
