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

    fun insertLangIntoThumbUrl(original: String, lang: String): String {
        val matcher = WIDTH_IN_IMAGE_URL_REGEX.matcher(original)
        return if (matcher.find() && matcher.group(2)!!.toInt() > 0) {
            matcher.replaceFirst("/${matcher.group(1).orEmpty()}lang$lang-${matcher.group(2).orEmpty()}$3")
        } else {
            original
        }
    }

    fun isGif(url: String?): Boolean {
        return url?.lowercase()?.endsWith(".gif") == true
    }
}
