package com.example.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.AudioVoiceSettingsScreen
import com.example.ui.screens.FaceWarpFilterScreen
import com.example.ui.screens.FeedCommunityScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LockScreenView
import com.example.ui.screens.StatsHistoryScreen
import com.example.ui.screens.ThemeWallpaperScreen
import com.example.ui.theme.SoftPeachBackground
import com.example.ui.theme.SoftPeachBorder
import com.example.ui.theme.SoftPeachCard
import com.example.ui.theme.SoftRosePrimary
import com.example.ui.theme.SoftTextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class Screen(val pageIndex: Int) {
    HOME(0),
    FEED(1),
    FILTER(2),
    THEME_SETTINGS(3),
    AUDIO_SETTINGS(4),
    STATS_HISTORY(5),
    LOCK_SCREEN(-1),
    SETUP_WIZARD(-1)
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val settings by viewModel.settingsState.collectAsState()
    val isProtectorActive by viewModel.isScreenProtectorActive.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 6 })

    if (isProtectorActive) {
        LockScreenView(
            viewModel = viewModel,
            settings = settings,
            onClose = { viewModel.setScreenProtectorActive(false) }
        )
    } else {
        Scaffold(
            bottomBar = {
                NaturalBottomNavBar(
                    currentPage = pagerState.currentPage,
                    onNavigate = { screen ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(screen.pageIndex)
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // HorizontalPager allows swiping left and right across screens
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true
                ) { page ->
                    when (page) {
                        0 -> HomeScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onNavigateToAudio = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.AUDIO_SETTINGS.pageIndex) }
                            },
                            onNavigateToThemes = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.THEME_SETTINGS.pageIndex) }
                            },
                            onNavigateToStats = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.STATS_HISTORY.pageIndex) }
                            },
                            onLaunchScreenProtector = { viewModel.setScreenProtectorActive(true) },
                            onNavigateToFeed = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.FEED.pageIndex) }
                            }
                        )
                        1 -> FeedCommunityScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.HOME.pageIndex) }
                            }
                        )
                        2 -> FaceWarpFilterScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.FEED.pageIndex) }
                            },
                            onNavigateToFeed = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.FEED.pageIndex) }
                            }
                        )
                        3 -> ThemeWallpaperScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.HOME.pageIndex) }
                            }
                        )
                        4 -> AudioVoiceSettingsScreen(
                            viewModel = viewModel,
                            settings = settings,
                            onBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.HOME.pageIndex) }
                            }
                        )
                        5 -> StatsHistoryScreen(
                            viewModel = viewModel,
                            onBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(Screen.HOME.pageIndex) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NaturalBottomNavBar(
    currentPage: Int,
    onNavigate: (Screen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = SoftPeachBorder
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NaturalNavItem(
                    label = "Início",
                    icon = Icons.Default.Home,
                    isSelected = currentPage == 0,
                    onClick = { onNavigate(Screen.HOME) },
                    testTag = "nav_home"
                )

                NaturalNavItem(
                    label = "Feed",
                    icon = Icons.Default.Diversity3,
                    isSelected = currentPage == 1,
                    onClick = { onNavigate(Screen.FEED) },
                    testTag = "nav_feed"
                )

                NaturalNavItem(
                    label = "Filtro",
                    icon = Icons.Default.AutoFixHigh,
                    isSelected = currentPage == 2,
                    onClick = { onNavigate(Screen.FILTER) },
                    testTag = "nav_filter"
                )

                NaturalNavItem(
                    label = "Telas",
                    icon = Icons.Default.Wallpaper,
                    isSelected = currentPage == 3,
                    onClick = { onNavigate(Screen.THEME_SETTINGS) },
                    testTag = "nav_themes"
                )

                NaturalNavItem(
                    label = "Áudios",
                    icon = Icons.Default.Mic,
                    isSelected = currentPage == 4,
                    onClick = { onNavigate(Screen.AUDIO_SETTINGS) },
                    testTag = "nav_audio"
                )

                NaturalNavItem(
                    label = "Ajustes",
                    icon = Icons.Default.AutoGraph,
                    isSelected = currentPage == 5,
                    onClick = { onNavigate(Screen.STATS_HISTORY) },
                    testTag = "nav_stats"
                )
            }
        }
    }
}

@Composable
fun NaturalNavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (isSelected) {
                        Modifier
                            .background(SoftPeachCard, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    } else {
                        Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) SoftRosePrimary else SoftTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) SoftRosePrimary else SoftTextSecondary
        )
    }
}
