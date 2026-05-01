package org.wikipedia.feed.personalization.topics

import org.wikipedia.R
import org.wikipedia.feed.personalization.interest.OnboardingTopic

// the values defined here are from https://gerrit.wikimedia.org/r/plugins/gitiles/mediawiki/extensions/WikimediaMessages/+/refs/heads/master/includes/ArticleTopicFiltersRegistry.php
object OnboardingTopics {
    val all = listOf(
        OnboardingTopic(
            topicId = "architecture",
            msgKey = R.string.wikimedia_articletopics_topic_architecture,
            queryTopicId = "architecture"
        ),
        OnboardingTopic(
            topicId = "art", // registry uses "art" as topicId but articleTopics = "visual-arts"
            msgKey = R.string.wikimedia_articletopics_topic_art,
            queryTopicId = "visual-arts"
        ),
        OnboardingTopic(
            topicId = "comics-and-anime",
            msgKey = R.string.wikimedia_articletopics_topic_comics_and_anime,
            queryTopicId = "comics-and-anime"
        ),
        OnboardingTopic(
            topicId = "entertainment",
            msgKey = R.string.wikimedia_articletopics_topic_entertainment,
            queryTopicId = "entertainment"
        ),
        OnboardingTopic(
            topicId = "fashion",
            msgKey = R.string.wikimedia_articletopics_topic_fashion,
            queryTopicId = "fashion"
        ),
        OnboardingTopic(
            topicId = "literature",
            msgKey = R.string.wikimedia_articletopics_topic_literature,
            queryTopicId = "books" // registry uses "literature" as topicId but articleTopics = "books"
        ),
        OnboardingTopic(
            topicId = "music",
            msgKey = R.string.wikimedia_articletopics_topic_music,
            queryTopicId = "music"
        ),
        OnboardingTopic(
            topicId = "performing-arts",
            msgKey = R.string.wikimedia_articletopics_topic_performing_arts,
            queryTopicId = "performing-arts"
        ),
        OnboardingTopic(
            topicId = "sports",
            msgKey = R.string.wikimedia_articletopics_topic_sports,
            queryTopicId = "sports"
        ),
        OnboardingTopic(
            topicId = "tv-and-film",
            msgKey = R.string.wikimedia_articletopics_topic_tv_and_film,
            queryTopicId = "films"
        ),
        OnboardingTopic(
            topicId = "video-games",
            msgKey = R.string.wikimedia_articletopics_topic_video_games,
            queryTopicId = "video-games"
        ),
        OnboardingTopic(
            topicId = "biography",
            msgKey = R.string.wikimedia_articletopics_topic_biography,
            queryTopicId = "biography"
        ),
        OnboardingTopic(
            topicId = "women",
            msgKey = R.string.wikimedia_articletopics_topic_women,
            queryTopicId = "women"
        ),
        OnboardingTopic(
            topicId = "business-and-economics",
            msgKey = R.string.wikimedia_articletopics_topic_business_and_economics,
            queryTopicId = "business-and-economics"
        ),
        OnboardingTopic(
            topicId = "education",
            msgKey = R.string.wikimedia_articletopics_topic_education,
            queryTopicId = "education"
        ),
        OnboardingTopic(
            topicId = "food-and-drink",
            msgKey = R.string.wikimedia_articletopics_topic_food_and_drink,
            queryTopicId = "food-and-drink"
        ),
        OnboardingTopic(
            topicId = "history",
            msgKey = R.string.wikimedia_articletopics_topic_history,
            queryTopicId = "history"
        ),
        OnboardingTopic(
            topicId = "military-and-warfare",
            msgKey = R.string.wikimedia_articletopics_topic_military_and_warfare,
            queryTopicId = "military-and-warfare"
        ),
        OnboardingTopic(
            topicId = "philosophy-and-religion",
            msgKey = R.string.wikimedia_articletopics_topic_philosophy_and_religion,
            queryTopicId = "philosophy-and-religion"
        ),
        OnboardingTopic(
            topicId = "politics-and-government",
            msgKey = R.string.wikimedia_articletopics_topic_politics_and_government,
            queryTopicId = "politics-and-government"
        ),
        OnboardingTopic(
            topicId = "society",
            msgKey = R.string.wikimedia_articletopics_topic_society,
            queryTopicId = "society"
        ),
        OnboardingTopic(
            topicId = "transportation",
            msgKey = R.string.wikimedia_articletopics_topic_transportation,
            queryTopicId = "transportation"
        ),
        OnboardingTopic(
            topicId = "biology",
            msgKey = R.string.wikimedia_articletopics_topic_biology,
            queryTopicId = "biology"
        ),
        OnboardingTopic(
            topicId = "chemistry",
            msgKey = R.string.wikimedia_articletopics_topic_chemistry,
            queryTopicId = "chemistry"
        ),
        OnboardingTopic(
            topicId = "computers-and-internet", // registry uses "computers-and-internet" as topicId but articleTopics = "internet-culture"
            msgKey = R.string.wikimedia_articletopics_topic_computers_and_internet,
            queryTopicId = "internet-culture"
        ),
        OnboardingTopic(
            topicId = "earth-and-environment", // registry uses "earth-and-environment" as topicId but articleTopics = "geographical"
            msgKey = R.string.wikimedia_articletopics_topic_earth_and_environment,
            queryTopicId = "geographical"
        ),
        OnboardingTopic(
            topicId = "engineering",
            msgKey = R.string.wikimedia_articletopics_topic_engineering,
            queryTopicId = "engineering"
        ),
        OnboardingTopic(
            topicId = "general-science", // registry uses "general-science" as topicId but articleTopics = "stem"
            msgKey = R.string.wikimedia_articletopics_topic_general_science,
            queryTopicId = "stem"
        ),
        OnboardingTopic(
            topicId = "mathematics",
            msgKey = R.string.wikimedia_articletopics_topic_mathematics,
            queryTopicId = "mathematics"
        ),
        OnboardingTopic(
            topicId = "medicine-and-health",
            msgKey = R.string.wikimedia_articletopics_topic_medicine_and_health,
            queryTopicId = "medicine-and-health"
        ),
        OnboardingTopic(
            topicId = "physics",
            msgKey = R.string.wikimedia_articletopics_topic_physics,
            queryTopicId = "physics"
        ),
        OnboardingTopic(
            topicId = "technology",
            msgKey = R.string.wikimedia_articletopics_topic_technology,
            queryTopicId = "technology"
        ),
        OnboardingTopic(
            topicId = "africa",
            msgKey = R.string.wikimedia_articletopics_topic_africa,
            queryTopicId = "africa"
        ),
        OnboardingTopic(
            topicId = "asia",
            msgKey = R.string.wikimedia_articletopics_topic_asia,
            queryTopicId = "asia"
        ),
        OnboardingTopic(
            topicId = "central-america",
            msgKey = R.string.wikimedia_articletopics_topic_central_america,
            queryTopicId = "central-america"
        ),
        OnboardingTopic(
            topicId = "europe",
            msgKey = R.string.wikimedia_articletopics_topic_europe,
            queryTopicId = "europe"
        ),
        OnboardingTopic(
            topicId = "north-america",
            msgKey = R.string.wikimedia_articletopics_topic_north_america,
            queryTopicId = "north-america"
        ),
        OnboardingTopic(
            topicId = "oceania",
            msgKey = R.string.wikimedia_articletopics_topic_oceania,
            queryTopicId = "oceania"
        ),
        OnboardingTopic(
            topicId = "south-america",
            msgKey = R.string.wikimedia_articletopics_topic_south_america,
            queryTopicId = "south-america"
        )
    )
}
