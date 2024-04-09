package org.wikipedia.analytics.eventplatform

import android.content.Context
import org.wikipedia.readinglist.database.ReadingList

object ReadingListsAnalyticsHelper {

    fun logListsShown(context: Context, listsCount: Int) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingLists.ListsCount:$listsCount.shown"
            )
        )
    }

    fun logListShown(context: Context, listCount: Int) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemsCount:$listCount.shown"
            )
        )
    }

    fun logShareList(context: Context, list: ReadingList) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount:${list.pages.size}.shared"
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
                "ReadingList.ListItemCount:${list.pages.size}.receive_preview"
            )
        )
    }

    fun logReceiveCancel(context: Context, list: ReadingList?) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount:${list?.pages?.size ?: 0}.receive_cancel"
            )
        )
    }

    fun logReceiveFinish(context: Context, list: ReadingList?) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount:${list?.pages?.size ?: 0}.receive_finish"
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

    fun logExportLists(context: Context, listCount: Int) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount:$listCount.exported"
            )
        )
    }

    fun logImportStart(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.import_started"
            )
        )
    }
    fun logImportCancelled(context: Context) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.import_cancelled"
            )
        )
    }

    fun logImportFinished(context: Context, listCount: Int) {
        EventPlatformClient.submit(
            BreadCrumbLogEvent(
                BreadCrumbViewUtil.getReadableScreenName(context),
                "ReadingList.ListItemCount:$listCount.import_finished"
            )
        )
    }
}
