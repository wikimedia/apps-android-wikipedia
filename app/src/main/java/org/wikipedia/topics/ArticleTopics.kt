package org.wikipedia.topics

import org.wikipedia.R

data class ArticleTopic(
    val topicId: String,
    val msgKey: Int,
    val queryTopicId: String
)

// the values defined here are from https://gerrit.wikimedia.org/r/plugins/gitiles/mediawiki/extensions/WikimediaMessages/+/refs/heads/master/includes/ArticleTopicFiltersRegistry.php
object ArticleTopics {
    val all = listOf(
        ArticleTopic(
            topicId = "architecture",
            msgKey = R.string.wikimedia_articletopics_topic_architecture,
            queryTopicId = "architecture"
        ),
        ArticleTopic(
            topicId = "art", // registry uses "art" as topicId but articleTopics = "visual-arts"
            msgKey = R.string.wikimedia_articletopics_topic_art,
            queryTopicId = "visual-arts"
        ),
        ArticleTopic(
            topicId = "comics-and-anime",
            msgKey = R.string.wikimedia_articletopics_topic_comics_and_anime,
            queryTopicId = "comics-and-anime"
        ),
        ArticleTopic(
            topicId = "entertainment",
            msgKey = R.string.wikimedia_articletopics_topic_entertainment,
            queryTopicId = "entertainment"
        ),
        ArticleTopic(
            topicId = "fashion",
            msgKey = R.string.wikimedia_articletopics_topic_fashion,
            queryTopicId = "fashion"
        ),
        ArticleTopic(
            topicId = "literature",
            msgKey = R.string.wikimedia_articletopics_topic_literature,
            queryTopicId = "books" // registry uses "literature" as topicId but articleTopics = "books"
        ),
        ArticleTopic(
            topicId = "music",
            msgKey = R.string.wikimedia_articletopics_topic_music,
            queryTopicId = "music"
        ),
        ArticleTopic(
            topicId = "performing-arts",
            msgKey = R.string.wikimedia_articletopics_topic_performing_arts,
            queryTopicId = "performing-arts"
        ),
        ArticleTopic(
            topicId = "sports",
            msgKey = R.string.wikimedia_articletopics_topic_sports,
            queryTopicId = "sports"
        ),
        ArticleTopic(
            topicId = "tv-and-film",
            msgKey = R.string.wikimedia_articletopics_topic_tv_and_film,
            queryTopicId = "films"
        ),
        ArticleTopic(
            topicId = "video-games",
            msgKey = R.string.wikimedia_articletopics_topic_video_games,
            queryTopicId = "video-games"
        ),
        ArticleTopic(
            topicId = "biography",
            msgKey = R.string.wikimedia_articletopics_topic_biography,
            queryTopicId = "biography"
        ),
        ArticleTopic(
            topicId = "women",
            msgKey = R.string.wikimedia_articletopics_topic_women,
            queryTopicId = "women"
        ),
        ArticleTopic(
            topicId = "business-and-economics",
            msgKey = R.string.wikimedia_articletopics_topic_business_and_economics,
            queryTopicId = "business-and-economics"
        ),
        ArticleTopic(
            topicId = "education",
            msgKey = R.string.wikimedia_articletopics_topic_education,
            queryTopicId = "education"
        ),
        ArticleTopic(
            topicId = "food-and-drink",
            msgKey = R.string.wikimedia_articletopics_topic_food_and_drink,
            queryTopicId = "food-and-drink"
        ),
        ArticleTopic(
            topicId = "history",
            msgKey = R.string.wikimedia_articletopics_topic_history,
            queryTopicId = "history"
        ),
        ArticleTopic(
            topicId = "military-and-warfare",
            msgKey = R.string.wikimedia_articletopics_topic_military_and_warfare,
            queryTopicId = "military-and-warfare"
        ),
        ArticleTopic(
            topicId = "philosophy-and-religion",
            msgKey = R.string.wikimedia_articletopics_topic_philosophy_and_religion,
            queryTopicId = "philosophy-and-religion"
        ),
        ArticleTopic(
            topicId = "politics-and-government",
            msgKey = R.string.wikimedia_articletopics_topic_politics_and_government,
            queryTopicId = "politics-and-government"
        ),
        ArticleTopic(
            topicId = "society",
            msgKey = R.string.wikimedia_articletopics_topic_society,
            queryTopicId = "society"
        ),
        ArticleTopic(
            topicId = "transportation",
            msgKey = R.string.wikimedia_articletopics_topic_transportation,
            queryTopicId = "transportation"
        ),
        ArticleTopic(
            topicId = "biology",
            msgKey = R.string.wikimedia_articletopics_topic_biology,
            queryTopicId = "biology"
        ),
        ArticleTopic(
            topicId = "chemistry",
            msgKey = R.string.wikimedia_articletopics_topic_chemistry,
            queryTopicId = "chemistry"
        ),
        ArticleTopic(
            topicId = "computers-and-internet", // registry uses "computers-and-internet" as topicId but articleTopics = "internet-culture"
            msgKey = R.string.wikimedia_articletopics_topic_computers_and_internet,
            queryTopicId = "internet-culture"
        ),
        ArticleTopic(
            topicId = "earth-and-environment", // registry uses "earth-and-environment" as topicId but articleTopics = "geographical"
            msgKey = R.string.wikimedia_articletopics_topic_earth_and_environment,
            queryTopicId = "geographical"
        ),
        ArticleTopic(
            topicId = "engineering",
            msgKey = R.string.wikimedia_articletopics_topic_engineering,
            queryTopicId = "engineering"
        ),
        ArticleTopic(
            topicId = "general-science", // registry uses "general-science" as topicId but articleTopics = "stem"
            msgKey = R.string.wikimedia_articletopics_topic_general_science,
            queryTopicId = "stem"
        ),
        ArticleTopic(
            topicId = "mathematics",
            msgKey = R.string.wikimedia_articletopics_topic_mathematics,
            queryTopicId = "mathematics"
        ),
        ArticleTopic(
            topicId = "medicine-and-health",
            msgKey = R.string.wikimedia_articletopics_topic_medicine_and_health,
            queryTopicId = "medicine-and-health"
        ),
        ArticleTopic(
            topicId = "physics",
            msgKey = R.string.wikimedia_articletopics_topic_physics,
            queryTopicId = "physics"
        ),
        ArticleTopic(
            topicId = "technology",
            msgKey = R.string.wikimedia_articletopics_topic_technology,
            queryTopicId = "technology"
        ),
        ArticleTopic(
            topicId = "africa",
            msgKey = R.string.wikimedia_articletopics_topic_africa,
            queryTopicId = "africa"
        ),
        ArticleTopic(
            topicId = "asia",
            msgKey = R.string.wikimedia_articletopics_topic_asia,
            queryTopicId = "asia"
        ),
        ArticleTopic(
            topicId = "central-america",
            msgKey = R.string.wikimedia_articletopics_topic_central_america,
            queryTopicId = "central-america"
        ),
        ArticleTopic(
            topicId = "europe",
            msgKey = R.string.wikimedia_articletopics_topic_europe,
            queryTopicId = "europe"
        ),
        ArticleTopic(
            topicId = "north-america",
            msgKey = R.string.wikimedia_articletopics_topic_north_america,
            queryTopicId = "north-america"
        ),
        ArticleTopic(
            topicId = "oceania",
            msgKey = R.string.wikimedia_articletopics_topic_oceania,
            queryTopicId = "oceania"
        ),
        ArticleTopic(
            topicId = "south-america",
            msgKey = R.string.wikimedia_articletopics_topic_south_america,
            queryTopicId = "south-america"
        )
    )
}
