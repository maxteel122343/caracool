package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppSettings
import com.example.data.model.RankedUser
import com.example.ui.theme.NaturalAccentDark
import com.example.ui.theme.NaturalAccentGold
import com.example.ui.theme.NaturalBorderLight
import com.example.ui.theme.NaturalBorderSubtle
import com.example.ui.theme.NaturalTextMuted
import com.example.ui.theme.NaturalTextPrimary
import com.example.ui.theme.NaturalTextSecondary
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeUnlockRankingCard(
    rankedUsers: List<RankedUser>,
    currentPeriod: String,
    settings: AppSettings,
    onPeriodChange: (String) -> Unit,
    onPerformUnlock: () -> Unit,
    onNavigateToFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCuMode = settings.isCaraDeKoolMode
    var isExpanded by remember { mutableStateOf(false) }
    var selectedUserForDetail by remember { mutableStateOf<RankedUser?>(null) }
    var showCheerFeedback by remember { mutableStateOf(false) }

    val top1 = rankedUsers.getOrNull(0)
    val top2 = rankedUsers.getOrNull(1)
    val top3 = rankedUsers.getOrNull(2)
    val remainingUsers = if (rankedUsers.size > 3) rankedUsers.subList(3, rankedUsers.size) else emptyList()
    val currentUserRank = rankedUsers.find { it.isCurrentUser }

    val primaryThemeColor = MaterialTheme.colorScheme.primary
    val secondaryThemeColor = MaterialTheme.colorScheme.secondary
    val bgCardColor = MaterialTheme.colorScheme.surface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_ranking_card"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            1.2.dp,
            MaterialTheme.colorScheme.outline
        ),
        colors = CardDefaults.cardColors(containerColor = bgCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // 1. Header: Icon, Title, and Period Tabs (Hoje vs Geral)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCuMode) {
                            TardigradeMascotIcon(size = 24.dp)
                        } else {
                            Text("🏆", fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isCuMode) "Ranking Cara de Cu 👾" else "Ranking de Desbloqueios 🏆",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Quem mais desbloqueou o aparelho",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Filter Switch Pills (Hoje / Geral)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isToday = currentPeriod == "today"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isToday) primaryThemeColor else Color.Transparent)
                                .clickable { onPeriodChange("today") }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hoje",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (!isToday) primaryThemeColor else Color.Transparent)
                                .clickable { onPeriodChange("all_time") }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Geral",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!isToday) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Podium Section (Top 1, Top 2, Top 3)
            if (top1 != null) {
                PodiumSection(
                    top1 = top1,
                    top2 = top2,
                    top3 = top3,
                    isCuMode = isCuMode,
                    onUserClick = { user -> selectedUserForDetail = user }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. User's own standing banner
            if (currentUserRank != null) {
                CurrentUserRankBanner(
                    currentUser = currentUserRank,
                    top1Count = top1?.unlockCount ?: 1,
                    isCuMode = isCuMode,
                    onQuickUnlock = onPerformUnlock
                )
            }

            // 4. Remaining Ranking List (4º, 5º, etc.)
            if (remainingUsers.isNotEmpty()) {
                val displayedUsers = if (isExpanded) remainingUsers else remainingUsers.take(2)

                Spacer(modifier = Modifier.height(10.dp))

                displayedUsers.forEach { user ->
                    RankingListItem(
                        user = user,
                        isCuMode = isCuMode,
                        onUserClick = { selectedUserForDetail = user }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (remainingUsers.size > 2) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ranking_expand_btn"),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Ver menos" else "Ver ranking completo (+${remainingUsers.size - 2})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryThemeColor
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = primaryThemeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet when clicking on any user in ranking
    if (selectedUserForDetail != null) {
        val user = selectedUserForDetail!!
        UserDetailModal(
            user = user,
            isCuMode = isCuMode,
            onDismiss = { selectedUserForDetail = null },
            onFeedNavigate = {
                selectedUserForDetail = null
                onNavigateToFeed()
            }
        )
    }
}

@Composable
private fun PodiumSection(
    top1: RankedUser,
    top2: RankedUser?,
    top3: RankedUser?,
    isCuMode: Boolean,
    onUserClick: (RankedUser) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd Place (Left)
            if (top2 != null) {
                PodiumItem(
                    user = top2,
                    rank = 2,
                    badgeText = "2º",
                    medalEmoji = "🥈",
                    ringColor = Color(0xFFB0BEC5), // Silver
                    podiumHeight = 90.dp,
                    isCuMode = isCuMode,
                    modifier = Modifier.weight(1f),
                    onClick = { onUserClick(top2) }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(6.dp))

            // 1st Place (Center - Elevated & Crowned)
            PodiumItem(
                user = top1,
                rank = 1,
                badgeText = "1º",
                medalEmoji = "👑",
                ringColor = Color(0xFFFFD54F), // Gold
                podiumHeight = 115.dp,
                isCuMode = isCuMode,
                modifier = Modifier.weight(1.15f),
                onClick = { onUserClick(top1) }
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 3rd Place (Right)
            if (top3 != null) {
                PodiumItem(
                    user = top3,
                    rank = 3,
                    badgeText = "3º",
                    medalEmoji = "🥉",
                    ringColor = Color(0xFFD7CCC8), // Bronze
                    podiumHeight = 78.dp,
                    isCuMode = isCuMode,
                    modifier = Modifier.weight(1f),
                    onClick = { onUserClick(top3) }
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PodiumItem(
    user: RankedUser,
    rank: Int,
    badgeText: String,
    medalEmoji: String,
    ringColor: Color,
    podiumHeight: androidx.compose.ui.unit.Dp,
    isCuMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isTop1 = rank == 1
    val avatarSize = if (isTop1) 62.dp else 50.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        // Crown above #1
        if (isTop1) {
            Text(
                text = "👑",
                fontSize = 20.sp,
                modifier = Modifier.offset(y = 2.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Avatar with Medal Badge
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier.size(avatarSize)
        ) {
            // Profile photo or Mascot
            AvatarImage(
                photoUri = user.photoUri,
                avatarEmoji = user.avatarEmoji,
                isKool = user.isKool || isCuMode,
                size = avatarSize,
                borderColor = ringColor,
                borderWidth = if (isTop1) 3.dp else 2.dp
            )

            // Rank Pill Badge
            Box(
                modifier = Modifier
                    .offset(x = 4.dp, y = 4.dp)
                    .background(
                        color = when (rank) {
                            1 -> Color(0xFFFFB300)
                            2 -> Color(0xFF78909C)
                            else -> Color(0xFFA1887F)
                        },
                        shape = CircleShape
                    )
                    .border(1.5.dp, Color.White, CircleShape)
                    .size(if (isTop1) 22.dp else 19.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$rank",
                    color = Color.White,
                    fontSize = if (isTop1) 11.sp else 10.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // User Name
        Text(
            text = user.name,
            fontSize = if (isTop1) 12.5.sp else 11.sp,
            fontWeight = if (isTop1) FontWeight.Black else FontWeight.Bold,
            color = if (user.isCurrentUser) {
                MaterialTheme.colorScheme.primary
            } else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // Unlock count badge
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isTop1) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            modifier = Modifier.padding(top = 3.dp)
        ) {
            Text(
                text = "${user.unlockCount} 🔓",
                fontSize = if (isTop1) 11.sp else 10.sp,
                fontWeight = FontWeight.Black,
                color = if (isTop1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun RankingListItem(
    user: RankedUser,
    isCuMode: Boolean,
    onUserClick: () -> Unit
) {
    val isCurrentUser = user.isCurrentUser
    val itemBg = if (isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = itemBg,
        border = if (isCurrentUser) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onUserClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Number
            Text(
                text = "#${user.rank}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )

            // User Avatar Photo
            AvatarImage(
                photoUri = user.photoUri,
                avatarEmoji = user.avatarEmoji,
                isKool = user.isKool || isCuMode,
                size = 38.dp,
                borderColor = if (isCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Name and Title
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        fontSize = 13.sp,
                        fontWeight = if (isCurrentUser) FontWeight.Black else FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(Você)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = user.badgeTitle,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            // Unlocks Count
            Box(
                modifier = Modifier
                    .background(
                        if (isCurrentUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${user.unlockCount} 🔓",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CurrentUserRankBanner(
    currentUser: RankedUser,
    top1Count: Int,
    isCuMode: Boolean,
    onQuickUnlock: () -> Unit
) {
    val diffToTop = (top1Count - currentUser.unlockCount + 1).coerceAtLeast(1)
    val bannerText = if (currentUser.rank == 1) {
        "Parabéns! Você é o #1 do ranking de desbloqueios! 🏆👑"
    } else {
        "Sua posição: #${currentUser.rank} • Desbloqueie mais $diffToTop vezes para alcançar a liderança! 🚀"
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentUser.rank == 1) "👑" else "⚡",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = bannerText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = onQuickUnlock,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun AvatarImage(
    photoUri: String?,
    avatarEmoji: String,
    isKool: Boolean,
    size: androidx.compose.ui.unit.Dp,
    borderColor: Color,
    borderWidth: androidx.compose.ui.unit.Dp = 1.5.dp
) {
    val context = LocalContext.current
    val isPhotoValid = remember(photoUri) {
        if (photoUri.isNullOrBlank()) false
        else if (photoUri.startsWith("http://") || photoUri.startsWith("https://") || photoUri.startsWith("content://") || photoUri.startsWith("data:image")) true
        else {
            val file = File(photoUri.removePrefix("file://"))
            file.exists() && file.length() > 0
        }
    }

    if (isPhotoValid && !photoUri.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photoUri)
                .crossfade(true)
                .build(),
            contentDescription = "Foto de perfil",
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(borderWidth, borderColor, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                )
                .border(borderWidth, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isKool || avatarEmoji == "👾") {
                TardigradeMascotIcon(size = size * 0.65f)
            } else {
                Text(
                    text = avatarEmoji.ifBlank { "🥜" },
                    fontSize = (size.value * 0.45f).sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserDetailModal(
    user: RankedUser,
    isCuMode: Boolean,
    onDismiss: () -> Unit,
    onFeedNavigate: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var isCheered by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Big Avatar with border
            AvatarImage(
                photoUri = user.photoUri,
                avatarEmoji = user.avatarEmoji,
                isKool = user.isKool || isCuMode,
                size = 90.dp,
                borderColor = when (user.rank) {
                    1 -> Color(0xFFFFD54F)
                    2 -> Color(0xFFB0BEC5)
                    3 -> Color(0xFFD7CCC8)
                    else -> MaterialTheme.colorScheme.primary
                },
                borderWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // User Name and Tag
            Text(
                text = user.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = user.badgeTitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rank card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Posição", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "#${user.rank}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Unlock count card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Desbloqueios", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${user.unlockCount}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { isCheered = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCheered) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isCheered) "Apoiado! 👏🎉" else "Aplaudir 👏",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                FilledTonalButton(
                    onClick = onFeedNavigate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Ver no Feed ⚡", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
