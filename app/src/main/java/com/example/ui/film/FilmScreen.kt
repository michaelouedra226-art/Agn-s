package com.example.ui.film

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.CinButton
import com.example.core.design.CinButtonStyle
import com.example.core.design.CinColors
import com.example.domain.model.Film

@Composable
fun FilmScreen(
    viewModel: FilmViewModel,
    onFilmCompleted: (Film) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Crash Recovery Banner (§13.2)
        if (uiState.showUnfinishedResumeBanner) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CinColors.Warning.copy(alpha = 0.2f))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = CinColors.Warning
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Un film inachevé a été détecté. Voulez-vous reprendre ?",
                            color = CinColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row {
                        TextButton(onClick = viewModel::dismissResumeBanner) {
                            Text("Ignorer", color = CinColors.TextTertiary, fontSize = 11.sp)
                        }
                        CinButton(
                            text = "Reprendre",
                            onClick = viewModel::resumeUnfinishedFilm,
                            style = CinButtonStyle.PRIMARY,
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isGenerating || uiState.currentFilm?.status == "processing") {
            FilmLiveProgressView(
                uiState = uiState,
                onSelectScene = viewModel::selectActiveScene,
                onTogglePause = viewModel::togglePause,
                onCancel = viewModel::cancelGeneration
            )
        } else {
            FilmConfigView(
                uiState = uiState,
                onPromptIdeaChange = viewModel::onPromptIdeaChange,
                onLanguageSelected = viewModel::onLanguageSelected,
                onStyleSelected = viewModel::onStyleSelected,
                onDurationChange = viewModel::onDurationChange,
                onSceneCountChange = viewModel::onSceneCountChange,
                onSubmit = { viewModel.startFilmCreation(onFilmCompleted) }
            )
        }
    }
}
