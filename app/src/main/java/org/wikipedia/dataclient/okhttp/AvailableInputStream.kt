package org.wikipedia.dataclient.okhttp

import java.io.IOException
import java.io.InputStream

/**
 * This is a subclass of InputStream that implements the available() method reliably enough
 * to satisfy WebResourceResponses or other consumers like BufferedInputStream that depend
 * on available() to return a meaningful value.
 *
 * The problem is that the InputStream provided by OkHttp's body().byteStream() returns zero
 * when calling available() prior to making any read() calls, which means that it will break
 * any consumers that wrap a BufferedInputStream onto this stream, or any other wrapper that
 * relies on a consistent implementation of available().
 *
 * This is initialized with the original InputStream plus its total size, which must be known
 * at the time of instantiation.  You may then call the read() and skip() methods in the usual
 * way, and then be able to call available() and get the number of bytes left to read.
 */
class AvailableInputStream(private val stream: InputStream, private var available: Long) : InputStream() {

    @Throws(IOException::class)
    override fun read(): Int {
        decreaseAvailable(1)
        return stream.read()
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray): Int {
        val ret = stream.read(b)
        if (ret > 0) {
            decreaseAvailable(ret.toLong())
        }
        return ret
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val ret = stream.read(b, off, len)
        if (ret > 0) {
            decreaseAvailable(ret.toLong())
        }
        return ret
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        val ret = stream.skip(n)
        if (ret > 0) {
            decreaseAvailable(ret)
        }
        return ret
    }

    @Throws(IOException::class)
    override fun available(): Int {
        val ret = stream.available()
        return if (ret == 0 && available > 0) {
            available.toInt()
        } else ret
    }

    private fun decreaseAvailable(n: Long) {
        available -= n
        if (available < 0) {
            available = 0
        }
    }
}