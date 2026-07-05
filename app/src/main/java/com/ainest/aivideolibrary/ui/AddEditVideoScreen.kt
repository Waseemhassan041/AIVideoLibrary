package com.ainest.aivideolibrary.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ainest.aivideolibrary.data.AiModels
import com.ainest.aivideolibrary.data.Categories
import com.ainest.aivideolibrary.data.VideoItem
import com.ainest.aivideolibrary.util.UriUtil
import com.ainest.aivideolibrary.viewmodel.VideoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVideoScreen(
    viewModel: VideoViewModel,
    videoId: Long,
    multiAddQueue: List<Uri> = emptyList(),
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isMultiAdd = videoId == -2L
    val isNew = videoId <= 0L

    var existing by remember { mutableStateOf<VideoItem?>(null) }
    var remainingQueue by remember { mutableStateOf(multiAddQueue) }

    var videoUri by remember { mutableStateOf<String?>(null) }
    var originalFileName by remember { mutableStateOf<String?>(null) }
    var thumbnailPath by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Categories.defaults.first()) }
    var aiModel by remember { mutableStateOf(AiModels.defaults.first()) } // "Select"
    var isFavorite by remember { mutableStateOf(false) }
    var isPinned by remember { mutableStateOf(false) }
    var uploadFacebook by remember { mutableStateOf(false) }
    var uploadTiktok by remember { mutableStateOf(false) }
    var uploadYoutube by remember { mutableStateOf(false) }
    var uploadInstagram by remember { mutableStateOf(false) }
    var generatingThumbnail by remember { mutableStateOf(false) }
    var isMissing by remember { mutableStateOf(false) }
    var dateAdded by remember { mutableStateOf(System.currentTimeMillis()) }
    var autoFilling by remember { mutableStateOf(false) }
    var duplicateWarning by remember { mutableStateOf(false) }
    val promptError = prompt.isBlank()

    fun resetFormForNewEntry() {
        existing = null
        videoUri = null
        originalFileName = null
        thumbnailPath = null
        title = ""; prompt = ""; hashtags = ""; keywords = ""; notes = ""
        category = Categories.defaults.first()
        aiModel = AiModels.defaults.first()
        isFavorite = false; isPinned = false
        uploadFacebook = false; uploadTiktok = false; uploadYoutube = false; uploadInstagram = false
        isMissing = false
        dateAdded = System.currentTimeMillis()
    }

    suspend fun pickUpFromUri(uri: Uri) {
        UriUtil.takePersistablePermission(context, uri)
        videoUri = uri.toString()
        originalFileName = UriUtil.getDisplayName(context, uri)
        generatingThumbnail = true
        thumbnailPath = viewModel.generateThumbnail(uri)
        generatingThumbnail = false
    }

    LaunchedEffect(videoId) {
        if (isMultiAdd) {
            if (remainingQueue.isNotEmpty()) {
                val next = remainingQueue.first()
                remainingQueue = remainingQueue.drop(1)
                pickUpFromUri(next)
            }
        } else if (!isNew) {
            val v = viewModel.getVideo(videoId)
            if (v != null) {
                existing = v
                videoUri = v.videoUri
                originalFileName = v.originalFileName
                thumbnailPath = v.thumbnailPath
                title = v.title
                prompt = v.prompt
                hashtags = v.hashtags
                keywords = v.keywords
                notes = v.notes
                category = v.category
                aiModel = v.aiModel ?: AiModels.defaults.first()
                isFavorite = v.isFavorite
                isPinned = v.isPinned
                uploadFacebook = v.uploadFacebook
                uploadTiktok = v.uploadTiktok
                uploadYoutube = v.uploadYoutube
                uploadInstagram = v.uploadInstagram
                isMissing = v.isMissing
                dateAdded = v.dateAdded
            }
        }
    }

    val pickVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val dup = viewModel.isDuplicate(uri.toString())
                if (dup && existing?.videoUri != uri.toString()) {
                    duplicateWarning = true
                } else {
                    pickUpFromUri(uri)
                }
            }
        }
    }

    fun buildItem(): VideoItem = VideoItem(
        id = existing?.id ?: 0L,
        videoUri = videoUri!!,
        originalFileName = originalFileName,
        thumbnailPath = thumbnailPath,
        title = title,
        prompt = prompt,
        hashtags = hashtags,
        keywords = keywords,
        notes = notes,
        category = category.ifBlank { "Other" },
        aiModel = if (aiModel == "Select") null else aiModel,
        isFavorite = isFavorite,
        isPinned = isPinned,
        dateAdded = dateAdded,
        lastEdited = System.currentTimeMillis(),
        uploadFacebook = uploadFacebook,
        uploadTiktok = uploadTiktok,
        uploadYoutube = uploadYoutube,
        uploadInstagram = uploadInstagram,
        isMissing = false
    )

    fun handleSave() {
        if (videoUri == null) {
            Toast.makeText(context, "Please select a video first", Toast.LENGTH_SHORT).show()
            return
        }
        if (promptError) {
            Toast.makeText(context, "Prompt is required", Toast.LENGTH_SHORT).show()
            return
        }
        val item = buildItem()
        viewModel.addOrUpdateVideo(item) {
            if (isMultiAdd && remainingQueue.isNotEmpty()) {
                resetFormForNewEntry()
                scope.launch {
                    val next = remainingQueue.first()
                    remainingQueue = remainingQueue.drop(1)
                    pickUpFromUri(next)
                }
            } else {
                onDone()
            }
        }
    }

    val saveLabel = if (isMultiAdd && remainingQueue.isNotEmpty()) "Save & Next" else "Save"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew || isMultiAdd) "Add Video" else "Edit Video") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = { handleSave() }) {
                        Icon(Icons.Filled.Save, contentDescription = saveLabel)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1:1 thumbnail with plus (select) / pen (change) icons
            Card(shape = RoundedCornerShape(16.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        generatingThumbnail -> Text("Generating thumbnail…")
                        thumbnailPath != null -> AsyncImage(
                            model = thumbnailPath,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        videoUri != null -> Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.size(48.dp))
                        else -> IconButton(
                            onClick = { pickVideoLauncher.launch(arrayOf("video/*")) },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Select video", modifier = Modifier.size(48.dp))
                        }
                    }

                    if (videoUri != null) {
                        // Favorite heart, top-left
                        IconButton(
                            onClick = { isFavorite = !isFavorite },
                            modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFE74C3C) else Color.White
                            )
                        }
                        // Pen icon to change video, bottom-right
                        IconButton(
                            onClick = { pickVideoLauncher.launch(arrayOf("video/*")) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Change video", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            if (videoUri != null) {
                Text(
                    "Only the video's link is saved — the original file stays in place and is never copied.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isMissing) {
                Text(
                    "This video wasn't found on this device. Tap the pen icon above to relink it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800)
                )
            }

            // Upload status, above title, horizontal
            Text("Upload Status", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UploadToggle("Facebook", uploadFacebook) { uploadFacebook = it }
                UploadToggle("TikTok", uploadTiktok) { uploadTiktok = it }
                UploadToggle("YouTube", uploadYoutube) { uploadYoutube = it }
                UploadToggle("Instagram", uploadInstagram) { uploadInstagram = it }
            }

            CopyableField(label = "Title", value = title, onValueChange = { title = it }, context = context)

            Column {
                CopyableField(
                    label = "Prompt *",
                    value = prompt,
                    onValueChange = { prompt = it },
                    context = context,
                    minLines = 3,
                    isError = promptError,
                    extraAction = {
                        IconButton(
                            onClick = {
                                autoFilling = true
                                scope.launch {
                                    val result = viewModel.autoFillFromPrompt(prompt)
                                    autoFilling = false
                                    result.onSuccess {
                                        title = it.title.ifBlank { title }
                                        hashtags = it.hashtags.ifBlank { hashtags }
                                        keywords = it.keywords.ifBlank { keywords }
                                        Toast.makeText(context, "Auto-filled from AI", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, it.message ?: "Auto-fill failed", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = prompt.isNotBlank() && !autoFilling
                        ) {
                            if (autoFilling) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Filled.AutoAwesome, contentDescription = "Auto-fill Title/Hashtags/Keywords")
                        }
                    }
                )
                if (promptError) {
                    Text("Prompt is required", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                val wordCount = prompt.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
                Text(
                    "${prompt.length} characters · $wordCount words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            CopyableField(
                label = "Hashtags", value = hashtags, onValueChange = { hashtags = it }, context = context, minLines = 2,
                extraAction = {
                    IconButton(onClick = { hashtags = lineBreakFormat(hashtags) }) {
                        Icon(Icons.Filled.Reorder, contentDescription = "One per line")
                    }
                }
            )

            CopyableField(
                label = "Keywords", value = keywords, onValueChange = { keywords = it }, context = context, minLines = 2,
                extraAction = {
                    IconButton(onClick = { keywords = lineBreakFormat(keywords) }) {
                        Icon(Icons.Filled.Reorder, contentDescription = "One per line")
                    }
                }
            )

            CopyableField(label = "Notes", value = notes, onValueChange = { notes = it }, context = context, minLines = 2)

            EditableDropdownField(label = "Category", options = Categories.defaults, selected = category, onSelected = { category = it })
            EditableDropdownField(label = "AI Model", options = AiModels.defaults, selected = aiModel, onSelected = { aiModel = it })

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Pinned")
                    Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                }
            }
        }
    }

    if (duplicateWarning) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { duplicateWarning = false },
            title = { Text("Already in your library") },
            text = { Text("This video is already added. Pick a different one, or edit the existing entry instead.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { duplicateWarning = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun UploadToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label.take(2).uppercase(), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun CopyableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    context: Context,
    minLines: Int = 1,
    isError: Boolean = false,
    extraAction: (@Composable () -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            minLines = minLines,
            singleLine = minLines == 1,
            isError = isError
        )
        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            extraAction?.invoke()
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
                    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy $label", modifier = Modifier.size(15.dp))
            }
        }
    }
}

private fun lineBreakFormat(text: String): String {
    return text
        .split(Regex("[,\\n]+|\\s+(?=#)"))
        .flatMap { it.trim().split(Regex("\\s+")) }
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableDropdownField(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isEditable = selected == "Other" || selected !in options

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = { if (isEditable) onSelected(it) },
            readOnly = !isEditable,
            label = { Text(label) },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Show options"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
