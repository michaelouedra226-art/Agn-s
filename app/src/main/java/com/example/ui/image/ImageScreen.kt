package com.example.ui.image

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
fun ImageScreen(
    viewModel: ImageViewModel,
    onMediaSelected: (MediaItem) -> Unit,
    onAnimateImage: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var showAdvanced by remember { mutableStateOf(false) }

    val styles = listOf("Cinématique", "Anime", "Noir & Blanc", "3D Rendu", "Cyberpunk", "Aquarelle", "Rétro 70s")
    val ratios = listOf("16:9", "1:1", "9:16", "4:3", "3:2")
    val qualities = listOf("Standard", "HD", "Ultra")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Hero Section
        CinCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CinColors.GradientCyanViolet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Génération d'Image Clé",
                            color = CinColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Créez une composition visuelle pour vos plans",
                            color = CinColors.TextSecond,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Prompt Input
        Text(
            text = "Description visuelle",
            color = CinColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        CinInput(
            value = uiState.prompt,
            onValueChange = viewModel::onPromptChange,
            placeholder = "Ex: Un samouraï solitaire marchant sous une pluie battante à Tokyo, néons cyberpunk violets, 8k...",
            minLines = 3,
            maxLines = 5,
            testTag = "image_prompt_input"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Prompt Actions & Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${uiState.prompt.length}/1000",
                color = if (uiState.prompt.length > 800) CinColors.Warning else CinColors.TextTertiary,
                fontSize = 12.sp
            )

            CinButton(
                text = if (uiState.isImprovingPrompt) "Optimisation..." else "✨ Améliorer (Gemini)",
                onClick = viewModel::improvePrompt,
                style = CinButtonStyle.SECONDARY,
                enabled = uiState.prompt.isNotBlank() && !uiState.isImprovingPrompt,
                loading = uiState.isImprovingPrompt,
                testTag = "improve_prompt_button"
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Style Selector
        Text(
            text = "Style artistique",
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
            styles.forEach { style ->
                CinChip(
                    text = style,
                    selected = uiState.selectedStyle == style,
                    onClick = { viewModel.onStyleSelected(style) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Aspect Ratio
        Text(
            text = "Ratio de cadrage",
            color = CinColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ratios.forEach { ratio ->
                CinChip(
                    text = ratio,
                    selected = uiState.selectedRatio == ratio,
                    onClick = { viewModel.onRatioSelected(ratio) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quality
        Text(
            text = "Niveau de rendu",
            color = CinColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            qualities.forEach { q ->
                CinChip(
                    text = q,
                    selected = uiState.selectedQuality == q,
                    onClick = { viewModel.onQualitySelected(q) }
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
            text = if (uiState.isGenerating) "Génération en cours..." else "Générer l'image HD",
            onClick = { viewModel.generateImage() },
            modifier = Modifier.fillMaxWidth(),
            style = CinButtonStyle.PRIMARY,
            enabled = !uiState.isGenerating,
            loading = uiState.isGenerating,
            icon = Icons.Default.AutoAwesome,
            testTag = "generate_image_button"
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Recent Creations
        if (uiState.recentImages.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Créations récentes",
                    color = CinColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${uiState.recentImages.size} images",
                    color = CinColors.TextTertiary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.recentImages, key = { it.id }) { item ->
                    RecentImageCard(
                        media = item,
                        onClick = { onMediaSelected(item) },
                        onAnimate = { onAnimateImage(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentImageCard(
    media: MediaItem,
    onClick: () -> Unit,
    onAnimate: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CinColors.BgSurface)
            .border(1.dp, CinColors.BorderDefault, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            val file = File(media.path)
            AsyncImage(
                model = file,
                contentDescription = media.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                contentScale = ContentScale.Crop
            )

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = media.title,
                    color = CinColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CinColors.BgElevated)
                            .clickable(onClick = onClick)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Voir",
                            tint = CinColors.TextSecond,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CinColors.GradientFilm)
                            .clickable(onClick = onAnimate)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Animer",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Animer",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
