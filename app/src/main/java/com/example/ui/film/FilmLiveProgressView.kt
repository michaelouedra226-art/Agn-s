package com.example.ui.film

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.design.CinButton
import com.example.core.design.CinButtonStyle
import com.example.core.design.CinCard
import com.example.core.design.CinColors
import com.example.core.design.CinProgressBar
import com.example.core.design.CinStatusBadge
import com.example.domain.model.FilmScene
import com.example.domain.model.GenerationPhase
import java.io.File

@Composable
fun FilmLiveProgressView(
    uiState: FilmUiState,
    onSelectScene: (Int) -> Unit,
    onTogglePause: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllScenes by remember { mutableStateOf(false) }
    val scenes = uiState.currentFilm?.scenes ?: emptyList()
    val activeScene = scenes.find { it.sceneIndex == uiState.activeSceneIndex } ?: scenes.firstOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        // Overall Progress Card
        CinCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = uiState.currentFilm?.title ?: "Génération en cours",
                            color = CinColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = uiState.phaseDetailMessage,
                            color = CinColors.TextSecond,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "${uiState.overallProgressPercent}%",
                        color = CinColors.AccentPink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                CinProgressBar(progress = uiState.overallProgressPercent / 100f)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Phases Indicator
        CinCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PhaseRow("Phase 1 : Scénario & Découpage", isDone = uiState.overallProgressPercent >= 25, isCurrent = uiState.currentPhase == GenerationPhase.SCRIPT)
                PhaseRow("Phase 2 : Rendu des images clés (${uiState.currentFilm?.scenes?.count { it.imagePath != null } ?: 0}/${uiState.currentFilm?.totalScenes ?: 0})", isDone = uiState.overallProgressPercent >= 50, isCurrent = uiState.currentPhase == GenerationPhase.IMAGES)
                PhaseRow("Phase 3 : Animation vidéo des plans (${uiState.currentFilm?.completedScenes ?: 0}/${uiState.currentFilm?.totalScenes ?: 0})", isDone = uiState.overallProgressPercent >= 90, isCurrent = uiState.currentPhase == GenerationPhase.VIDEOS)
                PhaseRow("Phase 4 : Assemblage et sonorisation", isDone = uiState.overallProgressPercent == 100, isCurrent = uiState.currentPhase == GenerationPhase.ASSEMBLY)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Scene Timeline
        Text(
            text = "Chronologie des scènes",
            color = CinColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            scenes.forEach { sc ->
                val isSelected = sc.sceneIndex == uiState.activeSceneIndex
                val isDone = sc.status == "done"
                val isImageDone = sc.imagePath != null

                val bgModifier = if (isSelected) {
                    Modifier.background(CinColors.GradientFilm, RoundedCornerShape(10.dp))
                } else {
                    Modifier.background(CinColors.BgElevated, RoundedCornerShape(10.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .then(bgModifier)
                        .border(1.dp, if (isSelected) CinColors.AccentPink else CinColors.BorderDefault, RoundedCornerShape(10.dp))
                        .clickable { onSelectScene(sc.sceneIndex) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Scène ${sc.sceneIndex}",
                            color = if (isSelected) Color.White else CinColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CinColors.Success,
                                modifier = Modifier.size(14.dp)
                            )
                        } else if (isImageDone) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CinColors.AccentCyan))
                        } else {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CinColors.TextTertiary))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Active Scene Details Card
        if (activeScene != null) {
            CinCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeScene.title,
                            color = CinColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CinStatusBadge(status = activeScene.status)
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = activeScene.description,
                        color = CinColors.TextSecond,
                        fontSize = 12.sp
                    )

                    if (activeScene.dialogue.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CinColors.BgVoid)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "💬 ${activeScene.dialogue}",
                                color = CinColors.AccentCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 16:9 Image Preview
                    if (activeScene.imagePath != null) {
                        val file = File(activeScene.imagePath)
                        AsyncImage(
                            model = file,
                            contentDescription = activeScene.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CinColors.BgVoid),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "En attente de génération d'image...",
                                color = CinColors.TextTertiary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scene controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CinButton(
                            text = "⏭ Passer",
                            onClick = { onSelectScene((activeScene.sceneIndex % scenes.size) + 1) },
                            style = CinButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                        CinButton(
                            text = if (uiState.isPaused) "Reprendre" else "Pause",
                            onClick = onTogglePause,
                            style = CinButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // All Scenes Accordion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showAllScenes = !showAllScenes }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Voir toutes les scènes (${scenes.size})",
                color = CinColors.TextSecond,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (showAllScenes) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = CinColors.TextSecond
            )
        }

        AnimatedVisibility(visible = showAllScenes) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                scenes.forEach { sc ->
                    SceneCompactCard(sc, onClick = { onSelectScene(sc.sceneIndex) })
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Global Control Buttons: Pause & Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CinButton(
                text = if (uiState.isPaused) "▶ Reprendre" else "⏸ Pause globale",
                onClick = onTogglePause,
                style = CinButtonStyle.SECONDARY,
                modifier = Modifier.weight(1f),
                testTag = "film_toggle_pause_button"
            )

            CinButton(
                text = "✕ Annuler",
                onClick = onCancel,
                style = CinButtonStyle.DANGER,
                modifier = Modifier.weight(1f),
                testTag = "film_cancel_button"
            )
        }
    }
}

@Composable
private fun PhaseRow(label: String, isDone: Boolean, isCurrent: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (isDone) CinColors.Success else if (isCurrent) CinColors.AccentPink else CinColors.BgElevated),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            } else if (isCurrent) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.White))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isCurrent) Color.White else if (isDone) CinColors.TextPrimary else CinColors.TextTertiary,
            fontSize = 12.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SceneCompactCard(scene: FilmScene, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CinColors.BgElevated)
            .border(1.dp, CinColors.BorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Scène ${scene.sceneIndex} : ${scene.title}",
                color = CinColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = scene.cameraMovement,
                color = CinColors.TextTertiary,
                fontSize = 11.sp
            )
        }
        CinStatusBadge(status = scene.status)
    }
}
