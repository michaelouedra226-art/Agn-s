package com.example.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.core.design.CinChip
import com.example.core.design.CinColors
import com.example.core.design.CinInput
import com.example.core.design.CinProgressBar
import com.example.domain.model.MediaItem
import java.io.File

@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onMediaSelected: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val cameraMovements = listOf("Statique", "Zoom in", "Zoom out", "Pan", "Orbite", "Dolly", "Tilt", "Rotate")
    val durations = listOf(Pair("4s", 4.0), Pair("6s", 6.0), Pair("8s", 8.0))
    val resolutions = listOf("720p", "1080p")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Hero Card
        CinCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CinColors.GradientFilm),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Création de Clip Vidéo",
                            color = CinColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Animez une image ou générez depuis un texte",
                            color = CinColors.TextSecond,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Selector: Image to Video vs Text to Video
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CinChip(
                text = "🖼️ Image vers Vidéo",
                selected = uiState.mode == VideoGenerationMode.IMAGE_TO_VIDEO,
                onClick = { viewModel.setMode(VideoGenerationMode.IMAGE_TO_VIDEO) },
                modifier = Modifier.weight(1f)
            )
            CinChip(
                text = "📝 Texte vers Vidéo",
                selected = uiState.mode == VideoGenerationMode.TEXT_TO_VIDEO,
                onClick = { viewModel.setMode(VideoGenerationMode.TEXT_TO_VIDEO) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.mode == VideoGenerationMode.IMAGE_TO_VIDEO) {
            // Source Image Container
            Text(
                text = "Image source à animer",
                color = CinColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (uiState.selectedImageSourcePath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, CinColors.BorderAccent, RoundedCornerShape(12.dp))
                ) {
                    val file = File(uiState.selectedImageSourcePath!!)
                    AsyncImage(
                        model = file,
                        contentDescription = "Image source",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    CinButton(
                        text = "Changer",
                        onClick = { viewModel.setImageSource(null) },
                        style = CinButtonStyle.SECONDARY,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CinColors.BgElevated)
                        .border(1.dp, CinColors.BorderDefault, RoundedCornerShape(12.dp))
                        .clickable(enabled = uiState.recentImages.isNotEmpty()) {
                            viewModel.setImageSource(uiState.recentImages.firstOrNull()?.path)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Sélectionner image",
                            tint = CinColors.AccentViolet,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.recentImages.isEmpty()) "Générez d’abord une image dans l’onglet Image" else "Touchez pour utiliser votre image la plus récente",
                            color = CinColors.TextSecond,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            if (uiState.recentImages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Images récentes — touchez pour choisir",
                    color = CinColors.TextSecond,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.recentImages, key = { it.id }) { image ->
                        Box(
                            modifier = Modifier
                                .size(width = 104.dp, height = 76.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    2.dp,
                                    if (image.path == uiState.selectedImageSourcePath) CinColors.AccentPink else CinColors.BorderDefault,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setImageSource(image.path) }
                        ) {
                            AsyncImage(
                                model = File(image.path),
                                contentDescription = "Choisir ${image.title}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        } else {
            // Text to video prompt
            Text(
                text = "Description de la scène vidéo",
                color = CinColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            CinInput(
                value = uiState.textPrompt,
                onValueChange = viewModel::setTextPrompt,
                placeholder = "Ex: Travelling avant spectaculaire à travers une forêt de bambous dans la brume matinale, 60fps...",
                minLines = 3,
                maxLines = 5,
                testTag = "video_prompt_input"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Camera Movement Chips
        Text(
            text = "Mouvement de caméra",
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
            cameraMovements.forEach { movement ->
                CinChip(
                    text = movement,
                    selected = uiState.cameraMovement == movement,
                    onClick = { viewModel.setCameraMovement(movement) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Movement Intensity Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Intensité cinématique",
                color = CinColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${uiState.movementIntensity.toInt()}/10",
                color = CinColors.AccentPink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = uiState.movementIntensity,
            onValueChange = viewModel::setMovementIntensity,
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.testTag("video_intensity_slider"),
            colors = SliderDefaults.colors(
                thumbColor = CinColors.AccentPink,
                activeTrackColor = CinColors.AccentViolet,
                inactiveTrackColor = CinColors.BorderDefault
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Duration selector
        Text(
            text = "Durée du plan",
            color = CinColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            durations.forEach { (label, durationVal) ->
                CinChip(
                    text = label,
                    selected = uiState.durationSeconds == durationVal,
                    onClick = { viewModel.setDuration(durationVal) }
                )
            }
        }
        Text(
            text = "Veo accepte 4, 6 ou 8 secondes ; la résolution 1080p nécessite 8 secondes.",
            color = CinColors.TextTertiary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Resolution selector
        Text(
            text = "Résolution",
            color = CinColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            resolutions.forEach { res ->
                CinChip(
                    text = res,
                    selected = uiState.resolution == res,
                    onClick = { viewModel.setResolution(res) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Progress bar if generating
        if (uiState.isGenerating) {
            CinCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.progressMessage,
                            color = CinColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${uiState.progressPercent}%",
                            color = CinColors.AccentPink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    CinProgressBar(progress = uiState.progressPercent / 100f)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CinColors.Danger.copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = CinColors.Danger,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Generate Button
        CinButton(
            text = if (uiState.isGenerating) "Génération de la vidéo en cours..." else "Générer la vidéo",
            onClick = { viewModel.generateVideo() },
            modifier = Modifier.fillMaxWidth(),
            style = CinButtonStyle.PRIMARY,
            enabled = !uiState.isGenerating,
            loading = uiState.isGenerating,
            icon = Icons.Default.MovieCreation,
            testTag = "generate_video_button"
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Recent Videos Grid
        if (uiState.recentVideos.isNotEmpty()) {
            Text(
                text = "Vidéos récentes",
                color = CinColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.recentVideos.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { item ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CinColors.BgElevated)
                                    .border(1.dp, CinColors.BorderDefault, RoundedCornerShape(12.dp))
                                    .clickable { onMediaSelected(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(CinColors.GradientFilm),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Lecture",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.title,
                                        color = CinColors.TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
