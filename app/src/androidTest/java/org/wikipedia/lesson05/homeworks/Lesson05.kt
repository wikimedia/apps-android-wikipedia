package org.wikipedia.lesson05.homeworks

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

    private var day: Days? = null

    operator fun invoke(fnc: Schedule.() -> Unit) {
        fnc()
    }

    fun monday(fnc: () -> Unit) = addDay(Days.MONDAY, fnc)

    fun tuesday(fnc: () -> Unit) = addDay(Days.TUESDAY, fnc)

    fun wednesday(fnc: () -> Unit) = addDay(Days.WEDNESDAY, fnc)

    fun thursday(fnc: () -> Unit) = addDay(Days.THURSDAY, fnc)

    fun friday(fnc: () -> Unit) = addDay(Days.FRIDAY, fnc)

    fun saturday(fnc: () -> Unit) = addDay(Days.SATURDAY, fnc)

    fun sunday(fnc: () -> Unit) = addDay(Days.SUNDAY, fnc)

    operator fun String.rangeTo(time: String): Pair<LocalTime, LocalTime> {
        return LocalTime.parse(this, timeFormatter) to
                LocalTime.parse(time, timeFormatter)
    }

    infix fun Pair<LocalTime, LocalTime>.schedule(lesson: String) {
        addSchedule(
            day ?: throw IllegalStateException("Не задан день недели"),
            ScheduleEntity(lesson, first, second)
        )
    }

    private fun addDay(day: Days, fnc: () -> Unit) {
        this.day = day
        fnc()
        this.day = null
    }

}

fun main() {

    val schedule = Schedule()
    
    schedule.addSchedule(Days.MONDAY, ScheduleEntity("Biology", LocalTime.of(10, 30), LocalTime.of(11, 10)))
    schedule.addSchedule(Days.MONDAY, ScheduleEntity("Chemistry", LocalTime.of(11, 15), LocalTime.of(11, 55)))

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

        saturday {
            "09:45".."10:25" schedule "Literature"
            "11:15".."11:55" schedule "History"
            "09:00".."09:40" schedule "Art"
            "10:30".."11:10" schedule "Mathematics"
        }

        sunday {
            "09:45".."10:25" schedule "Literature"
            "11:15".."11:55" schedule "History"
            "09:00".."09:40" schedule "Art"
            "10:30".."11:10" schedule "Mathematics"
        }
    }

    println(schedule.toString())
}