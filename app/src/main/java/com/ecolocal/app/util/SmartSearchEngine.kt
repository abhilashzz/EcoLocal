package com.ecolocal.app.util

import com.ecolocal.app.model.CommunityService
import com.ecolocal.app.model.MarketplaceListing
import kotlin.math.min

/**
 * AI Smart Search Engine
 * Client-side explainable ranking algorithm that intelligently matches queries across
 * Marketplace listings and Community Services using normalization, tokenization,
 * synonym expansion, fuzzy Levenshtein distance, category/location relevance, and proximity bonuses.
 * Does not use external AI APIs.
 */
object SmartSearchEngine {

    data class ScoredListing(
        val listing: MarketplaceListing,
        val score: Int,
        val distanceKm: Double? = null,
        val matchReasons: List<String> = emptyList()
    )

    data class ScoredService(
        val service: CommunityService,
        val score: Int,
        val distanceKm: Double? = null,
        val matchReasons: List<String> = emptyList()
    )

    private val synonymMap = mapOf(
        "phone" to listOf("mobile", "smartphone", "android", "iphone", "cell"),
        "mobile" to listOf("phone", "smartphone", "android"),
        "smartphone" to listOf("phone", "mobile", "android"),
        "sofa" to listOf("couch", "settee", "seating", "furniture"),
        "couch" to listOf("sofa", "settee", "chair"),
        "desk" to listOf("table", "study table", "workstation", "furniture"),
        "table" to listOf("desk", "study table", "dining table"),
        "books" to listOf("book", "textbook", "notes", "readings", "bundle"),
        "book" to listOf("books", "textbook", "novel"),
        "textbook" to listOf("book", "books", "notes", "study"),
        "repair" to listOf("fix", "maintenance", "servicing", "service", "plumbing", "electrical"),
        "fix" to listOf("repair", "maintenance", "servicing"),
        "maintenance" to listOf("repair", "fix", "servicing", "care"),
        "teacher" to listOf("tutor", "tutoring", "lessons", "classes", "coaching"),
        "tutor" to listOf("teacher", "tutoring", "lessons", "math", "science", "english", "classes"),
        "tutoring" to listOf("teacher", "tutor", "lessons", "classes", "math"),
        "free" to listOf("giveaway", "give away", "donation", "donate", "0"),
        "giveaway" to listOf("free", "give away", "donation"),
        "donate" to listOf("donation", "giveaway", "free"),
        "fan" to listOf("ceiling fan", "cooler", "appliance"),
        "cooker" to listOf("rice cooker", "appliance", "kitchen", "electronic"),
        "chair" to listOf("seat", "couch", "sofa", "stool"),
        "clothes" to listOf("clothing", "shirt", "dress", "pants", "wear"),
        "clothing" to listOf("clothes", "shirt", "dress", "wear"),
        "lawn" to listOf("grass", "garden", "mowing", "clearing", "yard"),
        "mowing" to listOf("lawn", "grass", "garden", "maintenance")
    )

    fun searchMarketplace(
        query: String,
        categoryFilter: String = "All",
        typeFilter: String = "All",
        userLat: Double? = null,
        userLon: Double? = null,
        userPreferredCategories: List<String> = emptyList(),
        items: List<MarketplaceListing>
    ): List<ScoredListing> {
        val trimmedQuery = query.trim().lowercase()
        val queryTokens = tokenize(trimmedQuery)
        val expandedTokens = expandWithSynonyms(queryTokens)

        val results = mutableListOf<ScoredListing>()

        for (item in items) {
            if (item.status == "DELETED") continue

            // Category filter check
            if (!categoryFilter.equals("All", ignoreCase = true) &&
                !item.category.equals(categoryFilter, ignoreCase = true)
            ) {
                continue
            }

            // Type filter check
            if (!typeFilter.equals("All", ignoreCase = true)) {
                val normalizedItemType = when (item.listingType.uppercase()) {
                    "FOR SALE" -> "For Sale"
                    "FREE", "GIVE AWAY" -> "Free"
                    "DONATION" -> "Donation"
                    else -> item.listingType
                }
                if (!normalizedItemType.equals(typeFilter, ignoreCase = true) &&
                    !item.listingType.equals(typeFilter, ignoreCase = true)
                ) {
                    continue
                }
            }

            var score = 0
            val reasons = mutableListOf<String>()

            // Compute distance if coordinates available
            var distanceKm: Double? = null
            if (userLat != null && userLon != null) {
                val coords = LocationHelper.getListingCoordinates(item)
                if (coords != null) {
                    val d = LocationHelper.calculateDistanceKm(userLat, userLon, coords.first, coords.second)
                    distanceKm = d
                    if (d <= LocationHelper.NEARBY_RADIUS_KM) {
                        score += 10
                        reasons.add("Near you (${LocationHelper.formatDistance(d)})")
                    }
                }
            }

            if (trimmedQuery.isEmpty()) {
                // Return all matching filters, ordered by date & nearby
                score += 1
                results.add(ScoredListing(item, score, distanceKm, reasons))
                continue
            }

            val titleLower = item.title.lowercase()
            val descLower = item.description.lowercase()
            val catLower = item.category.lowercase()
            val locLower = item.locationName.lowercase()

            // 1. Exact title match (50)
            if (titleLower == trimmedQuery) {
                score += 50
                reasons.add("Exact title match")
            } else if (titleLower.contains(trimmedQuery)) {
                score += 40
                reasons.add("Title contains search")
            } else {
                // Token matches in title (40)
                var titleTokenMatch = false
                for (token in queryTokens) {
                    if (titleLower.contains(token) || token.contains(titleLower)) {
                        titleTokenMatch = true
                        break
                    }
                }
                if (titleTokenMatch) {
                    score += 40
                    reasons.add("Matches title keywords")
                }
            }

            // 2. Category match (25)
            if (catLower.contains(trimmedQuery) || queryTokens.any { catLower.contains(it) }) {
                score += 25
                reasons.add("Category: ${item.category}")
            }

            // 3. Location match (20)
            if (locLower.contains(trimmedQuery) || queryTokens.any { locLower.contains(it) }) {
                score += 20
                reasons.add("Location match: ${item.locationName}")
            }

            // 4. Description match (15)
            if (descLower.contains(trimmedQuery) || queryTokens.any { descLower.contains(it) }) {
                score += 15
                reasons.add("Description match")
            }

            // 5. Fuzzy / Synonym match (10)
            var synonymMatched = false
            for (syn in expandedTokens) {
                if (titleLower.contains(syn) || descLower.contains(syn) || catLower.contains(syn)) {
                    synonymMatched = true
                    break
                }
            }
            if (!synonymMatched) {
                // Levenshtein fuzzy check against title words
                val titleWords = tokenize(titleLower)
                for (qToken in queryTokens) {
                    if (qToken.length >= 4) {
                        for (w in titleWords) {
                            if (w.length >= 4 && levenshteinDistance(qToken, w) <= 1) {
                                synonymMatched = true
                                break
                            }
                        }
                    }
                    if (synonymMatched) break
                }
            }
            if (synonymMatched && score < 30) {
                score += 10
                reasons.add("Intelligent synonym/fuzzy match")
            }

            // 6. User preference bonus (10)
            if (userPreferredCategories.any { it.equals(item.category, ignoreCase = true) }) {
                score += 10
                reasons.add("Matches your interests")
            }

            if (score > 0) {
                results.add(ScoredListing(item, score, distanceKm, reasons))
            }
        }

        return results.sortedWith(
            compareByDescending<ScoredListing> { it.score }
                .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                .thenByDescending { it.listing.createdAt }
        )
    }

    fun searchServices(
        query: String,
        serviceType: String? = null, // "SERVICE_OFFER" or "HELP_REQUEST"
        categoryFilter: String = "All",
        userLat: Double? = null,
        userLon: Double? = null,
        userPreferredCategories: List<String> = emptyList(),
        items: List<CommunityService>
    ): List<ScoredService> {
        val trimmedQuery = query.trim().lowercase()
        val queryTokens = tokenize(trimmedQuery)
        val expandedTokens = expandWithSynonyms(queryTokens)

        val results = mutableListOf<ScoredService>()

        for (item in items) {
            // Type separation check
            if (!serviceType.isNullOrBlank() && !item.serviceType.equals(serviceType, ignoreCase = true)) {
                continue
            }

            // Category filter check
            if (!categoryFilter.equals("All", ignoreCase = true) &&
                !item.category.equals(categoryFilter, ignoreCase = true)
            ) {
                continue
            }

            var score = 0
            val reasons = mutableListOf<String>()

            // Proximity bonus
            var distanceKm: Double? = null
            if (userLat != null && userLon != null) {
                val coords = LocationHelper.getServiceCoordinates(item)
                if (coords != null) {
                    val d = LocationHelper.calculateDistanceKm(userLat, userLon, coords.first, coords.second)
                    distanceKm = d
                    if (d <= LocationHelper.NEARBY_RADIUS_KM) {
                        score += 10
                        reasons.add("Near you (${LocationHelper.formatDistance(d)})")
                    }
                }
            }

            if (trimmedQuery.isEmpty()) {
                score += 1
                results.add(ScoredService(item, score, distanceKm, reasons))
                continue
            }

            val titleLower = item.title.lowercase()
            val descLower = item.description.lowercase()
            val catLower = item.category.lowercase()
            val locLower = item.locationName.lowercase()
            val providerLower = item.providerName.lowercase()

            // Title match
            if (titleLower == trimmedQuery) {
                score += 50
                reasons.add("Exact title match")
            } else if (titleLower.contains(trimmedQuery)) {
                score += 40
                reasons.add("Title keyword match")
            } else if (queryTokens.any { titleLower.contains(it) }) {
                score += 40
                reasons.add("Title keyword match")
            }

            // Category match (25)
            if (catLower.contains(trimmedQuery) || queryTokens.any { catLower.contains(it) }) {
                score += 25
                reasons.add("Category: ${item.category}")
            }

            // Location match (20)
            if (locLower.contains(trimmedQuery) || queryTokens.any { locLower.contains(it) }) {
                score += 20
                reasons.add("Location: ${item.locationName}")
            }

            // Provider name match (15)
            if (providerLower.contains(trimmedQuery) || queryTokens.any { providerLower.contains(it) }) {
                score += 15
                reasons.add("Provider match: ${item.providerName}")
            }

            // Description match (15)
            if (descLower.contains(trimmedQuery) || queryTokens.any { descLower.contains(it) }) {
                score += 15
                reasons.add("Description match")
            }

            // Synonym / Fuzzy match (10)
            if (expandedTokens.any { titleLower.contains(it) || descLower.contains(it) || catLower.contains(it) }) {
                score += 10
                reasons.add("Intelligent synonym match")
            }

            // Preference bonus (10)
            if (userPreferredCategories.any { it.equals(item.category, ignoreCase = true) }) {
                score += 10
                reasons.add("Matches your interests")
            }

            if (score > 0) {
                results.add(ScoredService(item, score, distanceKm, reasons))
            }
        }

        return results.sortedWith(
            compareByDescending<ScoredService> { it.score }
                .thenBy { it.distanceKm ?: Double.MAX_VALUE }
                .thenByDescending { it.service.createdAt }
        )
    }

    private fun tokenize(text: String): List<String> {
        return text.split(Regex("[\\s,;:.!?/-]+"))
            .map { it.trim().lowercase() }
            .filter { it.length >= 2 }
    }

    private fun expandWithSynonyms(tokens: List<String>): Set<String> {
        val expanded = mutableSetOf<String>()
        for (token in tokens) {
            synonymMap[token]?.let { expanded.addAll(it) }
            for ((key, syns) in synonymMap) {
                if (syns.contains(token)) {
                    expanded.add(key)
                    expanded.addAll(syns)
                }
            }
        }
        return expanded
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }
}
