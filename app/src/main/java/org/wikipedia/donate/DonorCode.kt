package org.wikipedia.donate

enum class DonorCode(val value: Int) {
    NONE(0),
    RECENT(1),
    SUSTAINING(2),
    LAPSED(3),
    RETURNING(4),
    CONTACTABLE(5),
    UNKNOWN(-1);

    companion object {
        fun fromValue(value: Int?): DonorCode =
            when (value) {
                null, 0 -> NONE
                else -> entries.firstOrNull { it.value == value } ?: UNKNOWN
            }
    }
}
