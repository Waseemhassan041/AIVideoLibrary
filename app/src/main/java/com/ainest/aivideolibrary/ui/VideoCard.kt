package com.ainest.aivideolibrary.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ainest.aivideolibrary.data.VideoItem
import com.ainest.aivideolibrary.util.UriUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoCard(
    video: VideoItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePinned: () -> Unit,
    onToggleUpload: (String) -> Unit,
    onToggleSelected: () -> Unit,
    onLocateVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            Box {
                val thumbModifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 12f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .combinedClickable(
                        onClick = {
                            if (isSelectionMode) onToggleSelected() else playVideo(context, video.videoUri)
                        },
                        onLongClick = onToggleSelected
                    )

                if (video.thumbnailPath != null) {
                    AsyncImage(
                        model = video.thumbnailPath,
                        contentDescription = video.title,
                        contentScale = ContentScale.Crop,
                        modifier = thumbModifier
                    )
                } else {
                    Box(modifier = thumbModifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                if (!isSelectionMode) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.align(Alignment.Center).size(44.dp)
                    )
                }

                // Favorite heart - top-left
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopStart).padding(2.dp).size(34.dp)
                ) {
                    Icon(
                        imageVector = if (video.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (video.isFavorite) Color(0xFFE74C3C) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (video.isPinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.TopCenter).padding(6.dp).size(16.dp)
                    )
                }

                if (video.isMissing) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Video not found on this device",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).size(18.dp)
                    )
                }

                video.aiModel?.let { model ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(model, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }

                // Selection checkbox / 3-dot menu, top-right
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    if (isSelectionMode) {
                        IconButton(onClick = onToggleSelected) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = "Select",
                                tint = Color.White
                            )
                        }
                    } else {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More actions", tint = Color.White)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Play") }, onClick = { menuExpanded = false; playVideo(context, video.videoUri) })
                            DropdownMenuItem(text = { Text("Copy Title") }, onClick = { menuExpanded = false; copyToClipboard(context, "Title", video.title) })
                            DropdownMenuItem(text = { Text("Copy Prompt") }, onClick = { menuExpanded = false; copyToClipboard(context, "Prompt", video.prompt) })
                            DropdownMenuItem(text = { Text("Copy Hashtags") }, onClick = { menuExpanded = false; copyToClipboard(context, "Hashtags", video.hashtags) })
                            DropdownMenuItem(text = { Text("Copy Keywords") }, onClick = { menuExpanded = false; copyToClipboard(context, "Keywords", video.keywords) })
                            DropdownMenuItem(text = { Text("Share Prompt") }, onClick = { menuExpanded = false; sharePrompt(context, video.prompt) })
                            if (video.isMissing) {
                                DropdownMenuItem(text = { Text("Locate Video") }, onClick = { menuExpanded = false; onLocateVideo() })
                            }
                            DropdownMenuItem(text = { Text(if (video.isPinned) "Unpin" else "Pin") }, onClick = { menuExpanded = false; onTogglePinned() })
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menuExpanded = false; onEdit() })
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuExpanded = false; onDelete() })
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .combinedClickable(
                        onClick = { if (isSelectionMode) onToggleSelected() else onEdit() },
                        onLongClick = onToggleSelected
                    )
            ) {
                Text(
                    text = video.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UploadStatusRow(video = video, clickable = !isSelectionMode, onToggle = onToggleUpload)
                    Text(
                        text = formatDate(video.dateAdded),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadStatusRow(video: VideoItem, clickable: Boolean, onToggle: (String) -> Unit) {
    Row {
        UploadDot("FB", video.uploadFacebook, clickable) { onToggle("FB") }
        UploadDot("TT", video.uploadTiktok, clickable) { onToggle("TT") }
        UploadDot("YT", video.uploadYoutube, clickable) { onToggle("YT") }
        UploadDot("IG", video.uploadInstagram, clickable) { onToggle("IG") }
    }
}

@Composable
private fun UploadDot(label: String, uploaded: Boolean, clickable: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 4.dp)
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (uploaded) Color(0xFF2ECC71) else MaterialTheme.colorScheme.surfaceVariant)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (uploaded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

private fun playVideo(context: Context, uriString: String) {
    UriUtil.openInDefaultPlayer(context, Uri.parse(uriString))
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun sharePrompt(context: Context, prompt: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, prompt)
    }
    context.startActivity(Intent.createChooser(intent, "Share Prompt"))
}
