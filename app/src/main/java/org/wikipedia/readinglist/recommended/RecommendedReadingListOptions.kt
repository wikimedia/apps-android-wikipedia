package org.wikipedia.readinglist.recommended

enum class UpdateFrequency {
    DAILY, WEEKLY, MONTHLY;

    companion object {
        fun fromInt(value: Int): UpdateFrequency {
            return entries[value]
        }
    }
}

enum class UpdateSource {
    INTERESTS, READING_LIST, HISTORY
}
