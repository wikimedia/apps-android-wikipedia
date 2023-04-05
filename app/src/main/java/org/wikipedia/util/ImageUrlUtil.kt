package org.wikipedia.util

object ImageUrlUtil {

    private val WIDTH_IN_IMAGE_URL_REGEX = """/(page\d+-)?(\d+)(px-)""".toPattern()

    fun getUrlForPreferredSize(original: String, size: Int): String {
        val matcher = WIDTH_IN_IMAGE_URL_REGEX.matcher(original)
        return if (matcher.find() && matcher.group(2)!!.toInt() != size) {
            matcher.replaceFirst("/${matcher.group(1).orEmpty()}$size$3")
        } else {
            original
        }
    }
}
