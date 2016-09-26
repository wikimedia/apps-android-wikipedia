package org.wikipedia.page.linkpreview;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.wikipedia.page.Page;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.xml.sax.InputSource;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Logic to obtain an extract text from a page. This extract text should be suitable to display
 * it as plain text in the LinkPreview dialog.
 */
public class PageExtract {
    private final Page page;
    private String text;

    public PageExtract(Page page) {
        this.page = page;
    }

    public String getText() {
        if (text == null) {
            text = extractTextFromPage();
        }
        return text;
    }

    private String extractTextFromPage() {
        NodeList elements = getXmlChildren(page.getSections().get(0).getContent());
        if (elements == null) {
            return "";
        }
        String firstSection = "";
        // First, extract the text of all the <p> tags from the first section
        for (int i = 0; i < elements.getLength(); i++) {
            if (elements.item(i).getNodeName().equalsIgnoreCase("p")) {
                firstSection += elements.item(i).getTextContent() + " ";
            }
        }
        // Strip the unwanted XML
        firstSection = StringUtil.fromHtml(firstSection).toString();
        // Strip the reference texts ([1], [2]...)
        firstSection = firstSection.replaceAll("\\[\\d+\\]", "");
        return firstSection;
    }

    /**
     * Parse the given HTML string and return a list of the immediate XML child nodes of that HTML.
     *
     * @param html HTML contents.
     * @return The list of XML child nodes, or null if there was an error.
     */
    private NodeList getXmlChildren(String html) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            L.e(e);
            return null;
        }
        InputSource inputSource = new InputSource(new StringReader("<dummy>" + html + "</dummy>"));
        Document document;
        try {
            document = builder.parse(inputSource);
        } catch (Exception e) {
            L.e(e);
            return null;
        }
        return document.getFirstChild().getChildNodes();
    }
}
