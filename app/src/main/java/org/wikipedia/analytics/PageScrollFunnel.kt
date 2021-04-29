package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DimenUtil.densityScalar
import org.wikipedia.util.ReleaseUtil.isProdRelease

class PageScrollFunnel(app: WikipediaApp, private val pageId: Int) : TimedFunnel(app, SCHEMA_NAME, REV_ID, if (isProdRelease) Funnel.Companion.SAMPLE_LOG_100 else Funnel.Companion.SAMPLE_LOG_ALL) {
    private var viewportHeight = 0
    private var pageHeight = 0
    private var scrollFluxDown = 0
    private var scrollFluxUp = 0
    private var maxScrollY = 0
    override fun preprocessSessionToken(eventData: JSONObject) {}
    fun onPageScrolled(oldScrollY: Int, scrollY: Int, isHumanScroll: Boolean) {
        if (isHumanScroll) {
            if (scrollY > oldScrollY) {
                scrollFluxDown += scrollY - oldScrollY
            } else {
                scrollFluxUp += oldScrollY - scrollY
            }
        }
        maxScrollY = Math.max(maxScrollY, scrollY)
    }

    fun setPageHeight(height: Int) {
        pageHeight = (height * densityScalar).toInt()
    }

    fun setViewportHeight(height: Int) {
        viewportHeight = height
    }

    fun logDone() {
        log(
                "pageID", pageId,
                "pageHeight", (pageHeight / densityScalar).toInt(),
                "scrollFluxDown", (scrollFluxDown / densityScalar).toInt(),
                "scrollFluxUp", (scrollFluxUp / densityScalar).toInt(),
                "maxPercentViewed", maxPercentViewed
        )
    }

    private val maxPercentViewed: Int
        get() {
            val maxPercent = 100
            return maxPercent.coerceAtMost(if (pageHeight == 0) 0
            else (maxScrollY + viewportHeight) * maxPercent / pageHeight)
        }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppPageScroll"
        private const val REV_ID = 18118723
    }
}
