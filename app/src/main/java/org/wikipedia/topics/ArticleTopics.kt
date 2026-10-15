package org.wikipedia.topics

import kotlinx.serialization.Serializable
import org.wikipedia.R

@Serializable
data class ArticleTopic(
    val topicId: String,
    val msgKey: Int,
    val queryTopicId: String,
    val taxonIds: List<String>,
    val thumbnailUrl: String
)

// the values defined here are from https://gerrit.wikimedia.org/r/plugins/gitiles/mediawiki/extensions/WikimediaMessages/+/refs/heads/master/includes/ArticleTopicFiltersRegistry.php
object ArticleTopics {
    val all = listOf(
        ArticleTopic(
            topicId = "architecture",
            msgKey = R.string.wikimedia_articletopics_topic_architecture,
            queryTopicId = "architecture",
            taxonIds = listOf("Culture.Visual_arts.Architecture"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d4/01_Architecture.png/250px-01_Architecture.png"
        ),
        ArticleTopic(
            topicId = "art", // registry uses "art" as topicId but articleTopics = "visual-arts"
            msgKey = R.string.wikimedia_articletopics_topic_art,
            queryTopicId = "visual-arts",
            taxonIds = listOf("Culture.Visual_arts.Visual_arts*"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e4/02_Art.png/250px-02_Art.png"
        ),
        ArticleTopic(
            topicId = "comics-and-anime",
            msgKey = R.string.wikimedia_articletopics_topic_comics_and_anime,
            queryTopicId = "comics-and-anime",
            taxonIds = listOf("Culture.Visual_arts.Comics_and_Anime"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e4/03_Comics_Anime.png/250px-03_Comics_Anime.png"
        ),
        ArticleTopic(
            topicId = "entertainment",
            msgKey = R.string.wikimedia_articletopics_topic_entertainment,
            queryTopicId = "entertainment",
            taxonIds = listOf(
                "Culture.Media.Entertainment",
                "Culture.Media.Radio"
            ),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ef/04_Entertainment.png/250px-04_Entertainment.png"
        ),
        ArticleTopic(
            topicId = "fashion",
            msgKey = R.string.wikimedia_articletopics_topic_fashion,
            queryTopicId = "fashion",
            taxonIds = listOf("Culture.Visual_arts.Fashion"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/4c/05_Fashion.png/250px-05_Fashion.png"
        ),
        ArticleTopic(
            topicId = "literature",
            msgKey = R.string.wikimedia_articletopics_topic_literature,
            queryTopicId = "books", // registry uses "literature" as topicId but articleTopics = "books"
            taxonIds = listOf("Culture.Media.Books"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/12/06_Literature.png/250px-06_Literature.png"
        ),
        ArticleTopic(
            topicId = "music",
            msgKey = R.string.wikimedia_articletopics_topic_music,
            queryTopicId = "music",
            taxonIds = listOf("Culture.Media.Music"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e8/07_Music.png/250px-07_Music.png"
        ),
        ArticleTopic(
            topicId = "performing-arts",
            msgKey = R.string.wikimedia_articletopics_topic_performing_arts,
            queryTopicId = "performing-arts",
            taxonIds = listOf("Culture.Performing_arts"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/06/08_Performing_Arts.png/250px-08_Performing_Arts.png"
        ),
        ArticleTopic(
            topicId = "sports",
            msgKey = R.string.wikimedia_articletopics_topic_sports,
            queryTopicId = "sports",
            taxonIds = listOf("Culture.Sports"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1b/09_Sports.png/250px-09_Sports.png"
        ),
        ArticleTopic(
            topicId = "tv-and-film",
            msgKey = R.string.wikimedia_articletopics_topic_tv_and_film,
            queryTopicId = "films",
            taxonIds = listOf(
                "Culture.Media.Films",
                "Culture.Media.Television"
            ),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/8e/10_TV_Film.png/250px-10_TV_Film.png"
        ),
        ArticleTopic(
            topicId = "video-games",
            msgKey = R.string.wikimedia_articletopics_topic_video_games,
            queryTopicId = "video-games",
            taxonIds = listOf("Culture.Media.Video_games"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/ca/11_Videogames.png/250px-11_Videogames.png"
        ),
        ArticleTopic(
            topicId = "biography",
            msgKey = R.string.wikimedia_articletopics_topic_biography,
            queryTopicId = "biography",
            taxonIds = listOf("Culture.Biography.Biography*"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/3/38/12_Biography_all.png/250px-12_Biography_all.png"
        ),
        ArticleTopic(
            topicId = "women",
            msgKey = R.string.wikimedia_articletopics_topic_women,
            queryTopicId = "women",
            taxonIds = listOf("Culture.Biography.Women"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/1/1e/13_Biography_women.png/250px-13_Biography_women.png"
        ),
        ArticleTopic(
            topicId = "business-and-economics",
            msgKey = R.string.wikimedia_articletopics_topic_business_and_economics,
            queryTopicId = "business-and-economics",
            taxonIds = listOf("History_and_Society.Business_and_economics"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/43/14_Business_Economics.png/250px-14_Business_Economics.png"
        ),
        ArticleTopic(
            topicId = "education",
            msgKey = R.string.wikimedia_articletopics_topic_education,
            queryTopicId = "education",
            taxonIds = listOf("History_and_Society.Education"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/80/15_Education.png/250px-15_Education.png"
        ),
        ArticleTopic(
            topicId = "food-and-drink",
            msgKey = R.string.wikimedia_articletopics_topic_food_and_drink,
            queryTopicId = "food-and-drink",
            taxonIds = listOf("Culture.Food_and_drink"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0c/16_Food_Drink.png/250px-16_Food_Drink.png"
        ),
        ArticleTopic(
            topicId = "history",
            msgKey = R.string.wikimedia_articletopics_topic_history,
            queryTopicId = "history",
            taxonIds = listOf("History_and_Society.History"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/40/17_History.png/250px-17_History.png"
        ),
        ArticleTopic(
            topicId = "military-and-warfare",
            msgKey = R.string.wikimedia_articletopics_topic_military_and_warfare,
            queryTopicId = "military-and-warfare",
            taxonIds = listOf("History_and_Society.Military_and_warfare"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/bb/18_Military_warfare.png/250px-18_Military_warfare.png"
        ),
        ArticleTopic(
            topicId = "philosophy-and-religion",
            msgKey = R.string.wikimedia_articletopics_topic_philosophy_and_religion,
            queryTopicId = "philosophy-and-religion",
            taxonIds = listOf("Culture.Philosophy_and_religion"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/a/a4/19_Religion_Philosophy.png/250px-19_Religion_Philosophy.png"
        ),
        ArticleTopic(
            topicId = "politics-and-government",
            msgKey = R.string.wikimedia_articletopics_topic_politics_and_government,
            queryTopicId = "politics-and-government",
            taxonIds = listOf("History_and_Society.Politics_and_government"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/27/20_Politics_Government.png/250px-20_Politics_Government.png"
        ),
        ArticleTopic(
            topicId = "society",
            msgKey = R.string.wikimedia_articletopics_topic_society,
            queryTopicId = "society",
            taxonIds = listOf("History_and_Society.Society"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/5/59/21_Society.png/250px-21_Society.png"
        ),
        ArticleTopic(
            topicId = "transportation",
            msgKey = R.string.wikimedia_articletopics_topic_transportation,
            queryTopicId = "transportation",
            taxonIds = listOf("History_and_Society.Transportation"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d2/22_Transportation.png/250px-22_Transportation.png"
        ),
        ArticleTopic(
            topicId = "biology",
            msgKey = R.string.wikimedia_articletopics_topic_biology,
            queryTopicId = "biology",
            taxonIds = listOf("STEM.Biology"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ef/23_Biology.png/250px-23_Biology.png"
        ),
        ArticleTopic(
            topicId = "chemistry",
            msgKey = R.string.wikimedia_articletopics_topic_chemistry,
            queryTopicId = "chemistry",
            taxonIds = listOf("STEM.Chemistry"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/69/24_Chemistry.png/250px-24_Chemistry.png"
        ),
        ArticleTopic(
            topicId = "computers-and-internet", // registry uses "computers-and-internet" as topicId but articleTopics = "internet-culture"
            msgKey = R.string.wikimedia_articletopics_topic_computers_and_internet,
            queryTopicId = "internet-culture",
            taxonIds = listOf(
                "Culture.Internet_culture",
                "Culture.Media.Software",
                "STEM.Computing"
            ),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ef/25_Computers_Internet.png/250px-25_Computers_Internet.png"
        ),
        ArticleTopic(
            topicId = "earth-and-environment", // registry uses "earth-and-environment" as topicId but articleTopics = "geographical"
            msgKey = R.string.wikimedia_articletopics_topic_earth_and_environment,
            queryTopicId = "geographical",
            taxonIds = listOf(
                "Geography.Geographical",
                "STEM.Earth_and_environment"
            ),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6d/26_Earth_Environment.png/250px-26_Earth_Environment.png"
        ),
        ArticleTopic(
            topicId = "engineering",
            msgKey = R.string.wikimedia_articletopics_topic_engineering,
            queryTopicId = "engineering",
            taxonIds = listOf("STEM.Engineering"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/7e/27_Engineering.png/250px-27_Engineering.png"
        ),
        ArticleTopic(
            topicId = "general-science", // registry uses "general-science" as topicId but articleTopics = "stem"
            msgKey = R.string.wikimedia_articletopics_topic_general_science,
            queryTopicId = "stem",
            taxonIds = listOf("STEM.STEM*"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/ef/28_General_Science.png/250px-28_General_Science.png"
        ),
        ArticleTopic(
            topicId = "mathematics",
            msgKey = R.string.wikimedia_articletopics_topic_mathematics,
            queryTopicId = "mathematics",
            taxonIds = listOf("STEM.Mathematics"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d6/29_Mathematics.png/250px-29_Mathematics.png"
        ),
        ArticleTopic(
            topicId = "medicine-and-health",
            msgKey = R.string.wikimedia_articletopics_topic_medicine_and_health,
            queryTopicId = "medicine-and-health",
            taxonIds = listOf("STEM.Medicine_&_Health"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/ca/30_Medicine_Health.png/250px-30_Medicine_Health.png"
        ),
        ArticleTopic(
            topicId = "physics",
            msgKey = R.string.wikimedia_articletopics_topic_physics,
            queryTopicId = "physics",
            taxonIds = listOf(
                "STEM.Physics",
                "STEM.Space"
            ),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/8/84/31_Physics.png/250px-31_Physics.png"
        ),
        ArticleTopic(
            topicId = "technology",
            msgKey = R.string.wikimedia_articletopics_topic_technology,
            queryTopicId = "technology",
            taxonIds = listOf("STEM.Technology"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/68/32_Technology.png/250px-32_Technology.png"
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
            ),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/33_-_39_Regional.png/250px-33_-_39_Regional.png"
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
            ),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/33_-_39_Regional.png/250px-33_-_39_Regional.png"
        ),
        ArticleTopic(
            topicId = "central-america",
            msgKey = R.string.wikimedia_articletopics_topic_central_america,
            queryTopicId = "central-america",
            taxonIds = listOf("Geography.Regions.Americas.Central_America"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/33_-_39_Regional.png/250px-33_-_39_Regional.png"
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
            ),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/33_-_39_Regional.png/250px-33_-_39_Regional.png"
        ),
        ArticleTopic(
            topicId = "north-america",
            msgKey = R.string.wikimedia_articletopics_topic_north_america,
            queryTopicId = "north-america",
            taxonIds = listOf("Geography.Regions.Americas.North_America"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/33_-_39_Regional.png/250px-33_-_39_Regional.png"
        ),
        ArticleTopic(
            topicId = "oceania",
            msgKey = R.string.wikimedia_articletopics_topic_oceania,
            queryTopicId = "oceania",
            taxonIds = listOf("Geography.Regions.Oceania"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/33_-_39_Regional.png/250px-33_-_39_Regional.png"
        ),
        ArticleTopic(
            topicId = "south-america",
            msgKey = R.string.wikimedia_articletopics_topic_south_america,
            queryTopicId = "south-america",
            taxonIds = listOf("Geography.Regions.Americas.South_America"),
            thumbnailUrl = "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/49/33_-_39_Regional.png/250px-33_-_39_Regional.png"
        )
    )
}
