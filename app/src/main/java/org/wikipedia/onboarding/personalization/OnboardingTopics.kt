package org.wikipedia.onboarding.personalization

// the values defined here are from https://gerrit.wikimedia.org/r/plugins/gitiles/mediawiki/extensions/WikimediaMessages/+/refs/heads/master/includes/ArticleTopicFiltersRegistry.php
object OnboardingTopics {
    val all = listOf(
        OnboardingTopic(
            topicId = "biography",
            msgKey = "wikimedia-articletopics-topic-biography",
            articleTopics = "biography",
            displayTitle = "Biography"
        ),
        OnboardingTopic(
            topicId = "food-and-drink",
            msgKey = "wikimedia-articletopics-topic-food-and-drink",
            articleTopics = "food-and-drink",
            displayTitle = "Food and Drink"
        ),
        OnboardingTopic(
            topicId = "computers-and-internet", // registry uses "computers-and-internet" as topicId but articleTopics = "internet-culture"
            msgKey = "wikimedia-articletopics-topic-computers-and-internet",
            articleTopics = "internet-culture",
            displayTitle = "Internet Culture"
        ),
        OnboardingTopic(
            topicId = "history",
            msgKey = "wikimedia-articletopics-topic-history",
            articleTopics = "history",
            displayTitle = "History"
        ),
        OnboardingTopic(
            topicId = "literature",
            msgKey = "wikimedia-articletopics-topic-literature",
            articleTopics = "books", // registry uses "literature" as topicId but articleTopics = "books"
            displayTitle = "Literature"
        ),
        OnboardingTopic(
            topicId = "performing-arts",
            msgKey = "wikimedia-articletopics-topic-performing-arts",
            articleTopics = "performing-arts",
            displayTitle = "Performing arts"
        ),
        OnboardingTopic(
            topicId = "philosophy-and-religion",
            msgKey = "wikimedia-articletopics-topic-philosophy-and-religion",
            articleTopics = "philosophy-and-religion",
            displayTitle = "Philosophy and religion"
        ),
        OnboardingTopic(
            topicId = "sports",
            msgKey = "wikimedia-articletopics-topic-sports",
            articleTopics = "sports",
            displayTitle = "Sports"
        ),
        OnboardingTopic(
            topicId = "art", // registry uses "art" as topicId but articleTopics = "visual-arts"
            msgKey = "wikimedia-articletopics-topic-art",
            articleTopics = "visual-arts",
            displayTitle = "Art"
        ),
        OnboardingTopic(
            topicId = "earth-and-environment", // registry uses "earth-and-environment" as topicId but articleTopics = "geographical"
            msgKey = "wikimedia-articletopics-topic-earth-and-environment",
            articleTopics = "geographical",
            displayTitle = "Geographical"
        ),
        OnboardingTopic(
            topicId = "africa",
            msgKey = "wikimedia-articletopics-topic-africa",
            articleTopics = "africa",
            displayTitle = "Africa"
        ),
        OnboardingTopic(
            topicId = "asia",
            msgKey = "wikimedia-articletopics-topic-asia",
            articleTopics = "asia",
            displayTitle = "Asia"
        ),
        OnboardingTopic(
            topicId = "europe",
            msgKey = "wikimedia-articletopics-topic-europe",
            articleTopics = "europe",
            displayTitle = "Europe"
        ),
        OnboardingTopic(
            topicId = "oceania",
            msgKey = "wikimedia-articletopics-topic-oceania",
            articleTopics = "oceania",
            displayTitle = "Oceania"
        ),
        OnboardingTopic(
            topicId = "business-and-economics",
            msgKey = "wikimedia-articletopics-topic-business-and-economics",
            articleTopics = "business-and-economics",
            displayTitle = "Business and economics"
        ),
        OnboardingTopic(
            topicId = "education",
            msgKey = "wikimedia-articletopics-topic-education",
            articleTopics = "education",
            displayTitle = "Education"
        ),
        OnboardingTopic(
            topicId = "military-and-warfare",
            msgKey = "wikimedia-articletopics-topic-military-and-warfare",
            articleTopics = "military-and-warfare",
            displayTitle = "Military and warfare"
        ),
        OnboardingTopic(
            topicId = "politics-and-government",
            msgKey = "wikimedia-articletopics-topic-politics-and-government",
            articleTopics = "politics-and-government",
            displayTitle = "Politics and government"
        ),
        OnboardingTopic(
            topicId = "society",
            msgKey = "wikimedia-articletopics-topic-society",
            articleTopics = "society",
            displayTitle = "Society"
        ),
        OnboardingTopic(
            topicId = "transportation",
            msgKey = "wikimedia-articletopics-topic-transportation",
            articleTopics = "transportation",
            displayTitle = "Transportation"
        ),
        OnboardingTopic(
            topicId = "general-science", // registry uses "general-science" as topicId but articleTopics = "stem"
            msgKey = "wikimedia-articletopics-topic-general-science",
            articleTopics = "stem",
            displayTitle = "STEM"
        ),
        OnboardingTopic(
            topicId = "central-america",
            msgKey = "wikimedia-articletopics-topic-central-america",
            articleTopics = "central-america",
            displayTitle = "Central America"
        ),
        OnboardingTopic(
            topicId = "north-america",
            msgKey = "wikimedia-articletopics-topic-north-america",
            articleTopics = "north-america",
            displayTitle = "North America"
        ),
        OnboardingTopic(
            topicId = "south-america",
            msgKey = "wikimedia-articletopics-topic-south-america",
            articleTopics = "south-america",
            displayTitle = "South America"
        ),
        OnboardingTopic(
            topicId = "architecture",
            msgKey = "wikimedia-articletopics-topic-architecture",
            articleTopics = "architecture",
            displayTitle = "Architecture"
        ),
        OnboardingTopic(
            topicId = "comics-and-anime",
            msgKey = "wikimedia-articletopics-topic-comics-and-anime",
            articleTopics = "comics-and-anime",
            displayTitle = "Comics and Anime"
        ),
        OnboardingTopic(
            topicId = "entertainment",
            msgKey = "wikimedia-articletopics-topic-entertainment",
            articleTopics = "entertainment",
            displayTitle = "Entertainment"
        ),
        OnboardingTopic(
            topicId = "fashion",
            msgKey = "wikimedia-articletopics-topic-fashion",
            articleTopics = "fashion",
            displayTitle = "Fashion"
        ),
        OnboardingTopic(
            topicId = "music",
            msgKey = "wikimedia-articletopics-topic-music",
            articleTopics = "music",
            displayTitle = "Music"
        ),
        OnboardingTopic(
            topicId = "tv-and-film",
            msgKey = "wikimedia-articletopics-topic-tv-and-film",
            articleTopics = "films",
            displayTitle = "TV and Film"
        ),
        OnboardingTopic(
            topicId = "video-games",
            msgKey = "wikimedia-articletopics-topic-video-games",
            articleTopics = "video-games",
            displayTitle = "Video Games"
        ),
        OnboardingTopic(
            topicId = "women",
            msgKey = "wikimedia-articletopics-topic-women",
            articleTopics = "women",
            displayTitle = "Women"
        ),
        OnboardingTopic(
            topicId = "biology",
            msgKey = "wikimedia-articletopics-topic-biology",
            articleTopics = "biology",
            displayTitle = "Biology"
        ),
        OnboardingTopic(
            topicId = "chemistry",
            msgKey = "wikimedia-articletopics-topic-chemistry",
            articleTopics = "chemistry",
            displayTitle = "Chemistry"
        ),
        OnboardingTopic(
            topicId = "engineering",
            msgKey = "wikimedia-articletopics-topic-engineering",
            articleTopics = "engineering",
            displayTitle = "Engineering"
        ),
        OnboardingTopic(
            topicId = "mathematics",
            msgKey = "wikimedia-articletopics-topic-mathematics",
            articleTopics = "mathematics",
            displayTitle = "Mathematics"
        ),
        OnboardingTopic(
            topicId = "medicine-and-health",
            msgKey = "wikimedia-articletopics-topic-medicine-and-health",
            articleTopics = "medicine-and-health",
            displayTitle = "Medicine and Health"
        ),
        OnboardingTopic(
            topicId = "physics",
            msgKey = "wikimedia-articletopics-topic-physics",
            articleTopics = "physics",
            displayTitle = "Physics"
        ),
        OnboardingTopic(
            topicId = "technology",
            msgKey = "wikimedia-articletopics-topic-technology",
            articleTopics = "technology",
            displayTitle = "Technology"
        ),
    )
}
