package com.example.ui.gallery

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.core.design.CinChip
import com.example.core.design.CinColors
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import java.io.File

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onMediaSelected: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val filteredList = remember(uiState.mediaItems, uiState.selectedFilter, uiState.searchQuery) {
        uiState.mediaItems.filter { item ->
            val matchFilter = when (uiState.selectedFilter) {
                GalleryFilter.ALL -> true
                GalleryFilter.IMAGES -> item.type == MediaType.IMAGE
                GalleryFilter.VIDEOS -> item.type == MediaType.VIDEO
                GalleryFilter.FILMS -> item.type == MediaType.FILM
            }
            val matchQuery = uiState.searchQuery.isBlank() ||
                item.title.contains(uiState.searchQuery, ignoreCase = true) ||
                (item.prompt?.contains(uiState.searchQuery, ignoreCase = true) == true)
            matchFilter && matchQuery
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Mode Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("gallery_search_input"),
                placeholder = { Text("Rechercher un média...", color = CinColors.TextTertiary, fontSize = 13.sp) },
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

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = viewModel::toggleViewMode,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CinColors.BgElevated)
            ) {
                Icon(
                    imageVector = if (uiState.isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Changer affichage",
                    tint = CinColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CinChip(text = "Tous", selected = uiState.selectedFilter == GalleryFilter.ALL, onClick = { viewModel.setFilter(GalleryFilter.ALL) })
            CinChip(text = "Images", selected = uiState.selectedFilter == GalleryFilter.IMAGES, onClick = { viewModel.setFilter(GalleryFilter.IMAGES) })
            CinChip(text = "Vidéos", selected = uiState.selectedFilter == GalleryFilter.VIDEOS, onClick = { viewModel.setFilter(GalleryFilter.VIDEOS) })
            CinChip(text = "Films", selected = uiState.selectedFilter == GalleryFilter.FILMS, onClick = { viewModel.setFilter(GalleryFilter.FILMS) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media Collection
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = CinColors.TextTertiary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucun média trouvé dans votre studio",
                        color = CinColors.TextSecond,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Créez une image, une vidéo ou un film pour commencer",
                        color = CinColors.TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (uiState.isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    GalleryGridItem(
                        item = item,
                        onClick = { onMediaSelected(item) },
                        onDelete = { viewModel.deleteMedia(item.id) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    GalleryListItem(
                        item = item,
                        onClick = { onMediaSelected(item) },
                        onDelete = { viewModel.deleteMedia(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryGridItem(
    item: MediaItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CinColors.BgElevated)
            .border(1.dp, CinColors.BorderDefault, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            val file = File(item.path)
            if (file.exists() && (item.type == MediaType.IMAGE || item.path.endsWith(".jpg"))) {
                AsyncImage(
                    model = file,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(CinColors.BgVoid),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.type == MediaType.FILM) Icons.Default.Movie else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = CinColors.AccentPink,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.title,
                    color = CinColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.type.name,
                        color = CinColors.AccentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = CinColors.TextTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryListItem(
    item: MediaItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CinColors.BgElevated)
            .border(1.dp, CinColors.BorderDefault, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val file = File(item.path)
        if (file.exists() && (item.type == MediaType.IMAGE || item.path.endsWith(".jpg"))) {
            AsyncImage(
                model = file,
                contentDescription = item.title,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CinColors.BgVoid),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.type == MediaType.FILM) Icons.Default.Movie else Icons.Default.Videocam,
                    contentDescription = null,
                    tint = CinColors.AccentPink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = CinColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.type.name} • ${if (item.prompt != null) item.prompt.take(35) + "..." else "Média cinématique"}",
                color = CinColors.TextSecond,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Supprimer",
                tint = CinColors.TextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
