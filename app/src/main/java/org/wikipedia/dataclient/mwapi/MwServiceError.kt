package org.wikipedia.dataclient.mwapi

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.ThrowableUtil
import org.wikipedia.util.log.L
import java.util.*

@Serializable
class MwServiceError(val code: String? = null,
                     var html: String? = null,
                     val data: Data? = null) : ServiceError {

    fun badToken(): Boolean {
        return "badtoken" == code
    }

    fun badLoginState(): Boolean {
        return "assertuserfailed" == code
    }

    fun hasMessageName(messageName: String): Boolean {
        return data?.messages?.find { it.name == messageName } != null
    }

    fun getMessageHtml(messageName: String): String? {
        return data?.messages?.first { it.name == messageName }?.html
    }

    override val title: String get() = code.orEmpty()

    override val details: String get() = StringUtil.removeStyleTags(html.orEmpty())

    init {
        // Special case: if it's a Blocked error, parse the blockinfo structure ourselves.
        if (("blocked" == code || "autoblocked" == code) && data?.blockinfo != null) {
            runBlocking(CoroutineExceptionHandler { _, t ->
                L.e(t)
            }) {
                html = ThrowableUtil.getBlockMessageHtml(data.blockinfo)
            }
        }
    }

    @Serializable
    class Data(val messages: List<Message>? = null, val blockinfo: BlockInfo? = null)

    @Serializable
    class Message(val name: String?, val html: String = "")

    @Serializable
    open class BlockInfo {

        @SerialName("blockedbyid")
        val blockedById = 0
        @SerialName("blockid")
        val blockId = 0
        @SerialName("blockedby")
        val blockedBy: String = ""
        @SerialName("blockreason")
        val blockReason: String = ""
        @SerialName("blockedtimestamp")
        val blockTimeStamp: String = ""
        @SerialName("blockexpiry")
        val blockExpiry: String = ""

        val isBlocked: Boolean
            get() {
                if (blockExpiry.isEmpty()) {
                    return false
                }
                val now = Date()
                val expiry = DateUtil.iso8601DateParse(blockExpiry)
                return expiry.after(now)
            }
    }
}
