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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.CinButton
import com.example.core.design.CinButtonStyle
import com.example.core.design.CinCard
import com.example.core.design.CinChip
import com.example.core.design.CinColors
import com.example.core.design.CinInput

@Composable
fun FilmConfigView(
    uiState: FilmUiState,
    onPromptIdeaChange: (String) -> Unit,
    onLanguageSelected: (String) -> Unit,
    onStyleSelected: (String) -> Unit,
    onDurationChange: (Double) -> Unit,
    onSceneCountChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdvancedOptions by remember { mutableStateOf(false) }

    val languages = listOf("Français", "English", "Español", "Deutsch", "Italiano", "日本語")
    val styles = listOf("Cinématique", "Anime", "Film Noir", "Cyberpunk", "3D Rendu", "Documentaire")

    Column(modifier = modifier.fillMaxWidth()) {
        // Hero
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
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Pipeline Film Complet",
                            color = CinColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Scénario • Images clés • Animation • Assemblage",
                            color = CinColors.TextSecond,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Film Idea Input
        Text(
            text = "Votre idée de film",
            color = CinColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        CinInput(
            value = uiState.promptIdea,
            onValueChange = onPromptIdeaChange,
            placeholder = "Ex: Une mission spatiale vers une station abandonnée en orbite de Jupiter, découverte d'une forme de vie mystérieuse...",
            minLines = 3,
            maxLines = 5,
            testTag = "film_idea_input"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Language Selector
        Text(
            text = "Langue du film & des dialogues",
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
            languages.forEach { lang ->
                CinChip(
                    text = lang,
                    selected = uiState.selectedLanguage == lang,
                    onClick = { onLanguageSelected(lang) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Style Selector
        Text(
            text = "Style visuel & cinématographique",
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
                    onClick = { onStyleSelected(style) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Duration Slider & Scene Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Durée du court-métrage",
                color = CinColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${uiState.targetDurationSeconds.toInt()}s (${uiState.sceneCount} scènes × 6.4s)",
                color = CinColors.AccentPink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = uiState.targetDurationSeconds.toFloat(),
            onValueChange = { onDurationChange(it.toDouble()) },
            valueRange = 12f..64f,
            steps = 3,
            modifier = Modifier.testTag("film_duration_slider"),
            colors = SliderDefaults.colors(
                thumbColor = CinColors.AccentPink,
                activeTrackColor = CinColors.AccentViolet,
                inactiveTrackColor = CinColors.BorderDefault
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Advanced Options Accordion
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { showAdvancedOptions = !showAdvancedOptions }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Options avancées de réalisation",
                color = CinColors.TextSecond,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = CinColors.TextSecond
            )
        }

        AnimatedVisibility(visible = showAdvancedOptions) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CinColors.BgElevated)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sous-titres automatiques", color = CinColors.TextPrimary, fontSize = 13.sp)
                    Switch(
                        checked = uiState.includeSubtitles,
                        onCheckedChange = {},
                        colors = SwitchDefaults.colors(checkedThumbColor = CinColors.AccentPink, checkedTrackColor = CinColors.AccentViolet)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Voix dialogue", color = CinColors.TextPrimary, fontSize = 13.sp)
                    Text("Français Neutre", color = CinColors.AccentCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // API Status Card
        CinCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = CinColors.AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gemini API (Scénario)", color = CinColors.TextPrimary, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CinColors.Success))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connecté", color = CinColors.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = CinColors.AccentPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Agnes Engine (Diffusion vidéo)", color = CinColors.TextPrimary, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(CinColors.Success))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prêt", color = CinColors.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CinColors.Danger.copy(alpha = 0.15f))
                    .padding(12.dp)
            ) {
                Text(
                    text = uiState.errorMessage,
                    color = CinColors.Danger,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Submit Button
        CinButton(
            text = "🎬 CRÉER MON FILM",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            style = CinButtonStyle.PRIMARY,
            testTag = "create_film_button"
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "⏱ Estimation : ~10-15 min • ${uiState.sceneCount} images clés + ${uiState.sceneCount} clips vidéos",
            color = CinColors.TextTertiary,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
