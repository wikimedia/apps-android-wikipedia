package org.wikipedia.util

object ImageUrlUtil {

    private val WIDTH_IN_IMAGE_URL_REGEX = """/(\d+)px-""".toPattern()

    fun getUrlForPreferredSize(original: String, size: Int): String {
        val matcher = WIDTH_IN_IMAGE_URL_REGEX.matcher(original)
        return if (matcher.find() && matcher.group(1)!!.toInt() != size) {
            matcher.replaceAll("/${size}px-")
        } else {
            original
        }
    }
}
