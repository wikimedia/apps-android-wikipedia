package org.wikipedia.analytics

import org.json.JSONObject
import org.wikipedia.WikipediaApp
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ReleaseUtil

class PageScrollFunnel(app: WikipediaApp, private val pageId: Int) :
        TimedFunnel(app, SCHEMA_NAME, REV_ID, if (ReleaseUtil.isProdRelease) SAMPLE_LOG_100 else SAMPLE_LOG_ALL) {

    private var viewportHeight = 0
    private var pageHeight = 0
    private var scrollFluxDown = 0
    private var scrollFluxUp = 0
    private var maxScrollY = 0

    private val maxPercentViewed: Int
        get() {
            val maxPercent = 100
            return maxPercent.coerceAtMost(if (pageHeight == 0) 0
            else (maxScrollY + viewportHeight) * maxPercent / pageHeight)
        }

    override fun preprocessSessionToken(eventData: JSONObject) {}

    fun onPageScrolled(oldScrollY: Int, scrollY: Int, isHumanScroll: Boolean) {
        if (isHumanScroll) {
            if (scrollY > oldScrollY) {
                scrollFluxDown += scrollY - oldScrollY
            } else {
                scrollFluxUp += oldScrollY - scrollY
            }
        }
        maxScrollY = maxScrollY.coerceAtLeast(scrollY)
    }

    fun setPageHeight(height: Int) {
        pageHeight = (height * DimenUtil.densityScalar).toInt()
    }

    fun setViewportHeight(height: Int) {
        viewportHeight = height
    }

    fun logDone() {
        log(
                "pageID", pageId,
                "pageHeight", (pageHeight / DimenUtil.densityScalar).toInt(),
                "scrollFluxDown", (scrollFluxDown / DimenUtil.densityScalar).toInt(),
                "scrollFluxUp", (scrollFluxUp / DimenUtil.densityScalar).toInt(),
                "maxPercentViewed", maxPercentViewed
        )
    }

    companion object {
        private const val SCHEMA_NAME = "MobileWikiAppPageScroll"
        private const val REV_ID = 18118723
    }
}
