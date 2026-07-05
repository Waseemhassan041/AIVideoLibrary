package com.ainest.aivideolibrary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ainest.aivideolibrary.data.RECYCLE_BIN_RETENTION_DAYS
import com.ainest.aivideolibrary.data.VideoItem
import com.ainest.aivideolibrary.viewmodel.VideoViewModel
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    viewModel: VideoViewModel,
    onBack: () -> Unit
) {
    val videos by viewModel.recycleBinVideos.collectAsState()
    var pendingPermanentDelete by remember { mutableStateOf<VideoItem?>(null) }
    var confirmEmptyBin by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (videos.isNotEmpty()) {
                        IconButton(onClick = { confirmEmptyBin = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "Empty Recycle Bin")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Recycle Bin is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    RecycleBinRow(
                        video = video,
                        onRestore = { viewModel.restoreFromBin(video) },
                        onDeleteForever = { pendingPermanentDelete = video }
                    )
                }
            }
        }
    }

    pendingPermanentDelete?.let { video ->
        ConfirmPermanentDeleteDialog(
            title = video.title.ifBlank { "this video" },
            onConfirm = {
                viewModel.permanentlyDelete(video)
                pendingPermanentDelete = null
            },
            onDismiss = { pendingPermanentDelete = null }
        )
    }

    if (confirmEmptyBin) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmEmptyBin = false },
            title = { Text("Empty Recycle Bin?") },
            text = { Text("All ${videos.size} entries will be permanently deleted. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyRecycleBin()
                    confirmEmptyBin = false
                }) { Text("Empty Bin") }
            },
            dismissButton = { TextButton(onClick = { confirmEmptyBin = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RecycleBinRow(video: VideoItem, onRestore: () -> Unit, onDeleteForever: () -> Unit) {
    val daysLeft = remember(video.deletedAt) {
        val deletedAt = video.deletedAt ?: System.currentTimeMillis()
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - deletedAt)
        (RECYCLE_BIN_RETENTION_DAYS - elapsedDays).coerceAtLeast(0)
    }

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (video.thumbnailPath != null) {
                        AsyncImage(model = video.thumbnailPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.PlayCircle, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp).widthIn(max = 170.dp)) {
                    Text(video.title.ifBlank { "Untitled" }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$daysLeft day${if (daysLeft == 1L) "" else "s"} left",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRestore) { Icon(Icons.Filled.Restore, contentDescription = "Restore") }
                IconButton(onClick = onDeleteForever) { Icon(Icons.Filled.DeleteForever, contentDescription = "Delete Forever") }
            }
        }
    }
}
