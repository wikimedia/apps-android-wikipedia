package org.wikipedia.topics

import org.wikipedia.R

data class ArticleTopic(
    val topicId: String,
    val msgKey: Int,
    val queryTopicId: String,
    val taxonIds: List<String>
)

// the values defined here are from https://gerrit.wikimedia.org/r/plugins/gitiles/mediawiki/extensions/WikimediaMessages/+/refs/heads/master/includes/ArticleTopicFiltersRegistry.php
object ArticleTopics {
    val all = listOf(
        ArticleTopic(
            topicId = "architecture",
            msgKey = R.string.wikimedia_articletopics_topic_architecture,
            queryTopicId = "architecture",
            taxonIds = listOf("Culture.Visual_arts.Architecture")
        ),
        ArticleTopic(
            topicId = "art", // registry uses "art" as topicId but articleTopics = "visual-arts"
            msgKey = R.string.wikimedia_articletopics_topic_art,
            queryTopicId = "visual-arts",
            taxonIds = listOf("Culture.Visual_arts.Visual_arts*")
        ),
        ArticleTopic(
            topicId = "comics-and-anime",
            msgKey = R.string.wikimedia_articletopics_topic_comics_and_anime,
            queryTopicId = "comics-and-anime",
            taxonIds = listOf("Culture.Visual_arts.Comics_and_Anime")
        ),
        ArticleTopic(
            topicId = "entertainment",
            msgKey = R.string.wikimedia_articletopics_topic_entertainment,
            queryTopicId = "entertainment",
            taxonIds = listOf(
                "Culture.Media.Entertainment",
                "Culture.Media.Radio"
            )
        ),
        ArticleTopic(
            topicId = "fashion",
            msgKey = R.string.wikimedia_articletopics_topic_fashion,
            queryTopicId = "fashion",
            taxonIds = listOf("Culture.Visual_arts.Fashion")
        ),
        ArticleTopic(
            topicId = "literature",
            msgKey = R.string.wikimedia_articletopics_topic_literature,
            queryTopicId = "books", // registry uses "literature" as topicId but articleTopics = "books"
            taxonIds = listOf("Culture.Media.Books")
        ),
        ArticleTopic(
            topicId = "music",
            msgKey = R.string.wikimedia_articletopics_topic_music,
            queryTopicId = "music",
            taxonIds = listOf("Culture.Media.Music")
        ),
        ArticleTopic(
            topicId = "performing-arts",
            msgKey = R.string.wikimedia_articletopics_topic_performing_arts,
            queryTopicId = "performing-arts",
            taxonIds = listOf("Culture.Performing_arts")
        ),
        ArticleTopic(
            topicId = "sports",
            msgKey = R.string.wikimedia_articletopics_topic_sports,
            queryTopicId = "sports",
            taxonIds = listOf("Culture.Sports")
        ),
        ArticleTopic(
            topicId = "tv-and-film",
            msgKey = R.string.wikimedia_articletopics_topic_tv_and_film,
            queryTopicId = "films",
            taxonIds = listOf(
                "Culture.Media.Films",
                "Culture.Media.Television"
            )
        ),
        ArticleTopic(
            topicId = "video-games",
            msgKey = R.string.wikimedia_articletopics_topic_video_games,
            queryTopicId = "video-games",
            taxonIds = listOf("Culture.Media.Video_games")
        ),
        ArticleTopic(
            topicId = "biography",
            msgKey = R.string.wikimedia_articletopics_topic_biography,
            queryTopicId = "biography",
            taxonIds = listOf("Culture.Biography.Biography*")
        ),
        ArticleTopic(
            topicId = "women",
            msgKey = R.string.wikimedia_articletopics_topic_women,
            queryTopicId = "women",
            taxonIds = listOf("Culture.Biography.Women")
        ),
        ArticleTopic(
            topicId = "business-and-economics",
            msgKey = R.string.wikimedia_articletopics_topic_business_and_economics,
            queryTopicId = "business-and-economics",
            taxonIds = listOf("History_and_Society.Business_and_economics")
        ),
        ArticleTopic(
            topicId = "education",
            msgKey = R.string.wikimedia_articletopics_topic_education,
            queryTopicId = "education",
            taxonIds = listOf("History_and_Society.Education")
        ),
        ArticleTopic(
            topicId = "food-and-drink",
            msgKey = R.string.wikimedia_articletopics_topic_food_and_drink,
            queryTopicId = "food-and-drink",
            taxonIds = listOf("Culture.Food_and_drink")
        ),
        ArticleTopic(
            topicId = "history",
            msgKey = R.string.wikimedia_articletopics_topic_history,
            queryTopicId = "history",
            taxonIds = listOf("History_and_Society.History")
        ),
        ArticleTopic(
            topicId = "military-and-warfare",
            msgKey = R.string.wikimedia_articletopics_topic_military_and_warfare,
            queryTopicId = "military-and-warfare",
            taxonIds = listOf("History_and_Society.Military_and_warfare")
        ),
        ArticleTopic(
            topicId = "philosophy-and-religion",
            msgKey = R.string.wikimedia_articletopics_topic_philosophy_and_religion,
            queryTopicId = "philosophy-and-religion",
            taxonIds = listOf("Culture.Philosophy_and_religion")
        ),
        ArticleTopic(
            topicId = "politics-and-government",
            msgKey = R.string.wikimedia_articletopics_topic_politics_and_government,
            queryTopicId = "politics-and-government",
            taxonIds = listOf("History_and_Society.Politics_and_government")
        ),
        ArticleTopic(
            topicId = "society",
            msgKey = R.string.wikimedia_articletopics_topic_society,
            queryTopicId = "society",
            taxonIds = listOf("History_and_Society.Society")
        ),
        ArticleTopic(
            topicId = "transportation",
            msgKey = R.string.wikimedia_articletopics_topic_transportation,
            queryTopicId = "transportation",
            taxonIds = listOf("History_and_Society.Transportation")
        ),
        ArticleTopic(
            topicId = "biology",
            msgKey = R.string.wikimedia_articletopics_topic_biology,
            queryTopicId = "biology",
            taxonIds = listOf("STEM.Biology")
        ),
        ArticleTopic(
            topicId = "chemistry",
            msgKey = R.string.wikimedia_articletopics_topic_chemistry,
            queryTopicId = "chemistry",
            taxonIds = listOf("STEM.Chemistry")
        ),
        ArticleTopic(
            topicId = "computers-and-internet", // registry uses "computers-and-internet" as topicId but articleTopics = "internet-culture"
            msgKey = R.string.wikimedia_articletopics_topic_computers_and_internet,
            queryTopicId = "internet-culture",
            taxonIds = listOf(
                "Culture.Internet_culture",
                "Culture.Media.Software",
                "STEM.Computing"
            )
        ),
        ArticleTopic(
            topicId = "earth-and-environment", // registry uses "earth-and-environment" as topicId but articleTopics = "geographical"
            msgKey = R.string.wikimedia_articletopics_topic_earth_and_environment,
            queryTopicId = "geographical",
            taxonIds = listOf(
                "Geography.Geographical",
                "STEM.Earth_and_environment"
            )
        ),
        ArticleTopic(
            topicId = "engineering",
            msgKey = R.string.wikimedia_articletopics_topic_engineering,
            queryTopicId = "engineering",
            taxonIds = listOf("STEM.Engineering")
        ),
        ArticleTopic(
            topicId = "general-science", // registry uses "general-science" as topicId but articleTopics = "stem"
            msgKey = R.string.wikimedia_articletopics_topic_general_science,
            queryTopicId = "stem",
            taxonIds = listOf("STEM.STEM*")
        ),
        ArticleTopic(
            topicId = "mathematics",
            msgKey = R.string.wikimedia_articletopics_topic_mathematics,
            queryTopicId = "mathematics",
            taxonIds = listOf("STEM.Mathematics")
        ),
        ArticleTopic(
            topicId = "medicine-and-health",
            msgKey = R.string.wikimedia_articletopics_topic_medicine_and_health,
            queryTopicId = "medicine-and-health",
            taxonIds = listOf("STEM.Medicine_&_Health")
        ),
        ArticleTopic(
            topicId = "physics",
            msgKey = R.string.wikimedia_articletopics_topic_physics,
            queryTopicId = "physics",
            taxonIds = listOf(
                "STEM.Physics",
                "STEM.Space"
            )
        ),
        ArticleTopic(
            topicId = "technology",
            msgKey = R.string.wikimedia_articletopics_topic_technology,
            queryTopicId = "technology",
            taxonIds = listOf("STEM.Technology")
        ),
        ArticleTopic(
            topicId = "africa",
            msgKey = R.string.wikimedia_articletopics_topic_africa,
            queryTopicId = "africa",
            taxonIds = listOf(
                "Geography.Regions.Africa.Africa*",
                "Geography.Regions.Africa.Eastern_Africa",
                "Geography.Regions.Africa.Western_Africa",
                "Geography.Regions.Africa.Central_Africa",
                "Geography.Regions.Africa.Northern_Africa",
                "Geography.Regions.Africa.Southern_Africa"
            )
        ),
        ArticleTopic(
            topicId = "asia",
            msgKey = R.string.wikimedia_articletopics_topic_asia,
            queryTopicId = "asia",
            taxonIds = listOf(
                "Geography.Regions.Asia.Asia*",
                "Geography.Regions.Asia.North_Asia",
                "Geography.Regions.Asia.Central_Asia",
                "Geography.Regions.Asia.West_Asia",
                "Geography.Regions.Asia.South_Asia",
                "Geography.Regions.Asia.Southeast_Asia",
                "Geography.Regions.Asia.East_Asia"
            )
        ),
        ArticleTopic(
            topicId = "central-america",
            msgKey = R.string.wikimedia_articletopics_topic_central_america,
            queryTopicId = "central-america",
            taxonIds = listOf("Geography.Regions.Americas.Central_America")
        ),
        ArticleTopic(
            topicId = "europe",
            msgKey = R.string.wikimedia_articletopics_topic_europe,
            queryTopicId = "europe",
            taxonIds = listOf(
                "Geography.Regions.Europe.Europe*",
                "Geography.Regions.Europe.Eastern_Europe",
                "Geography.Regions.Europe.Northern_Europe",
                "Geography.Regions.Europe.Southern_Europe",
                "Geography.Regions.Europe.Western_Europe"
            )
        ),
        ArticleTopic(
            topicId = "north-america",
            msgKey = R.string.wikimedia_articletopics_topic_north_america,
            queryTopicId = "north-america",
            taxonIds = listOf("Geography.Regions.Americas.North_America")
        ),
        ArticleTopic(
            topicId = "oceania",
            msgKey = R.string.wikimedia_articletopics_topic_oceania,
            queryTopicId = "oceania",
            taxonIds = listOf("Geography.Regions.Oceania")
        ),
        ArticleTopic(
            topicId = "south-america",
            msgKey = R.string.wikimedia_articletopics_topic_south_america,
            queryTopicId = "south-america",
            taxonIds = listOf("Geography.Regions.Americas.South_America")
        )
    )
}
