package org.wikipedia.analytics.testkitchen

import android.widget.Toast
import org.wikimedia.testkitchen.LogAdapter
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.okhttp.HttpStatusException
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.log.L
import java.net.HttpURLConnection

class LogAdapterImpl : LogAdapter {
    override fun info(message: String, vararg args: Any) {
        L.i(message)
    }

    override fun warn(message: String, vararg args: Any) {
        L.w(message)
    }

    override fun error(message: String, vararg args: Any) {
        L.e(message)
        if (args.isNotEmpty() && args[0] is Exception) {
            L.e(args[0] as Exception)
            if (ReleaseUtil.isDevRelease && args[0] is HttpStatusException && (args[0] as HttpStatusException).code != HttpURLConnection.HTTP_FORBIDDEN) {
                // Display the error very loudly to alert about potential Test Kitchen issues.
                WikipediaApp.instance.mainThreadHandler.post {
                    Toast.makeText(WikipediaApp.instance, args[0].toString(), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
