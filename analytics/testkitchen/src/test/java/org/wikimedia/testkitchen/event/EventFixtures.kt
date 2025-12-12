package org.wikimedia.testkitchen.event

import org.wikimedia.testkitchen.context.ClientData
import org.wikimedia.testkitchen.context.InteractionData
import org.wikimedia.testkitchen.context.PageData
import org.wikimedia.testkitchen.context.PerformerData
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object EventFixtures {
    private val dt get() = DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("Z")))

    fun minimalEvent(clientData: ClientData = ClientData()): Event {
        return Event("test_schema", "test_stream", dt, clientData, InteractionData())
    }

    fun getEvent(
        id: Int?,
        namespaceName: String?,
        groups: List<String>,
        isLoggedIn: Boolean,
        editCount: String?
    ): Event {
        return Event("test/event", "test.event", dt,
            ClientData(
                pageData = PageData().also {
                    it.id = id
                    it.namespaceName = namespaceName
                },
                performerData = PerformerData().also {
                    it.groups = groups
                    it.isLoggedIn = isLoggedIn
                }
            ),
            InteractionData()
        )
    }
}
