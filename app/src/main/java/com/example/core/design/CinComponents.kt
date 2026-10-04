package com.example.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class CinButtonStyle {
    PRIMARY,
    SECONDARY,
    GHOST,
    DANGER
}

@Composable
fun CinButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: CinButtonStyle = CinButtonStyle.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    testTag: String = "cin_button"
) {
    val bgModifier = when (style) {
        CinButtonStyle.PRIMARY -> if (enabled) Modifier.background(CinColors.GradientFilm, RoundedCornerShape(12.dp)) else Modifier.background(Color(0xFF2A2A38), RoundedCornerShape(12.dp))
        CinButtonStyle.SECONDARY -> Modifier.background(CinColors.BgElevated, RoundedCornerShape(12.dp)).border(1.dp, CinColors.BorderDefault, RoundedCornerShape(12.dp))
        CinButtonStyle.GHOST -> Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
        CinButtonStyle.DANGER -> Modifier.background(CinColors.Danger.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(bgModifier)
            .clickable(enabled = enabled && !loading, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) Color.White else CinColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    color = if (enabled) Color.White else CinColors.TextTertiary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun CinCard(
    modifier: Modifier = Modifier,
    borderGradient: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val borderModifier = if (borderGradient) {
        Modifier.border(1.dp, CinColors.GradientFilm, RoundedCornerShape(16.dp))
    } else {
        Modifier.border(1.dp, CinColors.BorderDefault, RoundedCornerShape(16.dp))
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(borderModifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CinColors.BgSurface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        content()
    }
}

@Composable
fun CinInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    maxLines: Int = 6,
    maxCharacters: Int = 1000,
    testTag: String = "cin_input",
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= maxCharacters) onValueChange(it) },
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        placeholder = { Text(placeholder, color = CinColors.TextTertiary, fontSize = 14.sp) },
        textStyle = TextStyle(color = CinColors.TextPrimary, fontSize = 14.sp),
        minLines = minLines,
        maxLines = maxLines,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = CinColors.BgElevated,
            unfocusedContainerColor = CinColors.BgSurface,
            focusedBorderColor = CinColors.AccentViolet,
            unfocusedBorderColor = CinColors.BorderDefault,
            cursorColor = CinColors.AccentPink
        )
    )
}

@Composable
fun CinChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String = "cin_chip"
) {
    val bgModifier = if (selected) {
        Modifier.background(CinColors.GradientFilm, RoundedCornerShape(20.dp))
    } else {
        Modifier.background(CinColors.BgElevated, RoundedCornerShape(20.dp)).border(1.dp, CinColors.BorderDefault, RoundedCornerShape(20.dp))
    }

    Row(
        modifier = modifier
            .testTag(testTag)
            .height(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else CinColors.TextSecond,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = if (selected) Color.White else CinColors.TextSecond,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
fun CinStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "done", "success", "completed" -> Triple(CinColors.Success.copy(alpha = 0.2f), CinColors.Success, "Terminé")
        "processing", "generating", "running" -> Triple(CinColors.AccentViolet.copy(alpha = 0.25f), CinColors.AccentPink, "En cours")
        "failed", "error" -> Triple(CinColors.Danger.copy(alpha = 0.2f), CinColors.Danger, "Erreur")
        "stalled", "paused" -> Triple(CinColors.Warning.copy(alpha = 0.2f), CinColors.Warning, "En pause")
        else -> Triple(Color(0xFF2A2A38), CinColors.TextSecond, "En attente")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CinProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    showShimmer: Boolean = true
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val shimmerTranslate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(CinColors.BorderDefault)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clampedProgress)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CinColors.GradientFilm)
        )
    }
}
