package org.wikipedia.dataclient.mwapi

import com.google.gson.annotations.SerializedName
import org.wikipedia.dataclient.ServiceError
import org.wikipedia.json.PostProcessingTypeAdapter.PostProcessable
import org.wikipedia.util.DateUtil
import org.wikipedia.util.ThrowableUtil
import java.util.*

class MwServiceError(val code: String?,
                     var html: String?,
                     val data: Data? = null) : ServiceError, PostProcessable {

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

    override val details: String get() = html.orEmpty()

    override fun postProcess() {
        // Special case: if it's a Blocked error, parse the blockinfo structure ourselves.
        if (("blocked" == code || "autoblocked" == code) && data?.blockinfo != null) {
            html = ThrowableUtil.getBlockMessageHtml(data.blockinfo)
        }
    }

    class Data(val messages: List<Message>?, val blockinfo: BlockInfo?)

    class Message(val name: String?, val html: String = "")

    open class BlockInfo {

        @SerializedName("blockedbyid")
        val blockedById = 0
        @SerializedName("blockid")
        val blockId = 0
        @SerializedName("blockedby")
        val blockedBy: String = ""
        @SerializedName("blockreason")
        val blockReason: String = ""
        @SerializedName("blockedtimestamp")
        val blockTimeStamp: String = ""
        @SerializedName("blockexpiry")
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
