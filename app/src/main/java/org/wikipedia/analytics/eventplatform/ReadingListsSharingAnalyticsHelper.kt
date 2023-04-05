package org.wikipedia.analytics.eventplatform

import android.content.Context
import org.wikipedia.readinglist.database.ReadingList

object ReadingListsSharingAnalyticsHelper {

    fun logShareList(context: Context, list: ReadingList) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount.${list.pages.size}.shared"
            )
        )
    }

    fun logReceiveStart(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.receive_start"
            )
        )
    }

    fun logReceivePreview(context: Context, list: ReadingList) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount.${list.pages.size}.receive_preview"
            )
        )
    }

    fun logReceiveCancel(context: Context, list: ReadingList?) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount.${list?.pages?.size ?: 0}.receive_cancel"
            )
        )
    }

    fun logReceiveFinish(context: Context, list: ReadingList?) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount.${list?.pages?.size ?: 0}.receive_finish"
            )
        )
    }

    fun logSurveyShown(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context), "ReadingList.survey_shown"
            )
        )
    }
}
