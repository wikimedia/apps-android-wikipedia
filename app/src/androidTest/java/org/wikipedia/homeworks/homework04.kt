package org.wikipedia.homeworks

import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class ScheduleEntity(val lesson: String, val startTime: LocalTime, val endTime: LocalTime)

enum class Days {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

class Schedule {
    private val scheduleOfWeek = mutableMapOf<Days, MutableList<ScheduleEntity>>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private var day: Days? = null

    fun addSchedule(day: Days, scheduleEntity: ScheduleEntity) {
        scheduleOfWeek.getOrPut(day) { mutableListOf() }.add(scheduleEntity)
    }

    override fun toString(): String {
        return scheduleOfWeek.toSortedMap()
            .map { (day, list) ->
                list.sortedBy { it.startTime }
                    .joinToString("\n") {
                        "%-15s${it.startTime.format(timeFormatter)} - ${
                            it.endTime.format(
                                timeFormatter
                            )
                        }".format("\t${it.lesson}:")
                    }.let {
                        "${day.name.lowercase().replaceFirstChar { day.name[0].uppercase() }}:\n$it"
                    }
            }.joinToString("\n\n")
    }

    operator fun String.rangeTo(endAt: String): Pair<LocalTime, LocalTime> {
        return Pair(LocalTime.parse(this, timeFormatter), LocalTime.parse(endAt, timeFormatter))
    }

    infix fun Pair<LocalTime, LocalTime>.schedule(lesson: String) {
        day?.let {
            addSchedule(it, ScheduleEntity(lesson, first, second))
        } ?: throw IllegalStateException("No day set")
    }

    operator fun invoke(function: Schedule.() -> Unit) {function(this)}

    fun monday(function: () -> Unit)    {setScheduleInDay(Days.MONDAY, function)}
    fun tuesday(function: () -> Unit)   {setScheduleInDay(Days.TUESDAY, function)}
    fun wednesday(function: () -> Unit) {setScheduleInDay(Days.WEDNESDAY, function)}
    fun thursday(function: () -> Unit)  {setScheduleInDay(Days.THURSDAY, function)}
    fun friday(function: () -> Unit)    {setScheduleInDay(Days.FRIDAY, function)}
    fun saturday(function: () -> Unit)  {setScheduleInDay(Days.SATURDAY, function)}
    fun sunday(function: () -> Unit)    {setScheduleInDay(Days.SUNDAY, function) }

    private fun setScheduleInDay(day: Days, function: () -> Unit) {
        this.day = day
        function()
        this.day = null
    }
}

fun main() {
    val schedule = Schedule()
    // Without DSL
    schedule.addSchedule(Days.MONDAY, ScheduleEntity("Biology", LocalTime.of(10, 30), LocalTime.of(11, 10)))
    schedule.addSchedule(Days.MONDAY, ScheduleEntity("Chemistry", LocalTime.of(11, 15), LocalTime.of(11, 55)))

    // DSL
    schedule {
        monday {
            "10:30".."11:10" schedule "Biology"
            "11:15".."11:55" schedule "Chemistry"
            "09:00".."09:40" schedule "Mathematics"
            "09:45".."10:25" schedule "History"
        }
        tuesday {
            "09:00".."09:40" schedule "English"
            "09:45".."10:25" schedule "Geography"
            "11:15".."11:55" schedule "Art"
            "10:30".."11:10" schedule "Physics"
        }
        wednesday {
            "11:15".."11:55" schedule "Biology"
            "09:00".."09:40" schedule "Literature"
            "10:30".."11:10" schedule "History"
            "09:45".."10:25" schedule "Mathematics"
        }
        thursday {
            "11:15".."11:55" schedule "Physics"
            "10:30".."11:10" schedule "Geography"
            "09:00".."09:40" schedule "Chemistry"
            "09:45".."10:25" schedule "English"
        }
        friday {
            "09:45".."10:25" schedule "Literature"
            "11:15".."11:55" schedule "History"
            "09:00".."09:40" schedule "Art"
            "10:30".."11:10" schedule "Mathematics"
        }
    }

    println(schedule.toString())
}