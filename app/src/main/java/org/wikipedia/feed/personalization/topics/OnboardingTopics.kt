package org.wikipedia.feed.personalization.topics

import org.wikipedia.feed.personalization.interest.OnboardingTopic

// the values defined here are from https://gerrit.wikimedia.org/r/plugins/gitiles/mediawiki/extensions/WikimediaMessages/+/refs/heads/master/includes/ArticleTopicFiltersRegistry.php
object OnboardingTopics {
    val all = listOf(
        OnboardingTopic(
            topicId = "architecture",
            msgKey = "wikimedia-articletopics-topic-architecture",
            queryTopicId = "architecture",
            displayTitle = "Architecture"
        ),
        OnboardingTopic(
            topicId = "art", // registry uses "art" as topicId but articleTopics = "visual-arts"
            msgKey = "wikimedia-articletopics-topic-art",
            queryTopicId = "visual-arts",
            displayTitle = "Art"
        ),
        OnboardingTopic(
            topicId = "comics-and-anime",
            msgKey = "wikimedia-articletopics-topic-comics-and-anime",
            queryTopicId = "comics-and-anime",
            displayTitle = "Comics and Anime"
        ),
        OnboardingTopic(
            topicId = "entertainment",
            msgKey = "wikimedia-articletopics-topic-entertainment",
            queryTopicId = "entertainment",
            displayTitle = "Entertainment"
        ),
        OnboardingTopic(
            topicId = "fashion",
            msgKey = "wikimedia-articletopics-topic-fashion",
            queryTopicId = "fashion",
            displayTitle = "Fashion"
        ),
        OnboardingTopic(
            topicId = "literature",
            msgKey = "wikimedia-articletopics-topic-literature",
            queryTopicId = "books", // registry uses "literature" as topicId but articleTopics = "books"
            displayTitle = "Literature"
        ),
        OnboardingTopic(
            topicId = "music",
            msgKey = "wikimedia-articletopics-topic-music",
            queryTopicId = "music",
            displayTitle = "Music"
        ),
        OnboardingTopic(
            topicId = "performing-arts",
            msgKey = "wikimedia-articletopics-topic-performing-arts",
            queryTopicId = "performing-arts",
            displayTitle = "Performing arts"
        ),
        OnboardingTopic(
            topicId = "sports",
            msgKey = "wikimedia-articletopics-topic-sports",
            queryTopicId = "sports",
            displayTitle = "Sports"
        ),
        OnboardingTopic(
            topicId = "tv-and-film",
            msgKey = "wikimedia-articletopics-topic-tv-and-film",
            queryTopicId = "films",
            displayTitle = "TV and Film"
        ),
        OnboardingTopic(
            topicId = "video-games",
            msgKey = "wikimedia-articletopics-topic-video-games",
            queryTopicId = "video-games",
            displayTitle = "Video Games"
        ),
        OnboardingTopic(
            topicId = "biography",
            msgKey = "wikimedia-articletopics-topic-biography",
            queryTopicId = "biography",
            displayTitle = "Biography"
        ),
        OnboardingTopic(
            topicId = "women",
            msgKey = "wikimedia-articletopics-topic-women",
            queryTopicId = "women",
            displayTitle = "Women"
        ),
        OnboardingTopic(
            topicId = "business-and-economics",
            msgKey = "wikimedia-articletopics-topic-business-and-economics",
            queryTopicId = "business-and-economics",
            displayTitle = "Business and economics"
        ),
        OnboardingTopic(
            topicId = "education",
            msgKey = "wikimedia-articletopics-topic-education",
            queryTopicId = "education",
            displayTitle = "Education"
        ),
        OnboardingTopic(
            topicId = "food-and-drink",
            msgKey = "wikimedia-articletopics-topic-food-and-drink",
            queryTopicId = "food-and-drink",
            displayTitle = "Food and Drink"
        ),
        OnboardingTopic(
            topicId = "history",
            msgKey = "wikimedia-articletopics-topic-history",
            queryTopicId = "history",
            displayTitle = "History"
        ),
        OnboardingTopic(
            topicId = "military-and-warfare",
            msgKey = "wikimedia-articletopics-topic-military-and-warfare",
            queryTopicId = "military-and-warfare",
            displayTitle = "Military and warfare"
        ),
        OnboardingTopic(
            topicId = "philosophy-and-religion",
            msgKey = "wikimedia-articletopics-topic-philosophy-and-religion",
            queryTopicId = "philosophy-and-religion",
            displayTitle = "Philosophy and religion"
        ),
        OnboardingTopic(
            topicId = "politics-and-government",
            msgKey = "wikimedia-articletopics-topic-politics-and-government",
            queryTopicId = "politics-and-government",
            displayTitle = "Politics and government"
        ),
        OnboardingTopic(
            topicId = "society",
            msgKey = "wikimedia-articletopics-topic-society",
            queryTopicId = "society",
            displayTitle = "Society"
        ),
        OnboardingTopic(
            topicId = "transportation",
            msgKey = "wikimedia-articletopics-topic-transportation",
            queryTopicId = "transportation",
            displayTitle = "Transportation"
        ),
        OnboardingTopic(
            topicId = "biology",
            msgKey = "wikimedia-articletopics-topic-biology",
            queryTopicId = "biology",
            displayTitle = "Biology"
        ),
        OnboardingTopic(
            topicId = "chemistry",
            msgKey = "wikimedia-articletopics-topic-chemistry",
            queryTopicId = "chemistry",
            displayTitle = "Chemistry"
        ),
        OnboardingTopic(
            topicId = "computers-and-internet", // registry uses "computers-and-internet" as topicId but articleTopics = "internet-culture"
            msgKey = "wikimedia-articletopics-topic-computers-and-internet",
            queryTopicId = "internet-culture",
            displayTitle = "Internet Culture"
        ),
        OnboardingTopic(
            topicId = "earth-and-environment", // registry uses "earth-and-environment" as topicId but articleTopics = "geographical"
            msgKey = "wikimedia-articletopics-topic-earth-and-environment",
            queryTopicId = "geographical",
            displayTitle = "Geographical"
        ),
        OnboardingTopic(
            topicId = "engineering",
            msgKey = "wikimedia-articletopics-topic-engineering",
            queryTopicId = "engineering",
            displayTitle = "Engineering"
        ),
        OnboardingTopic(
            topicId = "general-science", // registry uses "general-science" as topicId but articleTopics = "stem"
            msgKey = "wikimedia-articletopics-topic-general-science",
            queryTopicId = "stem",
            displayTitle = "STEM"
        ),
        OnboardingTopic(
            topicId = "mathematics",
            msgKey = "wikimedia-articletopics-topic-mathematics",
            queryTopicId = "mathematics",
            displayTitle = "Mathematics"
        ),
        OnboardingTopic(
            topicId = "medicine-and-health",
            msgKey = "wikimedia-articletopics-topic-medicine-and-health",
            queryTopicId = "medicine-and-health",
            displayTitle = "Medicine and Health"
        ),
        OnboardingTopic(
            topicId = "physics",
            msgKey = "wikimedia-articletopics-topic-physics",
            queryTopicId = "physics",
            displayTitle = "Physics"
        ),
        OnboardingTopic(
            topicId = "technology",
            msgKey = "wikimedia-articletopics-topic-technology",
            queryTopicId = "technology",
            displayTitle = "Technology"
        ),
        OnboardingTopic(
            topicId = "africa",
            msgKey = "wikimedia-articletopics-topic-africa",
            queryTopicId = "africa",
            displayTitle = "Africa"
        ),
        OnboardingTopic(
            topicId = "asia",
            msgKey = "wikimedia-articletopics-topic-asia",
            queryTopicId = "asia",
            displayTitle = "Asia"
        ),
        OnboardingTopic(
            topicId = "central-america",
            msgKey = "wikimedia-articletopics-topic-central-america",
            queryTopicId = "central-america",
            displayTitle = "Central America"
        ),
        OnboardingTopic(
            topicId = "europe",
            msgKey = "wikimedia-articletopics-topic-europe",
            queryTopicId = "europe",
            displayTitle = "Europe"
        ),
        OnboardingTopic(
            topicId = "north-america",
            msgKey = "wikimedia-articletopics-topic-north-america",
            queryTopicId = "north-america",
            displayTitle = "North America"
        ),
        OnboardingTopic(
            topicId = "oceania",
            msgKey = "wikimedia-articletopics-topic-oceania",
            queryTopicId = "oceania",
            displayTitle = "Oceania"
        ),
        OnboardingTopic(
            topicId = "south-america",
            msgKey = "wikimedia-articletopics-topic-south-america",
            queryTopicId = "south-america",
            displayTitle = "South America"
        )
    )
}
