package com.example.ui.log

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.design.CinColors
import com.example.core.log.LogEntry
import com.example.core.log.LogLevel
import com.example.core.log.LogManager

@Composable
fun LogFloatingPanel(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logs by LogManager.logs.collectAsState()
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, selectedFilter, searchQuery) {
        logs.filter { entry ->
            val matchLevel = selectedFilter == null || entry.level == selectedFilter
            val matchQuery = searchQuery.isBlank() ||
                entry.message.contains(searchQuery, ignoreCase = true) ||
                entry.source.contains(searchQuery, ignoreCase = true) ||
                (entry.details?.contains(searchQuery, ignoreCase = true) == true)
            matchLevel && matchQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.92f)
                .background(CinColors.BgSurface)
                .clickable(enabled = false) {}
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📋 JOURNAL TEMPS RÉEL",
                        color = CinColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${logs.size})",
                        color = CinColors.TextTertiary,
                        fontSize = 13.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("CinéIA Logs", LogManager.exportAsText()))
                        },
                        modifier = Modifier.size(36.dp).testTag("log_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier logs",
                            tint = CinColors.TextSecond,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { LogManager.clear() },
                        modifier = Modifier.size(36.dp).testTag("log_clear_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Effacer logs",
                            tint = CinColors.Danger,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("log_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = CinColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("log_search_input"),
                placeholder = { Text("Rechercher dans les événements...", color = CinColors.TextTertiary, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CinColors.TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CinColors.BgElevated,
                    unfocusedContainerColor = CinColors.BgElevated,
                    focusedBorderColor = CinColors.AccentViolet,
                    unfocusedBorderColor = CinColors.BorderDefault
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LogFilterChip("Tout", selectedFilter == null) { selectedFilter = null }
                LogFilterChip("Info", selectedFilter == LogLevel.INFO) { selectedFilter = LogLevel.INFO }
                LogFilterChip("Succès", selectedFilter == LogLevel.SUCCESS) { selectedFilter = LogLevel.SUCCESS }
                LogFilterChip("Warn", selectedFilter == LogLevel.WARN) { selectedFilter = LogLevel.WARN }
                LogFilterChip("Erreur", selectedFilter == LogLevel.ERROR) { selectedFilter = LogLevel.ERROR }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = CinColors.BorderDefault)
            Spacer(modifier = Modifier.height(8.dp))

            // Logs Stream List
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun événement enregistré pour ce filtre",
                        color = CinColors.TextTertiary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { entry ->
                        LogItemRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CinColors.AccentViolet else CinColors.BgElevated)
            .border(1.dp, if (isSelected) CinColors.AccentViolet else CinColors.BorderDefault, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else CinColors.TextSecond,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun LogItemRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.SUCCESS -> CinColors.Success
        LogLevel.WARN -> CinColors.Warning
        LogLevel.ERROR -> CinColors.Danger
        LogLevel.DEBUG -> CinColors.AccentPink
        LogLevel.INFO -> CinColors.AccentCyan
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CinColors.BgElevated)
            .border(1.dp, CinColors.BorderSubtle, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(levelColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = entry.level.name,
                        color = levelColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = entry.source,
                    color = CinColors.TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = entry.formattedTime(),
                color = CinColors.TextTertiary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = entry.message,
            color = CinColors.TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        if (!entry.details.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.details,
                color = CinColors.TextTertiary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
