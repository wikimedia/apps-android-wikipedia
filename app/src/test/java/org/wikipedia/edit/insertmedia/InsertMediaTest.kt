package org.wikipedia.edit.insertmedia

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InsertMediaTest {
    @Before
    fun setUp() {
        InsertMediaViewModel.initMagicWords()
    }

    @Test
    fun testInsertImageIntoArticleWithoutInfobox() {
        val wikitext = "'''Gabrielle de Bourbon''' or '''Gabrielle de Bourbon-Montpensier''' " +
                "(c.[[1447]]–30 November [[1516]]), princess of [[Talmont-Saint-Hilaire|Talmont]], " +
                "was a French [[author]] and daughter of the [[House of Bourbon]].\n" +
                "\n== Biography ==\nShe was the oldest daughter of [[Louis I, Count of Montpensier]] " +
                "and [[Gabrielle de La Tour d'Auvergne]].<ref name=\":0\">{{Cite web |title=Gabrielle " +
                "de Bourbon-Montpensier — SiefarWikiFr |url=http://siefar.org/dictionnaire/fr/Gabrielle_de_Bourbon-Montpensier" +
                " |access-date=2021-07-21 |website=siefar.org}}</ref>\n"

        val expected = "[[File:Test_image.jpg|thumb|right|alt=Bar|Foo]]\n'''Gabrielle de Bourbon''' or '''Gabrielle de Bourbon-Montpensier''' " +
                "(c.[[1447]]–30 November [[1516]]), princess of [[Talmont-Saint-Hilaire|Talmont]], " +
                "was a French [[author]] and daughter of the [[House of Bourbon]].\n" +
                "\n== Biography ==\nShe was the oldest daughter of [[Louis I, Count of Montpensier]] " +
                "and [[Gabrielle de La Tour d'Auvergne]].<ref name=\":0\">{{Cite web |title=Gabrielle " +
                "de Bourbon-Montpensier — SiefarWikiFr |url=http://siefar.org/dictionnaire/fr/Gabrielle_de_Bourbon-Montpensier" +
                " |access-date=2021-07-21 |website=siefar.org}}</ref>\n"

        MatcherAssert.assertThat(InsertMediaViewModel.insertImageIntoWikiText("en", wikitext, "Test_image.jpg", "Foo",
            "Bar", InsertMediaViewModel.IMAGE_SIZE_DEFAULT, InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL, InsertMediaViewModel.IMAGE_POSITION_RIGHT,
            0, true), Matchers.`is`(expected))
    }

    @Test
    fun testInsertImageIntoArticleWithHatnotes() {
        val wikitext = "{{HatnoteTemplate}}\n" +
                "{{Short description|Example description}}\n" +
                "'''Gabrielle de Bourbon''' or '''Gabrielle de Bourbon-Montpensier''' " +
                "(c.[[1447]]–30 November [[1516]]), princess of [[Talmont-Saint-Hilaire|Talmont]], " +
                "was a French [[author]] and daughter of the [[House of Bourbon]].\n" +
                "\n== Biography ==\nShe was the oldest daughter of [[Louis I, Count of Montpensier]] " +
                "and [[Gabrielle de La Tour d'Auvergne]].<ref name=\":0\">{{Cite web |title=Gabrielle " +
                "de Bourbon-Montpensier — SiefarWikiFr |url=http://siefar.org/dictionnaire/fr/Gabrielle_de_Bourbon-Montpensier" +
                " |access-date=2021-07-21 |website=siefar.org}}</ref>\n"

        val expected = "{{HatnoteTemplate}}\n" +
                "{{Short description|Example description}}\n" +
                "[[File:Test_image.jpg|thumb|right|alt=Bar|Foo]]\n'''Gabrielle de Bourbon''' or '''Gabrielle de Bourbon-Montpensier''' " +
                "(c.[[1447]]–30 November [[1516]]), princess of [[Talmont-Saint-Hilaire|Talmont]], " +
                "was a French [[author]] and daughter of the [[House of Bourbon]].\n" +
                "\n== Biography ==\nShe was the oldest daughter of [[Louis I, Count of Montpensier]] " +
                "and [[Gabrielle de La Tour d'Auvergne]].<ref name=\":0\">{{Cite web |title=Gabrielle " +
                "de Bourbon-Montpensier — SiefarWikiFr |url=http://siefar.org/dictionnaire/fr/Gabrielle_de_Bourbon-Montpensier" +
                " |access-date=2021-07-21 |website=siefar.org}}</ref>\n"

        MatcherAssert.assertThat(InsertMediaViewModel.insertImageIntoWikiText("en", wikitext, "Test_image.jpg", "Foo",
            "Bar", InsertMediaViewModel.IMAGE_SIZE_DEFAULT, InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL, InsertMediaViewModel.IMAGE_POSITION_RIGHT,
            0, true), Matchers.`is`(expected))
    }

    @Test
    fun testInsertImageIntoArticleWithBrokenSyntax() {
        val wikitext = "{{Invalid template}\n" +
                "'''Gabrielle de Bourbon''' or '''Gabrielle de Bourbon-Montpensier''' " +
                "(c.[[1447]]–30 November [[1516]]), princess of [[Talmont-Saint-Hilaire|Talmont]], " +
                "was a French [[author]] and daughter of the [[House of Bourbon]].\n" +
                "\n== Biography ==\nShe was the oldest daughter of [[Louis I, Count of Montpensier]] " +
                "and [[Gabrielle de La Tour d'Auvergne]].<ref name=\":0\">{{Cite web |title=Gabrielle " +
                "de Bourbon-Montpensier — SiefarWikiFr |url=http://siefar.org/dictionnaire/fr/Gabrielle_de_Bourbon-Montpensier" +
                " |access-date=2021-07-21 |website=siefar.org}}</ref>\n"

        val expected = "[[File:Test_image.jpg|thumb|right|alt=Bar|Foo]]\n" +
                "{{Invalid template}\n" +
                "'''Gabrielle de Bourbon''' or '''Gabrielle de Bourbon-Montpensier''' " +
                "(c.[[1447]]–30 November [[1516]]), princess of [[Talmont-Saint-Hilaire|Talmont]], " +
                "was a French [[author]] and daughter of the [[House of Bourbon]].\n" +
                "\n== Biography ==\nShe was the oldest daughter of [[Louis I, Count of Montpensier]] " +
                "and [[Gabrielle de La Tour d'Auvergne]].<ref name=\":0\">{{Cite web |title=Gabrielle " +
                "de Bourbon-Montpensier — SiefarWikiFr |url=http://siefar.org/dictionnaire/fr/Gabrielle_de_Bourbon-Montpensier" +
                " |access-date=2021-07-21 |website=siefar.org}}</ref>\n"

        MatcherAssert.assertThat(InsertMediaViewModel.insertImageIntoWikiText("en", wikitext, "Test_image.jpg", "Foo",
            "Bar", InsertMediaViewModel.IMAGE_SIZE_DEFAULT, InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL, InsertMediaViewModel.IMAGE_POSITION_RIGHT,
            0, true), Matchers.`is`(expected))
    }

    @Test
    fun testInsertImageIntoArticleWithImageButNotCaption() {
        val wikitext = "{{short description|Greek actor}}\n" +
                "{{Use dmy dates|date=March 2020}}\n" +
                "{{more footnotes|date=January 2013}}\n" +
                "{{Infobox person\n" +
                "|image       = \n" +
                "|name        = Giorgos Velentzas<br>''Γιώργος Βελέντζας''\n" +
                "|birth_date  = {{birth date|1927|12|4|df=y}}\n" +
                "|birth_place = [[Athens]], [[Greece]]\n" +
                "|death_date  = {{death date and age|2015|7|20|1927|12|4|df=y}}\n" +
                "|death_place = \n" +
                "|occupation  = [[actor]]\n" +
                "|awards      = [[Thessaloniki Film Festival|Thessaloniki Film Festival 1993]] for ''[[Zoi charissameni]]''\n" +
                "}}\n\n" +
                "'''Giorgos Velentzas''' ({{lang-el|Γιώργος Βελέντζας}}; 4 December 1927 – 20 July 2015)<ref>" +
                "{{Cite web|url=https://www.imdb.com/name/nm0892437/bio?ref_=nm_ov_bio_sm|title=Giorgos Velentzas|website=[[IMDb]]}}</ref> was a [[Greece|Greek]] actor."

        val expected = "{{short description|Greek actor}}\n" +
                "{{Use dmy dates|date=March 2020}}\n" +
                "{{more footnotes|date=January 2013}}\n" +
                "{{Infobox person\n" +
                "|image       = Test_image.jpg\n" +
                "|name        = Giorgos Velentzas<br>''Γιώργος Βελέντζας''\n" +
                "|alt = Bar\n" +
                "|caption = Foo\n" +
                "|birth_date  = {{birth date|1927|12|4|df=y}}\n" +
                "|birth_place = [[Athens]], [[Greece]]\n" +
                "|death_date  = {{death date and age|2015|7|20|1927|12|4|df=y}}\n" +
                "|death_place = \n" +
                "|occupation  = [[actor]]\n" +
                "|awards      = [[Thessaloniki Film Festival|Thessaloniki Film Festival 1993]] for ''[[Zoi charissameni]]''\n" +
                "}}\n\n" +
                "'''Giorgos Velentzas''' ({{lang-el|Γιώργος Βελέντζας}}; 4 December 1927 – 20 July 2015)<ref>" +
                "{{Cite web|url=https://www.imdb.com/name/nm0892437/bio?ref_=nm_ov_bio_sm|title=Giorgos Velentzas|website=[[IMDb]]}}</ref> was a [[Greece|Greek]] actor."

        MatcherAssert.assertThat(InsertMediaViewModel.insertImageIntoWikiText("en", wikitext, "Test_image.jpg", "Foo",
            "Bar", InsertMediaViewModel.IMAGE_SIZE_DEFAULT, InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL, InsertMediaViewModel.IMAGE_POSITION_RIGHT,
            0, true), Matchers.`is`(expected))
    }

    @Test
    fun testInsertImageIntoArticleWithInfoboxWithName() {
        val wikitext = "{{short description|Species of beetle}}\n" +
                "{{Automatic taxobox\n" +
                "| genus = Carabus\n" +
                "| species = goryi\n" +
                "| authority = Dejean, 1831\n" +
                "}}\n\n" +
                "'''''Carabus goryi''''' is a species of [[ground beetle]] in the family [[Carabidae]]." +
                "It is found in North America.<ref name=itis/><ref name=gbif/><ref name=buglink/><ref" +
                "name=Bousquet2012/>\n"

        val expected = "{{short description|Species of beetle}}\n" +
                "{{Automatic taxobox\n" +
                "| genus = Carabus\n" +
                "| species = goryi\n" +
                "| image_alt = Bar\n" +
                "| image_caption = Foo\n" +
                "| image = Test_image.jpg\n" +
                "| authority = Dejean, 1831\n" +
                "}}\n\n" +
                "'''''Carabus goryi''''' is a species of [[ground beetle]] in the family [[Carabidae]]." +
                "It is found in North America.<ref name=itis/><ref name=gbif/><ref name=buglink/><ref" +
                "name=Bousquet2012/>\n"

        MatcherAssert.assertThat(InsertMediaViewModel.insertImageIntoWikiText("en", wikitext, "Test_image.jpg", "Foo",
            "Bar", InsertMediaViewModel.IMAGE_SIZE_DEFAULT, InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL, InsertMediaViewModel.IMAGE_POSITION_RIGHT,
            0, true), Matchers.`is`(expected))
    }

    @Test
    fun testInsertImageIntoArticleWithInfoboxWithExistingImage() {
        val wikitext = "{{short description|Species of beetle}}\n" +
                "{{Speciesbox\n" +
                "| genus = Carabus\n" +
                "| species = goryi\n" +
                "| image = Test_image.jpg\n" +
                "| authority = Dejean, 1831\n" +
                "}}\n\n" +
                "'''''Carabus goryi''''' is a species of [[ground beetle]] in the family [[Carabidae]]." +
                "It is found in North America.<ref name=itis/><ref name=gbif/><ref name=buglink/><ref" +
                "name=Bousquet2012/>\n"

        val expected = "{{short description|Species of beetle}}\n" +
                "{{Speciesbox\n" +
                "| genus = Carabus\n" +
                "| species = goryi\n" +
                "| image = Test_image.jpg\n" +
                "| authority = Dejean, 1831\n" +
                "}}\n\n" +
                "[[File:Test_image.jpg|thumb|right|alt=Bar|Foo]]\n" +
                "'''''Carabus goryi''''' is a species of [[ground beetle]] in the family [[Carabidae]]." +
                "It is found in North America.<ref name=itis/><ref name=gbif/><ref name=buglink/><ref" +
                "name=Bousquet2012/>\n"

        MatcherAssert.assertThat(InsertMediaViewModel.insertImageIntoWikiText("en", wikitext, "Test_image.jpg", "Foo",
            "Bar", InsertMediaViewModel.IMAGE_SIZE_DEFAULT, InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL, InsertMediaViewModel.IMAGE_POSITION_RIGHT,
            0, true), Matchers.`is`(expected))
    }

    @Test
    fun testInsertImageIntoArticleWithInfoboxEmptyImageParam() {
        val wikitext = "{{Short description|Genus of plants}}\n" +
                "{{Italic title}}\n" +
                "{{taxobox\n" +
                "|name = \n" +
                "|image = \n" +
                "|image_caption = \n" +
                "|regnum = [[Plantae]]\n" +
                "|unranked_divisio = [[Angiosperms]]\n" +
                "|unranked_classis = [[Eudicots]]\n" +
                "|unranked_ordo = [[Asterids]]\n" +
                "|ordo = [[Gentianales]]\n" +
                "|familia = [[Apocynaceae]]\n" +
                "|subfamilia = [[Asclepiadoideae]]\n" +
                "|genus = '''''Calostigma'''''\n" +
                "|genus_authority = [[Decne.]] 1838 not Schott 1832, the latter name published " +
                "without description<ref name=r>[http://www.ipni.org/ipni/idPlantNameSearch.do?id=86526-1 " +
                "International Plant Names Index, Calostigma imbe Schott]</ref>\n" +
                "|synonyms_ref=\n" +
                "|synonyms=\n" +
                "}}\n\n" +
                "'''''Calostigma''''' is a genus of flowering plants in the family [[Apocynaceae]], " +
                "first described as a genus in 1838.<ref>Decaisne, Joseph. 1838.  Annales des Sciences " +
                "Naturelles; Botanique, sér. 2 9: 343–4, t. 12H</ref><ref>[http://www.tropicos.org/Name/40035627 " +
                "Tropicos, genus ''Calostigma'' Decne.]</ref> The genus is native to [[South America]]."

        val expected = "{{Short description|Genus of plants}}\n" +
                "{{Italic title}}\n" +
                "{{taxobox\n" +
                "|name = \n" +
                "|image_alt = Bar\n" +
                "|image = Test_image.jpg\n" +
                "|image_caption = Foo\n" +
                "|regnum = [[Plantae]]\n" +
                "|unranked_divisio = [[Angiosperms]]\n" +
                "|unranked_classis = [[Eudicots]]\n" +
                "|unranked_ordo = [[Asterids]]\n" +
                "|ordo = [[Gentianales]]\n" +
                "|familia = [[Apocynaceae]]\n" +
                "|subfamilia = [[Asclepiadoideae]]\n" +
                "|genus = '''''Calostigma'''''\n" +
                "|genus_authority = [[Decne.]] 1838 not Schott 1832, the latter name published " +
                "without description<ref name=r>[http://www.ipni.org/ipni/idPlantNameSearch.do?id=86526-1 " +
                "International Plant Names Index, Calostigma imbe Schott]</ref>\n" +
                "|synonyms_ref=\n" +
                "|synonyms=\n" +
                "}}\n\n" +
                "'''''Calostigma''''' is a genus of flowering plants in the family [[Apocynaceae]], " +
                "first described as a genus in 1838.<ref>Decaisne, Joseph. 1838.  Annales des Sciences " +
                "Naturelles; Botanique, sér. 2 9: 343–4, t. 12H</ref><ref>[http://www.tropicos.org/Name/40035627 " +
                "Tropicos, genus ''Calostigma'' Decne.]</ref> The genus is native to [[South America]]."

        MatcherAssert.assertThat(InsertMediaViewModel.insertImageIntoWikiText("en", wikitext, "Test_image.jpg", "Foo",
            "Bar", InsertMediaViewModel.IMAGE_SIZE_DEFAULT, InsertMediaViewModel.IMAGE_TYPE_THUMBNAIL, InsertMediaViewModel.IMAGE_POSITION_RIGHT,
            0, true), Matchers.`is`(expected))
    }
}
