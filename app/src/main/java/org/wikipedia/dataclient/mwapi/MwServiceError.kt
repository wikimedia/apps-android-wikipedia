package org.wikipedia.dataclient.mwapi

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.json.InstantAsString
import org.wikipedia.util.StringUtil
import org.wikipedia.util.ThrowableUtil
import java.time.Instant

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
            html = ThrowableUtil.getBlockMessageHtml(data.blockinfo)
        }
    }

    @Serializable
    class Data(val messages: List<Message>? = null, val blockinfo: BlockInfo? = null)

    @Serializable
    class Message(val name: String?, val html: String = "")

    @Serializable
    open class BlockInfo(
        @SerialName("blockedbyid") val blockedById: Int = 0,
        @SerialName("blockid") val blockId: Int = 0,
        @SerialName("blockedby") val blockedBy: String = "",
        @SerialName("blockreason") val blockReason: String = "",
        @SerialName("blockedtimestamp") val blockTimeStamp: InstantAsString? = null,
        @SerialName("blockexpiry") val blockExpiry: InstantAsString? = null
    ) {
        val isBlocked: Boolean
            get() = blockExpiry != null && blockExpiry > Instant.now()
    }
}
