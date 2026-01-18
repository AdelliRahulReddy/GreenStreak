package com.rahul.githubwallpaper.data

// Simplified data classes without Moshi - using org.json for parsing

data class GraphQLRequest(
    val query: String,
    val variables: Map<String, Any> = emptyMap()
)

data class ContributionCalendar(
    val totalContributions: Int,
    val weeks: List<Week>
)

data class Week(
    val contributionDays: List<ContributionDay>
)

data class ContributionDay(
    val date: String,
    val contributionCount: Int,
    val level: String
) {
    fun getLevelInt(): Int {
        return when (level) {
            "NONE" -> 0
            "FIRST_QUARTILE" -> 1
            "SECOND_QUARTILE" -> 2
            "THIRD_QUARTILE" -> 3
            "FOURTH_QUARTILE" -> 4
            else -> 0
        }
    }
}

// Simplified model for cached data
data class CachedContributionData(
    val username: String,
    val totalContributions: Int,
    val weeks: List<Week>,
    val currentStreak: Int,
    val longestStreak: Int,
    val todayCommits: Int,
    val lastUpdated: Long
)
