package org.wikipedia.util

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import org.wikipedia.notifications.PollNotificationService

object JobsUtil {
    fun schedulePollNotificationJob(context: Context) {
        val serviceComponent = ComponentName(context, PollNotificationService::class.java)
        val builder = JobInfo.Builder(0, serviceComponent)
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(builder.build())
    }
}
