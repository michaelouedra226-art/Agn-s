package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.core.design.AnimatedCinemaBackground
import com.example.core.design.CinColors
import com.example.core.log.LogManager
import com.example.core.ratelimit.RateLimiter
import com.example.data.api.AgnesApiClient
import com.example.data.api.ApiKeyManager
import com.example.data.api.GeminiApiClient
import com.example.data.database.AppDatabase
import com.example.data.repository.FilmRepository
import com.example.data.repository.MediaRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import com.example.ui.film.FilmScreen
import com.example.ui.film.FilmViewModel
import com.example.ui.gallery.GalleryScreen
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.header.CinTopBar
import com.example.ui.image.ImageScreen
import com.example.ui.image.ImageViewModel
import com.example.ui.log.LogFloatingPanel
import com.example.ui.navigation.MainTab
import com.example.ui.navigation.Screen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.CineIATheme
import com.example.ui.video.VideoScreen
import com.example.ui.video.VideoViewModel
import com.example.ui.viewer.UniversalMediaViewer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val filmRepository = FilmRepository(database)
        val mediaRepository = MediaRepository(database)
        val settingsRepository = SettingsRepository(applicationContext)
        val apiKeyManager = ApiKeyManager(applicationContext)
        val geminiApiClient = GeminiApiClient(apiKeyManager)
        val agnesApiClient = AgnesApiClient(applicationContext, apiKeyManager)

        val imageViewModel = ImageViewModel(geminiApiClient, agnesApiClient, mediaRepository)
        val videoViewModel = VideoViewModel(agnesApiClient, mediaRepository)
        val filmViewModel = FilmViewModel(filmRepository, mediaRepository, geminiApiClient, agnesApiClient, apiKeyManager)
        val galleryViewModel = GalleryViewModel(mediaRepository)
        val settingsViewModel = SettingsViewModel(apiKeyManager, settingsRepository)

        LogManager.info("Système", "CinéIA Studio démarré avec succès. Moteur prêt.")

        setContent {
            val userSettings by settingsRepository.settings.collectAsState()
            val unreadLogCount by LogManager.unreadCount.collectAsState()
            val hasLogError by LogManager.hasUnreadError.collectAsState()
            val filmUiState by filmViewModel.uiState.collectAsState()
            val imageUiState by imageViewModel.uiState.collectAsState()
            val videoUiState by videoViewModel.uiState.collectAsState()
            val quotaState by RateLimiter.quotaState.collectAsState()

            var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
            var currentTab by remember { mutableStateOf(MainTab.FILM) }
            var selectedViewerMedia by remember { mutableStateOf<MediaItem?>(null) }
            var showLogPanel by remember { mutableStateOf(false) }

            val isGenerating = filmUiState.isGenerating || imageUiState.isGenerating || videoUiState.isGenerating

            // Back button handling
            BackHandler(enabled = currentScreen != Screen.Main || selectedViewerMedia != null || showLogPanel) {
                when {
                    showLogPanel -> showLogPanel = false
                    selectedViewerMedia != null -> selectedViewerMedia = null
                    currentScreen != Screen.Main -> currentScreen = Screen.Main
                }
            }

            CineIATheme(darkTheme = userSettings.darkTheme) {
                AnimatedCinemaBackground(
                    isGenerating = isGenerating,
                    hasError = hasLogError,
                    particlesEnabled = userSettings.particlesEnabled && userSettings.animatedBackgroundEnabled
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        topBar = {
                            val title = when (currentScreen) {
                                Screen.Gallery -> "Galerie du Studio"
                                Screen.Settings -> "Paramètres & Clés API"
                                Screen.Main -> when (currentTab) {
                                    MainTab.IMAGE -> "Studio Image"
                                    MainTab.VIDEO -> "Studio Vidéo"
                                    MainTab.FILM -> "Studio CinéIA"
                                }
                            }

                            val geminiQuota = quotaState[RateLimiter.GEMINI]
                            val subtitle = when {
                                isGenerating -> "Génération en cours..."
                                hasLogError -> "Attention : incident d'exécution"
                                geminiQuota != null -> "● Prêt · ${geminiQuota.maxRpm - geminiQuota.requestsThisMinute}/${geminiQuota.maxRpm} RPM · FR"
                                else -> "● Prêt · 10/10 RPM · FR"
                            }

                            CinTopBar(
                                title = title,
                                subtitle = subtitle,
                                canNavigateBack = currentScreen != Screen.Main,
                                onNavigateBack = { currentScreen = Screen.Main },
                                onOpenGallery = { currentScreen = Screen.Gallery },
                                onOpenLog = {
                                    showLogPanel = true
                                    LogManager.markRead()
                                },
                                onOpenSettings = { currentScreen = Screen.Settings },
                                unreadLogCount = unreadLogCount,
                                hasLogError = hasLogError,
                                isGenerating = isGenerating
                            )
                        },
                        bottomBar = {
                            if (currentScreen == Screen.Main) {
                                CinTabBar(
                                    activeTab = currentTab,
                                    onTabSelected = { currentTab = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                Screen.Gallery -> GalleryScreen(
                                    viewModel = galleryViewModel,
                                    onMediaSelected = { selectedViewerMedia = it }
                                )
                                Screen.Settings -> SettingsScreen(viewModel = settingsViewModel)
                                Screen.Main -> when (currentTab) {
                                    MainTab.IMAGE -> ImageScreen(
                                        viewModel = imageViewModel,
                                        onMediaSelected = { selectedViewerMedia = it },
                                        onAnimateImage = { item ->
                                            videoViewModel.setImageSource(item.path)
                                            currentTab = MainTab.VIDEO
                                        }
                                    )
                                    MainTab.VIDEO -> VideoScreen(
                                        viewModel = videoViewModel,
                                        onMediaSelected = { selectedViewerMedia = it }
                                    )
                                    MainTab.FILM -> FilmScreen(
                                        viewModel = filmViewModel,
                                        onFilmCompleted = { completedFilm ->
                                            selectedViewerMedia = MediaItem(
                                                id = completedFilm.id,
                                                type = MediaType.FILM,
                                                path = completedFilm.thumbnailPath ?: "",
                                                title = completedFilm.title,
                                                prompt = completedFilm.prompt,
                                                durationMs = (completedFilm.duration * 1000).toLong()
                                            )
                                        }
                                    )
                                }
                            }

                            // Universal Media Viewer Overlay
                            AnimatedVisibility(
                                visible = selectedViewerMedia != null,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                selectedViewerMedia?.let { media ->
                                    UniversalMediaViewer(
                                        media = media,
                                        onDismiss = { selectedViewerMedia = null }
                                    )
                                }
                            }

                            // Floating Log Panel Slide-In
                            AnimatedVisibility(
                                visible = showLogPanel,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                LogFloatingPanel(onDismiss = { showLogPanel = false })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CinTabBar(
    activeTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CinColors.BgSurface.copy(alpha = 0.95f))
            .border(1.dp, CinColors.BorderDefault, RoundedCornerShape(24.dp))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CinTabItem(
                title = "Image",
                icon = Icons.Default.Image,
                isSelected = activeTab == MainTab.IMAGE,
                onClick = { onTabSelected(MainTab.IMAGE) }
            )

            CinTabItem(
                title = "Vidéo",
                icon = Icons.Default.Videocam,
                isSelected = activeTab == MainTab.VIDEO,
                onClick = { onTabSelected(MainTab.VIDEO) }
            )

            CinTabItem(
                title = "Film",
                icon = Icons.Default.Movie,
                isSelected = activeTab == MainTab.FILM,
                onClick = { onTabSelected(MainTab.FILM) }
            )
        }
    }
}

@Composable
private fun CinTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgModifier = if (isSelected) {
        Modifier.background(CinColors.GradientFilm, RoundedCornerShape(16.dp))
    } else {
        Modifier.background(Color.Transparent)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else CinColors.TextSecond,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) Color.White else CinColors.TextSecond,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
