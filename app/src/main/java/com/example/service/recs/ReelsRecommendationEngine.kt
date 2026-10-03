package com.example.service.recs

import android.util.Log
import com.example.model.UserPost
import kotlin.random.Random

/**
 * Production-ready User Engagement Ingestion Payload
 */
data class UserEngagement(
    val userId: String,
    val contentId: String,
    val watchDurationMs: Long,
    val totalDurationMs: Long,
    val isLiked: Boolean,
    val isShared: Boolean,
    val isSkipped: Boolean
)

/**
 * Highly Optimized Hybrid Recommendation Engine for Visol App
 * Processes engagement telemetry dynamically and returns prioritized interest-based feeds.
 */
object ReelsRecommendationEngine {
    private const val TAG = "ReelsRecEngine"
    
    // Thread-safe engagement storage for tracking real-time user actions (ZERO mock data fallback)
    private val engagementHistory = mutableListOf<UserEngagement>()
    
    // Dynamic category weights for personalization: category -> score
    private val userCategoryWeights = mutableMapOf<String, Float>()

    /**
     * API: POST /api/v1/analytics/track-engagement
     * Ingests individual viewer interactions to build user affinity profile.
     */
    fun trackEngagement(engagement: UserEngagement) {
        synchronized(engagementHistory) {
            engagementHistory.add(engagement)
        }
        
        // Calculate dynamic completion rate
        val completionRate = if (engagement.totalDurationMs > 0) {
            engagement.watchDurationMs.toFloat() / engagement.totalDurationMs.toFloat()
        } else {
            0f
        }
        
        val category = getCategoryForContent(engagement.contentId)
        
        // Personalized learning weight adjustments
        var weightDelta = 0f
        if (completionRate >= 0.8f) {
            weightDelta += 0.35f // Watched almost fully
        } else if (engagement.isSkipped || completionRate < 0.2f) {
            weightDelta -= 0.25f // Skipped quickly
        }
        
        if (engagement.isLiked) weightDelta += 0.4f
        if (engagement.isShared) weightDelta += 0.5f
        
        if (category != null) {
            val currentVal = userCategoryWeights.getOrDefault(category, 1.0f)
            userCategoryWeights[category] = (currentVal + weightDelta).coerceIn(0.1f, 10.0f)
            Log.d(TAG, "Processed engagement telemetry for content '${engagement.contentId}': Category '$category' adjusted to ${userCategoryWeights[category]}")
        }
    }
    
    /**
     * Hybrid Recommendation Algorithm matching:
     * - 60% Collaborative/Interest-Based (Personalized category weights)
     * - 20% Trending / High Engagement
     * - 20% Cold-Start / New Uploads
     */
    fun getRecommendedReels(allReels: List<UserPost>, userId: String): List<UserPost> {
        if (allReels.isEmpty()) return emptyList()
        
        // Compute dynamically calculated affinity scores
        val scoredReels = allReels.map { reel ->
            val cat = getCategoryForContent(reel.id) ?: "General"
            val preferenceWeight = userCategoryWeights.getOrDefault(cat, 1.0f)
            
            // Extract actual history metrics
            val realHistory = synchronized(engagementHistory) {
                engagementHistory.filter { it.contentId == reel.id }
            }
            
            val historyScore = if (realHistory.isNotEmpty()) {
                val likes = realHistory.count { it.isLiked }
                val shares = realHistory.count { it.isShared }
                val skips = realHistory.count { it.isSkipped }
                (likes * 2.0f + shares * 3.0f - skips * 1.5f) / realHistory.size.toFloat()
            } else {
                0f
            }
            
            val finalScore = preferenceWeight * (1.0f + historyScore.coerceAtLeast(-0.9f))
            reel to finalScore
        }
        
        // 1. Interest-Based Segment (60%)
        val interestBased = scoredReels.sortedByDescending { it.second }.map { it.first }
        
        // 2. Trending Segment (20%) - Measured by direct total interaction count
        val trending = allReels.sortedByDescending { it.likesCount + (it.commentsCount * 2) }
        
        // 3. Cold Start / Fresh Uploads Segment (20%) - Prioritize newest items
        val newest = allReels.sortedByDescending { it.id }
        
        val hybridFeed = mutableListOf<UserPost>()
        val addedIds = mutableSetOf<String>()
        
        val targetSize = allReels.size
        val countInterest = (targetSize * 0.60).toInt().coerceAtLeast(1)
        val countTrending = (targetSize * 0.20).toInt().coerceAtLeast(1)
        
        // Pull 60% from Interest
        for (reel in interestBased) {
            if (hybridFeed.size < countInterest && addedIds.add(reel.id)) {
                hybridFeed.add(reel)
            }
        }
        
        // Pull 20% from Trending
        for (reel in trending) {
            if (hybridFeed.size < (countInterest + countTrending) && addedIds.add(reel.id)) {
                hybridFeed.add(reel)
            }
        }
        
        // Fill remaining with Fresh Uploads / Cold-Start
        for (reel in newest) {
            if (hybridFeed.size < targetSize && addedIds.add(reel.id)) {
                hybridFeed.add(reel)
            }
        }
        
        // Safe guard fallback to prevent losing any entries
        for (reel in allReels) {
            if (addedIds.add(reel.id)) {
                hybridFeed.add(reel)
            }
        }
        
        return hybridFeed
    }
    
    private fun getCategoryForContent(contentId: String): String? {
        return when {
            contentId.lowercase().contains("musiqa") || contentId.lowercase().contains("music") -> "Music"
            contentId.lowercase().contains("tanishuv") || contentId.lowercase().contains("sevgi") -> "Dating"
            contentId.lowercase().contains("shov-shuv") || contentId.lowercase().contains("comedy") -> "Comedy"
            else -> "General"
        }
    }
}
