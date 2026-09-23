package org.wikipedia.richtext

import android.text.Annotation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wikipedia.compose.extensions.composeFromHtml

@RunWith(RobolectricTestRunner::class)
class CustomHtmlParserTest {

    @Test
    fun testSimpleHiddenSpan() {
        val html = """visible<span style="display:none">hidden</span>text"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        assertEquals("visibletext", result)
    }

    @Test
    fun testNestedHiddenSpan() {
        // This is the core pattern from Polish Wikipedia's {{Cite}} template.
        // A display:none span contains child spans with visible text that must
        // all be suppressed.
        val html = """<span class="cite-name-after" style="display:none">""" +
                """<span>&nbsp;</span>""" +
                """<span class="cite-name-full">Guy</span>""" +
                """<span class="cite-name-initials">G.</span>""" +
                """</span>"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        assertEquals("", result)
    }

    @Test
    fun testCiteTemplateAuthorRendering() {
        // Full realistic {{Cite}} template output with both cite-name-before
        // (containing a display:none initials span) and cite-name-after
        // (entire span is display:none with nested children).
        val html = """<span class="cite-name-before">""" +
                """<span class="cite-name-full">Guy</span>""" +
                """<span class="cite-name-initials" style="display:none">G.</span>""" +
                """<span>&nbsp;</span>""" +
                """</span>""" +
                """<span class="cite-lastname">Mansell</span>""" +
                """<span class="cite-name-after" style="display:none">""" +
                """<span>&nbsp;</span>""" +
                """<span class="cite-name-full">Guy</span>""" +
                """<span class="cite-name-initials">G.</span>""" +
                """</span>""" +
                """, <i>Irlandia</i>"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        // &nbsp; is rendered as \u00A0 (non-breaking space) by parseAsHtml.
        assertEquals("Guy\u00A0Mansell, Irlandia", result)
    }

    @Test
    fun testMultipleAuthorsWithCiteTemplate() {
        // Two authors, each with display:none spans for initials and after-name.
        val html = """<span class="cite-name-before">""" +
                """<span class="cite-name-full">Guy</span>""" +
                """<span class="cite-name-initials" style="display:none">G.</span>""" +
                """<span>&nbsp;</span>""" +
                """</span>""" +
                """<span class="cite-lastname">Mansell</span>""" +
                """<span class="cite-name-after" style="display:none">""" +
                """<span>&nbsp;</span>""" +
                """<span class="cite-name-full">Guy</span>""" +
                """<span class="cite-name-initials">G.</span>""" +
                """</span>""" +
                """, """ +
                """<span class="cite-name-before">""" +
                """<span class="cite-name-full">Jason</span>""" +
                """<span class="cite-name-initials" style="display:none">J.</span>""" +
                """<span>&nbsp;</span>""" +
                """</span>""" +
                """<span class="cite-lastname">Mitchell</span>""" +
                """<span class="cite-name-after" style="display:none">""" +
                """<span>&nbsp;</span>""" +
                """<span class="cite-name-full">Jason</span>""" +
                """<span class="cite-name-initials">J.</span>""" +
                """</span>"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        assertEquals("Guy\u00A0Mansell, Jason\u00A0Mitchell", result)
    }

    @Test
    fun testMultipleSequentialHiddenSpans() {
        val html = """A<span style="display:none">X</span>""" +
                """B<span style="display:none">Y</span>C"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        assertEquals("ABC", result)
    }

    @Test
    fun testNonHiddenSpansPreserved() {
        val html = """<span class="normal">visible</span> text"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        assertEquals("visible text", result)
    }

    @Test
    fun testDeeplyNestedHiddenSpan() {
        val html = """before<span style="display:none">""" +
                """<span><span><span>deep</span></span></span>""" +
                """</span>after"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        assertEquals("beforeafter", result)
    }

    @Test
    fun testEmptyHiddenSpan() {
        val html = """before<span style="display:none"></span>after"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        assertEquals("beforeafter", result)
    }

    @Test
    fun testDisplayNoneWithSpaces() {
        // Some templates may have spaces around the colon in the style attribute.
        val html = """visible<span style="display: none">hidden</span>text"""
        val result = CustomHtmlParser.fromHtml(html).toString()
        assertEquals("visibletext", result)
    }

    @Test
    fun testSearchMatchSpanAnnotation() {
        val html = """This is <span style="searchmatch">highlighted</span> text"""
        val spanned = CustomHtmlParser.fromHtml(html)
        val annotations = spanned.getSpans(0, spanned.length, Annotation::class.java)
        assertEquals(1, annotations.size)
        assertEquals("searchmatch", annotations[0].value)
        assertEquals(8, spanned.getSpanStart(annotations[0]))
        assertEquals(19, spanned.getSpanEnd(annotations[0]))
    }

    @Test
    fun testSearchMatchComposeFromHtml() {
        val html = """This is <span style="searchmatch">highlighted</span> text"""
        val customStyle = SpanStyle(background = Color.Yellow)
        val annotatedString = AnnotatedString.composeFromHtml(
            htmlString = html,
            linkStyles = null,
            searchMatchStyle = customStyle
        )
        val spanStyles = annotatedString.spanStyles
        assertEquals(1, spanStyles.size)
        assertEquals(customStyle, spanStyles[0].item)
        assertEquals(8, spanStyles[0].start)
        assertEquals(19, spanStyles[0].end)
    }
}
