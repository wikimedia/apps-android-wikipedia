package org.wikipedia.dataclient.okhttp
import android.util.Log
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.Socket
import java.security.KeyStore
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
private val sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
private val censoredDomain = listOf(
    "wikipedia.org",
    "wikinews.org",
    "wikiquote.org"
)
private fun String.isCensored() = censoredDomain.any { this == it || this.endsWith(".$it") }
object OkHttpSSLSocketFactory : SSLSocketFactory() {
    override fun getDefaultCipherSuites(): Array<String> = sslSocketFactory.defaultCipherSuites
    override fun getSupportedCipherSuites(): Array<String> = sslSocketFactory.supportedCipherSuites
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        val address = s.inetAddress.hostAddress.takeIf { host.isCensored() || host in WIKIMEDIA_DOH_URL }
        Log.d("OkHttSSLSocketFactory", "Host: $host Address: $address")
        return sslSocketFactory.createSocket(s, address ?: host, port, autoClose) as SSLSocket
    }
    override fun createSocket(host: String, port: Int): Socket = sslSocketFactory.createSocket(host, port)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket = sslSocketFactory.createSocket(host, port, localHost, localPort)
    override fun createSocket(host: InetAddress, port: Int): Socket = sslSocketFactory.createSocket(host, port)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket = sslSocketFactory.createSocket(address, port, localAddress, localPort)
}
fun OkHttpClient.Builder.install(sslSocketFactory: SSLSocketFactory) = apply {
    val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())!!
    factory.init(null as KeyStore?)
    val manager = factory.trustManagers!!
    val trustManager = manager.filterIsInstance<X509TrustManager>().first()
    sslSocketFactory(sslSocketFactory, trustManager)
}
