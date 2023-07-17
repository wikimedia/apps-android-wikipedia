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
    fun testInsertImageIntoArticleWithInfoboxWithName() {
        val wikitext = "{{short description|Species of beetle}}\n" +
                "{{Speciesbox\n" +
                "| genus = Carabus\n" +
                "| species = goryi\n" +
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
                "| image_caption = Foo\n" +
                "| image_alt = Bar\n" +
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

        val expected = "[[File:Test_image.jpg|thumb|right|alt=Bar|Foo]]\n" +
                "{{short description|Species of beetle}}\n" +
                "{{Speciesbox\n" +
                "| genus = Carabus\n" +
                "| species = goryi\n" +
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
