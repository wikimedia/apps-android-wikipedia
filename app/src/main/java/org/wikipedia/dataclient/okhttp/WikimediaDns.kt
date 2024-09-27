package org.wikipedia.dataclient.okhttp
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
const val WIKIMEDIA_DOH_URL = "https://wikimedia-dns.org/dns-query"
private fun buildDoHDNS() = DnsOverHttps.Builder()
    .client(OkHttpConnectionFactory.client)
    .url(WIKIMEDIA_DOH_URL.toHttpUrl())
    .post(true)
    .bootstrapDnsHosts(
        InetAddress.getByName("185.71.138.138"),
        InetAddress.getByName("2001:67c:930::1")
    )
    .build()
object WikimediaDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return runCatching {
            buildDoHDNS().lookup(hostname).takeIf { it.isNotEmpty() }
        }.onFailure { it.printStackTrace() }
            .getOrNull() ?: Dns.SYSTEM.lookup(hostname)
    }
}
