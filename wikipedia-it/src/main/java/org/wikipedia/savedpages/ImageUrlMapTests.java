package org.wikipedia.savedpages;

import junit.framework.TestCase;

/** Unit test for ImageUrlMap */
public class ImageUrlMapTests extends TestCase {
    private static final String BASE_DIR = "/data/short/img";

    private ImageUrlMap.Builder builder = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        builder = new ImageUrlMap.Builder(BASE_DIR);
    }

    private static final String HTML_INPUT
            = "<div><img alt=\"Alt\" src=\"//foo.org/aaa.png\" width=\"30\"/></div>"
            + "<div><img alt=\"Alt\" src=\"//foo.org/bbb.png\" width=\"30\"/></div>"
            + "<div><img alt=\"Alt\" src=\"//foo.org/aaa.png\" width=\"30\"/></div>"; // repeated the first one

    private static final String IMG_MAP_JSON_OUTPUT
            = "{\"img_map\":["
            + "{\"originalURL\":\"\\/\\/foo.org\\/aaa.png\",\"newURL\":\"file:\\/\\/\\/data\\/short\\/img\\/b1ce85b9f1bd65f79d42ad3358f51f8.png\"},"
            + "{\"originalURL\":\"\\/\\/foo.org\\/bbb.png\",\"newURL\":\"file:\\/\\/\\/data\\/short\\/img\\/a0bf5f6da269a012fefb997167844e3.png\"}"
            + "]}";

    public void testUrlRewrite() throws Exception {
        builder.extractUrlsInSection(HTML_INPUT);
        ImageUrlMap imageUrlMap = builder.build();
        assertEquals(2, imageUrlMap.size());
        assertEquals(IMG_MAP_JSON_OUTPUT, imageUrlMap.toJSON().toString());
    }

    public void testNonClosedImgTag() throws Exception {
        // main page on 2014-06-10; like most main pages right now it has img tags that are not closed
        builder.extractUrlsInSection(
                "<div id=\"mainpage\"><h2>Today's featured article</h2><div id=\"mp-tfa\" style=\"padding:2px 5px\">\n" +
                        "<div style=\"float: left; margin: 0.5em 0.9em 0.4em 0em;\"><a href=\"/wiki/File:Fritz_Delius_(1907).jpg\" class=\"image\" title=\"Frederick Delius\">\n" +
                        "<img alt=\"Frederick Delius\" src=\"//upload.wikimedia.org/wikipedia/en/thumb/7/79/Fritz_Delius_%281907%29.jpg/100px-Fritz_Delius_%281907%29.jpg\" width=\"100\" height=\"148\" srcset=\"//upload.wikimedia.org/wikipedia/en/thumb/7/79/Fritz_Delius_%281907%29.jpg/150px-Fritz_Delius_%281907%29.jpg 1.5x, //upload.wikimedia.org/wikipedia/en/thumb/7/79/Fritz_Delius_%281907%29.jpg/200px-Fritz_Delius_%281907%29.jpg 2x\" \n" +
                        "data-file-width=\"1596\" data-file-height=\"2368\"></a></div>\n" +
                        "<p><b><a href=\"/wiki/Frederick_Delius\" title=\"Frederick Delius\">Frederick Delius</a></b> (1862–1934) was an English composer. Born in the north of England to a prosperous mercantile family, he was sent to <a href=\"/wiki/Florida\" title=\"Florida\">Florida</a> in 1884 to manage an orange plantation. Influenced by <a href=\"/wiki/African-American_music\" title=\"African-American music\">African-American music</a>, he began composing. After a brief period of formal musical study in Germany from 1886, he embarked on a full-time career as a composer in France, living in <a href=\"/wiki/Grez-sur-Loing\" title=\"Grez-sur-Loing\">Grez-sur-Loing</a> with his wife <a href=\"/wiki/Jelka_Rosen\" title=\"Jelka Rosen\">Jelka</a>. His first successes came in Germany in the late 1890s; it was not until 1907 that his music regularly appeared in British concerts. <a href=\"/wiki/Thomas_Beecham\" title=\"Thomas Beecham\">Thomas Beecham</a> conducted the full premiere of <i><a href=\"/wiki/A_Mass_of_Life\" title=\"A Mass of Life\">A Mass of Life</a></i> in London in 1909, staged the opera <i><a href=\"/wiki/A_Village_Romeo_and_Juliet\" title=\"A Village Romeo and Juliet\">A Village Romeo and Juliet</a></i> at <a href=\"/wiki/Royal_Opera_House\" title=\"Royal Opera House\">Covent Garden</a> in 1910, mounted a six-day Delius festival in London in 1929, and made gramophone recordings of many works. After 1918 Delius began to suffer the effects of <a href=\"/wiki/Syphilis\" title=\"Syphilis\">syphilis</a>, became paralysed and blind, but completed some late compositions with the aid of <a href=\"/wiki/Eric_Fenby\" title=\"Eric Fenby\">Eric Fenby</a>. His early compositions reflect the music he had heard in America and Europe; later he developed a style uniquely his own. The Delius Society, formed in 1962, promotes knowledge of his life and works, and sponsors an annual competition for young musicians. (<a href=\"/wiki/Frederick_Delius\" title=\"Frederick Delius\"><b>Full article...</b></a>)</p>\n" +
                        "<p>Recently featured: <i><a href=\"/wiki/Thopha_saccata\" title=\"Thopha saccata\">Thopha saccata</a></i> – <a href=\"/wiki/Erschallet,_ihr_Lieder,_erklinget,_ihr_Saiten!_BWV_172\" title=\"Erschallet, ihr Lieder, erklinget, ihr Saiten! BWV 172\"><i>Erschallet, ihr Lieder, erklinget, ihr Saiten!</i> BWV 172</a> – <a href=\"/wiki/Wells_Cathedral\" title=\"Wells Cathedral\">Wells Cathedral</a></p>\n" +
                        "<div style=\"text-align: right;\" class=\"noprint\"><b><a href=\"/wiki/Wikipedia:Today%27s_featured_article/June_2014\" title=\"Wikipedia:Today's featured article/June 2014\">Archive</a></b> – <b><a href=\"https://lists.wikimedia.org/mailman/listinfo/daily-article-l\" class=\"extiw\" title=\"mail:daily-article-l\">By email</a></b> – <b><a href=\"/wiki/Wikipedia:Featured_articles\" title=\"Wikipedia:Featured articles\">More featured articles...</a></b></div>\n" +
                        "</div><h2>In the news</h2><div id=\"mp-itn\">\n" +
                        "<div style=\"float:right;margin-left:0.5em;\"><a href=\"/wiki/File:Maria_Sharapova,_December_2008.jpg\" class=\"image\" title=\"Maria Sharapova\">\n" +
                        "<img alt=\"Maria Sharapova in 2008\" src=\"//upload.wikimedia.org/wikipedia/en/thumb/c/c6/Maria_Sharapova%2C_December_2008.jpg/61px-Maria_Sharapova%2C_December_2008.jpg\" width=\"61\" height=\"100\" class=\"thumbborder\" srcset=\"//upload.wikimedia.org/wikipedia/en/thumb/c/c6/Maria_Sharapova%2C_December_2008.jpg/91px-Maria_Sharapova%2C_December_2008.jpg 1.5x, //upload.wikimedia.org/wikipedia/en/thumb/c/c6/Maria_Sharapova%2C_December_2008.jpg/121px-Maria_Sharapova%2C_December_2008.jpg 2x\" data-file-width=\"405\" \n" +
                        "data-file-height=\"667\"></a></div>\n" +
                        "<ul><li>Militants from the <a href=\"/wiki/Islamic_State_of_Iraq_and_the_Levant\" title=\"Islamic State of Iraq and the Levant\">Islamic State of Iraq and the Levant</a> <a href=\"/wiki/2014_Mosul_offensive\" title=\"2014 Mosul offensive\"><b>capture</b></a> <a href=\"/wiki/Mosul\" title=\"Mosul\">Mosul</a>, Iraq.</li>\n" +
                        "<li>In <a href=\"/wiki/Tennis\" title=\"Tennis\">tennis</a>, the <a href=\"/wiki/2014_French_Open\" title=\"2014 French Open\">French Open</a> concludes with <a href=\"/wiki/Rafael_Nadal\" title=\"Rafael Nadal\">Rafael Nadal</a> winning the <b><a href=\"/wiki/2014_French_Open_%E2%80%93_Men%27s_Singles\" title=\"2014 French Open – Men's Singles\">men's singles</a></b> and <a href=\"/wiki/Maria_Sharapova\" title=\"Maria Sharapova\">Maria Sharapova</a> <i>(pictured)</i> winning the <b><a href=\"/wiki/2014_French_Open_%E2%80%93_Women%27s_Singles\" title=\"2014 French Open – Women's Singles\">women's singles</a></b>.</li>\n" +
                        "<li>At least 36 people are killed in <b><a href=\"/wiki/2014_Jinnah_International_Airport_attack\" title=\"2014 Jinnah International Airport attack\">an attack</a></b> on <a href=\"/wiki/Jinnah_International_Airport\" title=\"Jinnah International Airport\">Jinnah International Airport</a> in <a href=\"/wiki/Karachi\" title=\"Karachi\">Karachi</a>, Pakistan.</li>\n" +
                        "<li>In <a href=\"/wiki/Horse_racing\" title=\"Horse racing\">horse racing</a>, <a href=\"/wiki/Australia_(horse)\" title=\"Australia (horse)\">Australia</a> wins the <b><a href=\"/wiki/2014_Epsom_Derby\" title=\"2014 Epsom Derby\">Epsom Derby</a></b> and <a href=\"/wiki/Tonalist\" title=\"Tonalist\">Tonalist</a> wins the <b><a href=\"/wiki/2014_Belmont_Stakes\" title=\"2014 Belmont Stakes\">Belmont Stakes</a></b>.</li>\n" +
                        "<li>Around 35 people are killed in <b><a href=\"/wiki/2014_South_Kivu_attack\" title=\"2014 South Kivu attack\">an attack</a></b> on a village in <a href=\"/wiki/South_Kivu\" title=\"South Kivu\">South Kivu</a>, Democratic Republic of the Congo.</li>\n" +
                        "<li>The <a href=\"/wiki/European_Central_Bank\" title=\"European Central Bank\">European Central Bank</a> <b><a href=\"/wiki/Eurozone_crisis#European_Central_Bank\" title=\"Eurozone crisis\">cuts</a></b> the main interest rate to 0.15% and sets the deposit rate at −0.10% in an attempt to stimulate the <a href=\"/wiki/Eurozone\" title=\"Eurozone\">eurozone</a> economy.</li>\n" +
                        "<li>The <a href=\"/wiki/Government_of_Ireland\" title=\"Government of Ireland\">Irish government</a> investigates <b><a href=\"/wiki/Bon_Secours_Mother_and_Baby_Home#Burial_ground\" title=\"Bon Secours Mother and Baby Home\">a mass grave</a></b> discovered at a former children's home in <a href=\"/wiki/Tuam\" title=\"Tuam\">Tuam</a>, Ireland.</li>\n" +
                        "</ul><p><b><a href=\"/wiki/Portal:Current_events\" title=\"Portal:Current events\">Ongoing</a></b>: <span class=\"nowrap\"><a href=\"/wiki/2014_pro-Russian_conflict_in_Ukraine\" title=\"2014 pro-Russian conflict in Ukraine\">Ukrainian conflict</a></span><br><b><a href=\"/wiki/Deaths_in_2014\" title=\"Deaths in 2014\">Recent deaths</a></b>: <span class=\"nowrap\"><a href=\"/wiki/Rik_Mayall\" title=\"Rik Mayall\">Rik Mayall</a> –</span> <span class=\"nowrap\"><a href=\"/wiki/Svyatoslav_Belza\" title=\"Svyatoslav Belza\">Svyatoslav Belza</a></span></p>\n" +
                        "</div></div>"
        );
        ImageUrlMap imageUrlMap = builder.build();
        assertEquals(2, imageUrlMap.size());
    }
}
