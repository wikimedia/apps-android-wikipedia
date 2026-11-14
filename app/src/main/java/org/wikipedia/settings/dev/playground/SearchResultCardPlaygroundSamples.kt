package org.wikipedia.settings.dev.playground

import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchResult

// Sample search results for the search result card playground, for layout reference only. They are not
// live search results, the article text and figures are illustrative, and the thumbnails are blank gray fills.
// They run through the real mapper, so the cards follow the same rules as real results. Every wiki has
// the same eight cases, in the same order.
object SearchResultCardPlaygroundSamples {
    val languageCodes = listOf("en", "fr", "ar", "ja")

    // Cards whose attribution counts are shown as still loading.
    val attributionLoadingPageIds = setOf(8)

    fun results(languageCode: String): List<SearchResult> {
        val wikiSite = WikiSite.forLanguageCode(languageCode)
        return CASES.zip(COPY.getValue(languageCode)).map { (case, copy) ->
            val pageTitle = PageTitle(copy.title, wikiSite).apply {
                description = copy.description
                thumbUrl = case.thumbUrl
            }
            val isSemantic = case.type == SearchResult.SearchResultType.SEMANTIC
            SearchResult(
                pageTitle = pageTitle,
                redirectFrom = null,
                type = case.type,
                snippet = copy.text.takeIf { isSemantic },
                sectionTitle = copy.section,
                editCounts = case.editCounts,
                referenceCounts = case.referenceCounts,
                pageId = case.pageId,
                leadText = copy.text.takeIf { !isSemantic }
            )
        }
    }

    private data class Case(
        val pageId: Int,
        val type: SearchResult.SearchResultType,
        val thumbUrl: String?,
        val editCounts: Int?,
        val referenceCounts: Int?
    )

    private data class Copy(
        val title: String,
        val description: String?,
        val section: String?,
        val text: String
    )

    // Not a real image: it has no network scheme, so the image fails to load straight away and the card shows
    // its gray placeholder fill. This keeps the thumbnail slot without using real Commons images.
    private const val THUMB = "playground://thumbnail"

    private val SEMANTIC = SearchResult.SearchResultType.SEMANTIC
    private val PREFIX = SearchResult.SearchResultType.PREFIX
    private val FULL_TEXT = SearchResult.SearchResultType.FULL_TEXT

    private val CASES = listOf(
        // 1. Meaning match in a section, with a thumbnail.
        Case(1, SEMANTIC, THUMB, editCounts = 2348, referenceCounts = 35),
        // 2. Title match (lead), with a thumbnail.
        Case(2, PREFIX, THUMB, editCounts = 112, referenceCounts = 9),
        // 3. Meaning match in the lead, no thumbnail: text spans the full width.
        Case(3, SEMANTIC, null, editCounts = 640, referenceCounts = 0),
        // 4. Every line limit: long title, description, trail and excerpt.
        Case(4, SEMANTIC, THUMB, editCounts = 57, referenceCounts = 14),
        // 5. Disambiguation page: empty lead, so no excerpt.
        Case(5, PREFIX, null, editCounts = 23, referenceCounts = 0),
        // 6. Passage drawn from a table.
        Case(6, SEMANTIC, THUMB, editCounts = 88, referenceCounts = 21),
        // 7. Full-text title match, reference count missing for this article only.
        Case(7, FULL_TEXT, null, editCounts = 31, referenceCounts = null),
        // 8. Attribution counts still loading.
        Case(8, PREFIX, THUMB, editCounts = null, referenceCounts = null)
    )

    private val COPY = mapOf(
        "en" to listOf(
            Copy("Eiffel Tower", "Wrought-iron lattice tower in Paris, France", "Design",
                "The tower is built from puddled iron and stands on four lattice legs that curve inward and meet at the first platform. " +
                        "The curve of the legs was chosen to resist the wind, following the forces acting on the structure."),
            Copy("Eiffel Tower (Delaunay, 1911)", "Painting by Robert Delaunay", null,
                "Eiffel Tower is a 1911 painting by the French artist Robert Delaunay. It belongs to a series in which he broke the tower " +
                        "into overlapping planes of colour, showing it from several angles at once."),
            Copy("History of the Eiffel Tower", null, null,
                "Work on the tower began in January 1887 and was finished in March 1889, in time for the Exposition Universelle. " +
                        "When it opened it was the tallest structure in the world, a title it kept for more than forty years."),
            Copy("Eiffel Tower (Robert Delaunay, 1926), held by the Museum of Modern Art of the City of Paris",
                "Oil painting by Robert Delaunay from his later Eiffel Tower series, shown at the Museum of Modern Art of the City of Paris",
                "Reception, exhibitions and influence on later painters of the modern city",
                "Delaunay returned to the tower in the 1920s, after more than a decade away from the subject. The later paintings are calmer " +
                        "than the series of 1910 and 1911: the tower stands upright, its structure is easier to read, and the colours are " +
                        "brighter. Critics at the time saw the change as a move from breaking the city apart to celebrating it. The 1926 " +
                        "painting was shown in several exhibitions in Paris before entering the museum's collection, and it is often " +
                        "cited as an influence on painters who took the modern city as their subject. Historians have also pointed to the " +
                        "role of photography, aerial views and cinema in shaping how Delaunay framed the tower in these later works."),
            Copy("Eiffel Tower (disambiguation)", "Wikimedia disambiguation page", null, ""),
            Copy("Eiffel Tower visitor numbers", "Statistics on visits to the Eiffel Tower", "Annual visitors",
                "Year | Visitors | Notes 1889 | 1,896,987 | Exposition Universelle 1900 | 1,024,887 | Exposition Universelle " +
                        "2007 | 6,893,000 | Highest number recorded"),
            Copy("The Eiffel Tower (short story)", "Short story by Dino Buzzati", null,
                "\"The Eiffel Tower\" is a short story by the Italian writer Dino Buzzati, in which a worker on the tower imagines it " +
                        "rising far beyond its planned height."),
            Copy("Eiffel Tower Operating Company", "Company that runs the Eiffel Tower", null,
                "The Eiffel Tower Operating Company manages the tower on behalf of the City of Paris, including ticketing, maintenance " +
                        "and visitor services.")
        ),
        "fr" to listOf(
            Copy("Tour Eiffel", "Tour de fer puddlé de 330 mètres à Paris, France", "Conception",
                "La tour est construite en fer puddlé et repose sur quatre piliers en treillis qui se rejoignent au premier étage. " +
                        "La courbe des piliers a été choisie pour résister au vent, en suivant les forces qui s'exercent sur la structure."),
            Copy("Tour Eiffel (Delaunay, 1911)", "Tableau de Robert Delaunay", null,
                "Tour Eiffel est un tableau peint en 1911 par l'artiste français Robert Delaunay. Il appartient à une série dans " +
                        "laquelle la tour est décomposée en plans de couleur qui se superposent, vue sous plusieurs angles à la fois."),
            Copy("Histoire de la tour Eiffel", null, null,
                "Les travaux commencent en janvier 1887 et s'achèvent en mars 1889, à temps pour l'Exposition universelle. " +
                        "À son ouverture, la tour est la plus haute structure du monde, un titre qu'elle garde plus de quarante ans."),
            Copy("Tour Eiffel (Robert Delaunay, 1926), conservée au musée d'Art moderne de la Ville de Paris",
                "Huile sur toile de Robert Delaunay, issue de sa dernière série consacrée à la tour Eiffel et exposée au musée d'Art moderne de Paris",
                "Réception, expositions et influence sur les peintres de la ville moderne",
                "Delaunay revient à la tour dans les années 1920, après plus de dix ans loin du sujet. Les tableaux de cette période " +
                        "sont plus calmes que ceux de 1910 et 1911 : la tour est droite, sa structure se lit mieux et les couleurs sont " +
                        "plus vives. Les critiques de l'époque y voient le passage d'une ville éclatée à une ville célébrée. Le tableau " +
                        "de 1926 est présenté dans plusieurs expositions à Paris avant d'entrer dans les collections du musée, et il est " +
                        "souvent cité comme une influence pour les peintres de la ville moderne. Les historiens soulignent aussi le rôle " +
                        "de la photographie, des vues aériennes et du cinéma dans la façon dont Delaunay cadre la tour."),
            Copy("Tour Eiffel (homonymie)", "Page d'homonymie de Wikimédia", null, ""),
            Copy("Fréquentation de la tour Eiffel", "Statistiques des visites de la tour Eiffel", "Visiteurs par année",
                "Année | Visiteurs | Remarques 1889 | 1 896 987 | Exposition universelle 1900 | 1 024 887 | Exposition universelle " +
                        "2007 | 6 893 000 | Record de fréquentation"),
            Copy("La Tour Eiffel (nouvelle)", "Nouvelle de Dino Buzzati", null,
                "« La Tour Eiffel » est une nouvelle de l'écrivain italien Dino Buzzati, dans laquelle un ouvrier du chantier " +
                        "imagine la tour s'élever bien au-delà de la hauteur prévue."),
            Copy("Société d'exploitation de la tour Eiffel", "Société publique locale", null,
                "La Société d'exploitation de la tour Eiffel gère le monument pour le compte de la Ville de Paris, notamment la " +
                        "billetterie, l'entretien et l'accueil des visiteurs.")
        ),
        "ar" to listOf(
            Copy("برج إيفل", "برج شبكي من الحديد المطاوع في باريس، فرنسا", "التصميم",
                "بُني البرج من الحديد المطاوع ويقوم على أربع قوائم شبكية تنحني إلى الداخل وتلتقي عند الطابق الأول. " +
                        "اختير انحناء القوائم لمقاومة الرياح، تبعًا للقوى المؤثرة في البنية."),
            Copy("برج إيفل (ديلوناي، 1911)", "لوحة للرسام روبرت ديلوناي", null,
                "برج إيفل لوحة رسمها الفنان الفرنسي روبرت ديلوناي عام 1911، وهي جزء من سلسلة فكّك فيها البرج إلى مساحات " +
                        "لونية متداخلة، وأظهره من زوايا عدة في آن واحد."),
            Copy("تاريخ برج إيفل", null, null,
                "بدأ العمل في البرج في يناير 1887 وانتهى في مارس 1889، في الوقت المناسب للمعرض العالمي. وعند افتتاحه كان " +
                        "أعلى بناء في العالم، واحتفظ بهذا اللقب أكثر من أربعين عامًا."),
            Copy("برج إيفل (روبرت ديلوناي، 1926)، المحفوظة في متحف الفن الحديث لمدينة باريس",
                "لوحة زيتية لروبرت ديلوناي من سلسلته المتأخرة عن برج إيفل، معروضة في متحف الفن الحديث لمدينة باريس",
                "الاستقبال والمعارض وتأثيرها في رسامي المدينة الحديثة لاحقًا",
                "عاد ديلوناي إلى البرج في عشرينيات القرن العشرين بعد أكثر من عقد بعيدًا عن الموضوع. وتبدو لوحات هذه المرحلة " +
                        "أهدأ من سلسلة 1910 و1911، إذ يقف البرج مستقيمًا وتسهل قراءة بنيته وتزداد الألوان إشراقًا. ورأى نقاد تلك " +
                        "الفترة في هذا التحول انتقالًا من تفكيك المدينة إلى الاحتفاء بها. وعُرضت لوحة 1926 في عدة معارض في باريس " +
                        "قبل أن تدخل مجموعة المتحف، وكثيرًا ما تُذكر بوصفها مؤثرًا في الرسامين الذين اتخذوا المدينة الحديثة " +
                        "موضوعًا لهم. ويشير المؤرخون أيضًا إلى دور التصوير الفوتوغرافي والمناظر الجوية والسينما في طريقة تأطيره للبرج."),
            Copy("برج إيفل (توضيح)", "صفحة توضيح في ويكيميديا", null, ""),
            Copy("أعداد زوار برج إيفل", "إحصاءات زيارات برج إيفل", "الزوار سنويًا",
                "السنة | الزوار | ملاحظات 1889 | 1٬896٬987 | المعرض العالمي 1900 | 1٬024٬887 | المعرض العالمي " +
                        "2007 | 6٬893٬000 | أعلى عدد مسجل"),
            Copy("برج إيفل (قصة قصيرة)", "قصة قصيرة للكاتب دينو بوتزاتي", null,
                "«برج إيفل» قصة قصيرة للكاتب الإيطالي دينو بوتزاتي، يتخيل فيها عامل في بناء البرج أنه يرتفع إلى ما هو أبعد " +
                        "بكثير من الارتفاع المخطط له."),
            Copy("شركة تشغيل برج إيفل", "الشركة التي تدير برج إيفل", null,
                "تدير شركة تشغيل برج إيفل البرج نيابة عن مدينة باريس، بما في ذلك بيع التذاكر والصيانة وخدمات الزوار.")
        ),
        "ja" to listOf(
            Copy("エッフェル塔", "フランス・パリにある錬鉄製の格子塔", "設計",
                "塔はパドル鉄で造られ、内側に湾曲する4本の格子状の脚が第1展望台で一つになる。脚の曲線は、構造にかかる力に" +
                        "沿うように、風に耐える形として選ばれた。"),
            Copy("エッフェル塔 (ドローネー、1911年)", "ロベール・ドローネーの絵画", null,
                "『エッフェル塔』は、フランスの画家ロベール・ドローネーが1911年に描いた絵画である。塔を重なり合う色面に分解し、" +
                        "複数の角度から同時に描いた連作の一つ。"),
            Copy("エッフェル塔の歴史", null, null,
                "塔の工事は1887年1月に始まり、パリ万国博覧会に間に合うよう1889年3月に完成した。開業当時は世界一高い建造物で、" +
                        "その座を40年以上保った。"),
            Copy("エッフェル塔 (ロベール・ドローネー、1926年) - パリ市立近代美術館所蔵の油彩画",
                "ロベール・ドローネーによる後期のエッフェル塔連作の一つで、パリ市立近代美術館に展示されている油彩画",
                "評価、展覧会、そして後の近代都市を描く画家たちへの影響",
                "ドローネーは10年以上この主題から離れた後、1920年代に再び塔を描いた。この時期の作品は1910年から1911年の連作より" +
                        "落ち着いており、塔はまっすぐに立ち、構造が読み取りやすく、色彩も明るい。当時の批評家は、この変化を都市の" +
                        "解体から都市の賛美への移行と捉えた。1926年の作品はパリでのいくつもの展覧会で紹介された後に美術館の所蔵と" +
                        "なり、近代都市を主題とする画家たちへの影響としてしばしば挙げられる。歴史家はまた、写真や空からの眺め、" +
                        "映画が、ドローネーによる塔の構図に与えた影響も指摘している。"),
            Copy("エッフェル塔 (曖昧さ回避)", "ウィキメディアの曖昧さ回避ページ", null, ""),
            Copy("エッフェル塔の入場者数", "エッフェル塔の来場者に関する統計", "年間来場者数",
                "年 | 来場者数 | 備考 1889年 | 1,896,987 | パリ万国博覧会 1900年 | 1,024,887 | パリ万国博覧会 " +
                        "2007年 | 6,893,000 | 過去最多"),
            Copy("エッフェル塔 (短編小説)", "ディーノ・ブッツァーティの短編小説", null,
                "「エッフェル塔」は、イタリアの作家ディーノ・ブッツァーティの短編小説。塔の建設に携わる作業員が、塔が予定の" +
                        "高さをはるかに超えて伸びていく様子を思い描く。"),
            Copy("エッフェル塔運営会社", "エッフェル塔を運営する会社", null,
                "エッフェル塔運営会社は、パリ市に代わって塔を管理し、チケット販売、保守、来場者サービスなどを担っている。")
        )
    )
}
