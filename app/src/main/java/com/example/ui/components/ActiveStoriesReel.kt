package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserProfile
import com.example.model.UserStory
import com.example.ui.theme.*

/**
 * Modern Dating Story Avatars Bar (Instagram / Telegram Story style)
 * - Displays only followed candidates' stories + current user's own story.
 * - Groups multiple stories from the same author into a SINGLE avatar circle.
 * - Shows multi-story count badge (e.g. "2 ta").
 */
@Composable
fun ActiveStoriesReel(
    currentUser: UserProfile?,
    allStories: List<UserStory>,
    followedCandidateIds: Set<String>,
    candidates: List<UserProfile>,
    isDark: Boolean,
    onMyStoryClick: () -> Unit,
    onStoryGroupClick: (List<UserStory>) -> Unit,
    onCandidateClick: (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val myActiveStories = allStories.filter {
        (it.authorId == (currentUser?.id ?: "user_me") || it.authorId == "user_me" || it.authorId == "me") && it.isStoryActive()
    }

    // Group followed users' active stories by authorId
    val followedActiveStories = allStories.filter { story ->
        val isMe = story.authorId == (currentUser?.id ?: "user_me") || story.authorId == "user_me" || story.authorId == "me"
        !isMe && (followedCandidateIds.contains(story.authorId) || followedCandidateIds.contains("cand_${story.authorId}")) && story.isStoryActive()
    }

    val groupedFollowedStories = followedActiveStories.groupBy { it.authorId }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Current User "Hikoyangiz" Avatar
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onMyStoryClick() }
                    .testTag("btn_add_my_story")
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (myActiveStories.isNotEmpty()) {
                        // Glowing Gradient Ring if user has active stories
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(SoftPeach, SoftRosePink, WarmHoneyGold, SoftPeach)
                                    )
                                )
                                .padding(2.5.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF13091B) else Color.White)
                                .padding(2.dp)
                        ) {
                            ProfileAvatar(
                                photoPath = currentUser?.photoPath ?: "",
                                gender = currentUser?.gender ?: "Erkak",
                                size = 56.dp,
                                showCameraBadge = false
                            )
                        }

                        // Badge showing story count
                        Surface(
                            shape = CircleShape,
                            color = SoftPeach,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${myActiveStories.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Regular avatar with plus icon badge
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, SoftGlassBorderDark, CircleShape)
                        ) {
                            ProfileAvatar(
                                photoPath = currentUser?.photoPath ?: "",
                                gender = currentUser?.gender ?: "Erkak",
                                size = 60.dp,
                                showCameraBadge = false
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SoftPeach, SoftRosePink)))
                                .border(1.5.dp, if (isDark) Color(0xFF13091B) else Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Hikoya qo'shish",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hikoyangiz",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFFD4B8C7) else TextSecondary,
                    maxLines = 1
                )
            }
        }

        // 2. Followed Users' Grouped Stories
        items(groupedFollowedStories.entries.toList(), key = { "group_${it.key}" }) { entry ->
            val authorStories = entry.value
            val firstStory = authorStories.first()
            val candidateObj = candidates.find { it.id == entry.key }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onStoryGroupClick(authorStories) }
                    .testTag("story_avatar_${entry.key}")
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Glowing Gradient Ring
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        SoftPeach,
                                        SoftRosePink,
                                        WarmHoneyGold,
                                        SoftPeach
                                    )
                                )
                            )
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF13091B) else Color.White)
                            .padding(2.dp)
                    ) {
                        ProfileAvatar(
                            photoPath = firstStory.authorPhotoPath,
                            gender = firstStory.authorGender,
                            size = 56.dp,
                            showCameraBadge = false
                        )
                    }

                    // Multi-story badge if > 1 stories
                    if (authorStories.size > 1) {
                        Surface(
                            shape = CircleShape,
                            color = SoftRosePink,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${authorStories.size}",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Online indicator dot
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                                .border(1.5.dp, if (isDark) Color(0xFF13091B) else Color.White, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.widthIn(max = 68.dp)
                ) {
                    Text(
                        text = firstStory.authorName.split(" ").firstOrNull() ?: firstStory.authorName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
