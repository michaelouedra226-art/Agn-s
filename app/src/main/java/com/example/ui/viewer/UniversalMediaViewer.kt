package com.example.ui.viewer

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.core.design.CinColors
import com.example.domain.model.MediaItem
import com.example.domain.model.MediaType
import java.io.File

@Composable
fun UniversalMediaViewer(
    media: MediaItem,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val file = File(media.path)
    val videoViewRef = remember { mutableStateOf<VideoView?>(null) }
    DisposableEffect(Unit) {
        onDispose { videoViewRef.value?.stopPlayback() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = false) {},
            contentAlignment = Alignment.Center
        ) {
            if (media.type == MediaType.IMAGE) {
                AsyncImage(
                    model = file,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (file.isFile) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    factory = { viewContext ->
                        VideoView(viewContext).apply {
                            videoViewRef.value = this
                            tag = media.path
                            val controller = MediaController(viewContext)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoURI(Uri.fromFile(file))
                            setOnPreparedListener { player ->
                                player.isLooping = false
                                start()
                                controller.show(3_000)
                            }
                        }
                    },
                    update = { videoView ->
                        if (videoView.tag != media.path) {
                            videoView.tag = media.path
                            videoView.setVideoURI(Uri.fromFile(file))
                        }
                    }
                )
            } else {
                Text(
                    text = "Fichier média introuvable sur cet appareil.",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.75f))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = media.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${media.type.name} • ${if (media.sizeBytes > 0) "${media.sizeBytes / 1024} Ko" else "Fichier local"}",
                    color = CinColors.TextSecond,
                    fontSize = 12.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, "Créé avec CinéIA Studio : ${media.title}\n${media.prompt.orEmpty()}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partager le média"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Partager", tint = Color.White)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                }
            }
        }

        if (!media.prompt.isNullOrBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .padding(16.dp)
            ) {
                Text(
                    text = media.prompt.orEmpty(),
                    color = CinColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Les commandes de lecture sont disponibles sur la vidéo.",
                    color = CinColors.TextTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
