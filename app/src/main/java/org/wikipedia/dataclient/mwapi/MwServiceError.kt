package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.util.ThrowableUtil.getBlockMessageHtml
import java.util.*

/**
 * Moshi POJO for a MediaWiki API error.
 */
@JsonClass(generateAdapter = true)
class MwServiceError(
    @Json(name = "code") override val title: String = "",
    @Json(name = "html") internal val text: String = "",
    override var details: String = "",
    internal val data: Data? = null
) : ServiceError {
    val isBadToken: Boolean
        get() = "badtoken" == title
    val isBadLoginState: Boolean
        get() = "assertuserfailed" == title

    init {
        // Special case: if it's a Blocked error, parse the blockinfo structure ourselves.
        if (("blocked" == title || "autoblocked" == title) && data?.blockInfo != null) {
            details = getBlockMessageHtml(data.blockInfo)
        }
    }

    fun hasMessageName(messageName: String): Boolean {
        return data?.messages?.any { it.name == messageName } ?: false
    }

    fun getMessageHtml(messageName: String): String? {
        return data?.messages?.filter { it.name == messageName }?.map { it.html }?.firstOrNull()
    }

    @JsonClass(generateAdapter = true)
    class Data(val messages: List<Message> = emptyList(), @Json(name = "blockinfo") val blockInfo: BlockInfo? = null)

    @JsonClass(generateAdapter = true)
    class Message(val name: String = "", val html: String = "")

    @JsonClass(generateAdapter = true)
    open class BlockInfo(
        @Json(name = "blockid") val blockId: Int = 0,
        @Json(name = "blockedbyid") val blockedById: Int = 0,
        @Json(name = "blockreason") val blockReason: String = "",
        @Json(name = "blockedby") val blockedBy: String = "",
        @Json(name = "blockedtimestamp") val blockedTimeStamp: Date = Date(0),
        @Json(name = "blockexpiry") val blockExpiry: Date = Date(0)
    ) {
        val isBlocked: Boolean
            get() = blockExpiry.after(Date())
    }
}
