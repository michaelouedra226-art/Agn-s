package com.example.ui.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.CinButton
import com.example.core.design.CinButtonStyle
import com.example.core.design.CinCard
import com.example.core.design.CinChip
import com.example.core.design.CinColors
import com.example.core.design.CinInput

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section: API Keys
        CinCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = CinColors.AccentViolet,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connexion Google Gemini",
                        color = CinColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gemini API Key
                Text(
                    text = "✨ Clé Google AI Studio / Gemini",
                    color = CinColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                CinInput(
                    value = uiState.geminiKeyInput,
                    onValueChange = viewModel::onGeminiKeyChange,
                    placeholder = "Collez votre clé Google AI Studio",
                    testTag = "gemini_key_input",
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Cette clé sert aux scénarios, aux images Gemini et aux vidéos Veo 3.1. La génération vidéo dépend de l’accès et du quota de votre compte Google.",
                    color = CinColors.TextTertiary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.geminiTestResult != null) {
                        Text(
                            text = uiState.geminiTestResult!!,
                            color = if (uiState.geminiTestResult!!.startsWith("✓")) CinColors.Success else CinColors.Danger,
                            fontSize = 11.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    CinButton(
                        text = if (uiState.isTestingGemini) "Test..." else "Tester",
                        onClick = viewModel::testGeminiKey,
                        style = CinButtonStyle.SECONDARY,
                        modifier = Modifier.height(36.dp),
                        loading = uiState.isTestingGemini
                    )
                }

            }
        }

        // Section: Appearance
        CinCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = CinColors.AccentPink,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apparence & Rendu",
                        color = CinColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Theme Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thème de l'interface", color = CinColors.TextPrimary, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CinChip(
                            text = "Sombre",
                            selected = uiState.userSettings.darkTheme,
                            onClick = { viewModel.toggleTheme(true) }
                        )
                        CinChip(
                            text = "Clair",
                            selected = !uiState.userSettings.darkTheme,
                            onClick = { viewModel.toggleTheme(false) }
                        )
                    }
                }

                // Animated Background Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Fond animé cinéma (Mesh)", color = CinColors.TextPrimary, fontSize = 13.sp)
                        Text("Atmosphère lumineuse dynamique", color = CinColors.TextTertiary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = uiState.userSettings.animatedBackgroundEnabled,
                        onCheckedChange = viewModel::toggleAnimatedBackground,
                        colors = SwitchDefaults.colors(checkedThumbColor = CinColors.AccentPink, checkedTrackColor = CinColors.AccentViolet)
                    )
                }

                // Starfield Particles Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Particules flottantes", color = CinColors.TextPrimary, fontSize = 13.sp)
                        Text("Poussière de projecteur cinéma", color = CinColors.TextTertiary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = uiState.userSettings.particlesEnabled,
                        onCheckedChange = viewModel::toggleParticles,
                        colors = SwitchDefaults.colors(checkedThumbColor = CinColors.AccentPink, checkedTrackColor = CinColors.AccentViolet)
                    )
                }
            }
        }

        // Section: System & About
        CinCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CinColors.AccentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "À propos de CinéIA Studio",
                        color = CinColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Version : 1.0.0 (Production Release)",
                    color = CinColors.TextSecond,
                    fontSize = 12.sp
                )
                Text(
                    text = "Moteurs : Gemini Flash Image + Veo 3.1",
                    color = CinColors.TextSecond,
                    fontSize = 12.sp
                )
                Text(
                    text = "Architecture : MVVM + Room + Rate Limiter Token Bucket",
                    color = CinColors.TextSecond,
                    fontSize = 12.sp
                )
            }
        }
    }
}
