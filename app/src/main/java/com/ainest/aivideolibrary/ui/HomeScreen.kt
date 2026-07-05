package com.ainest.aivideolibrary.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.ainest.aivideolibrary.data.DashboardFilter
import com.ainest.aivideolibrary.data.VideoItem
import com.ainest.aivideolibrary.util.BiometricUtil
import com.ainest.aivideolibrary.util.PrefsManager
import com.ainest.aivideolibrary.viewmodel.SearchScope
import com.ainest.aivideolibrary.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: VideoViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onOpenVideo: (Long) -> Unit,
    onAddSingleVideo: () -> Unit,
    onAddMultipleVideos: (List<android.net.Uri>) -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val videos by viewModel.displayedVideos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchScope by viewModel.searchScope.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val aiModels by viewModel.aiModels.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()

    var searchExpanded by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }
    var pendingBackupPassword by remember { mutableStateOf(false) }
    var backupTargetUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val prefs = remember { PrefsManager(context.applicationContext) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            if (prefs.backupEncryptionEnabled) {
                pendingBackupPassword = true
                // store target uri briefly via closure below
                backupTargetUri = uri
            } else {
                viewModel.exportBackup(uri, null) { ok ->
                    Toast.makeText(context, if (ok) "Backup saved" else "Backup failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            viewModel.exportCsv(uri) { ok ->
                Toast.makeText(context, if (ok) "CSV exported" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val multiPickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) onAddMultipleVideos(uris)
    }

    // Needed so the "Locate Video" / auto-relink feature can query MediaStore for matches.
    val mediaPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val permission = if (android.os.Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            mediaPermissionLauncher.launch(permission)
        }
    }

    Scaffold(
        topBar = {
            Column {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedIds.size} selected") },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                if (activity != null && BiometricUtil.isAvailable(activity)) {
                                    BiometricUtil.confirm(
                                        activity = activity,
                                        title = "Confirm delete",
                                        subtitle = "Verify it's you to delete ${selectedIds.size} video(s)",
                                        onSuccess = { viewModel.softDeleteSelected() },
                                        onUnavailable = { pendingDelete = true },
                                        onCancelled = {}
                                    )
                                } else {
                                    pendingDelete = true
                                }
                            }) {
                                Icon(Icons.Filled.Fingerprint, contentDescription = "Delete selected")
                            }
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            if (searchExpanded) {
                                RoundedSearchField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.setSearchQuery(it) },
                                    focusRequester = focusRequester,
                                    onCollapse = {
                                        searchExpanded = false
                                        keyboardController?.hide()
                                    }
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text("AI Video Library", fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                searchExpanded = !searchExpanded
                            }) {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        },
                        actions = {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(Icons.Filled.Tune, contentDescription = "Filter / Sort")
                            }
                            IconButton(onClick = onToggleDarkTheme) {
                                Icon(if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode, contentDescription = "Toggle theme")
                            }
                            Box {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Filled.Settings, contentDescription = "More")
                                }
                                DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                    DropdownMenuItem(text = { Text("Backup Library") }, leadingIcon = { Icon(Icons.Filled.Backup, contentDescription = null) }, onClick = {
                                        showOverflowMenu = false
                                        exportLauncher.launch("ai_video_library_backup.json")
                                    })
                                    DropdownMenuItem(text = { Text("Restore Library") }, leadingIcon = { Icon(Icons.Filled.Restore, contentDescription = null) }, onClick = {
                                        showOverflowMenu = false
                                        importLauncher.launch(arrayOf("application/json"))
                                    })
                                    DropdownMenuItem(text = { Text("Export CSV") }, leadingIcon = { Icon(Icons.Filled.TableChart, contentDescription = null) }, onClick = {
                                        showOverflowMenu = false
                                        csvExportLauncher.launch("ai_video_library.csv")
                                    })
                                    DropdownMenuItem(text = { Text("Recycle Bin") }, leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) }, onClick = {
                                        showOverflowMenu = false
                                        onOpenRecycleBin()
                                    })
                                    DropdownMenuItem(text = { Text("Settings") }, leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) }, onClick = {
                                        showOverflowMenu = false
                                        onOpenSettings()
                                    })
                                }
                            }
                        }
                    )
                    if (searchExpanded) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SearchScope.entries.forEach { scope ->
                                FilterChip(selected = searchScope == scope, onClick = { viewModel.setSearchScope(scope) }, label = { Text(scope.label) })
                            }
                        }
                    }
                    DashboardRow(
                        stats = stats,
                        activeFilter = filterState.dashboardFilter,
                        onFilterClick = { viewModel.toggleDashboardFilter(it) },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                Box {
                    FloatingActionButton(onClick = { showAddMenu = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Video")
                    }
                    DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                        DropdownMenuItem(text = { Text("Add Single Video") }, onClick = { showAddMenu = false; onAddSingleVideo() })
                        DropdownMenuItem(
                            text = { Text("Add Multiple Videos") },
                            leadingIcon = { Icon(Icons.Filled.PlaylistAdd, contentDescription = null) },
                            onClick = { showAddMenu = false; multiPickLauncher.launch(arrayOf("video/*")) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (searchExpanded) {
                        searchExpanded = false
                        keyboardController?.hide()
                    }
                }
        ) {
            if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.VideoLibrary, contentDescription = null, modifier = Modifier.padding(bottom = 8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (searchQuery.isNotBlank() || filterState.isActive) "No videos match your search/filters" else "Your library is empty.\nTap Add Video to get started.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(videos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            isSelectionMode = isSelectionMode,
                            isSelected = video.id in selectedIds,
                            onEdit = { onOpenVideo(video.id) },
                            onDelete = { viewModel.softDeleteSingle(video) },
                            onToggleFavorite = { viewModel.toggleFavorite(video) },
                            onTogglePinned = { viewModel.togglePinned(video) },
                            onToggleUpload = { platform -> viewModel.toggleUploadStatus(video, platform) },
                            onToggleSelected = { viewModel.toggleSelected(video.id) },
                            onLocateVideo = { onOpenVideo(video.id) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSortSheet(
            filterState = filterState,
            sortOption = sortOption,
            categories = categories,
            aiModels = aiModels,
            onFilterChange = { viewModel.setFilterState(it) },
            onSortChange = { viewModel.setSortOption(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (pendingDelete) {
        ConfirmDeleteDialog(
            count = selectedIds.size,
            onConfirm = { viewModel.softDeleteSelected(); pendingDelete = false },
            onDismiss = { pendingDelete = false }
        )
    }

    if (pendingBackupPassword) {
        var pwd by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingBackupPassword = false },
            title = { Text("Set backup password") },
            text = {
                OutlinedTextField(value = pwd, onValueChange = { pwd = it }, label = { Text("Password") }, singleLine = true)
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val target = backupTargetUri
                    if (target != null && pwd.isNotBlank()) {
                        viewModel.exportBackup(target, pwd) { ok ->
                            Toast.makeText(context, if (ok) "Encrypted backup saved" else "Backup failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    pendingBackupPassword = false
                }) { Text("Save") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingBackupPassword = false }) { Text("Cancel") } }
        )
    }

    pendingRestoreUri?.let { uri ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Restore Library") },
            text = {
                Column {
                    Text("If this backup is password-protected, enter the password. Otherwise leave blank.")
                    OutlinedTextField(value = restorePassword, onValueChange = { restorePassword = it }, label = { Text("Password (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.importBackup(uri, restorePassword.ifBlank { null }) { ok ->
                        Toast.makeText(context, if (ok) "Library restored" else "Restore failed - check password", Toast.LENGTH_SHORT).show()
                    }
                    pendingRestoreUri = null
                    restorePassword = ""
                }) { Text("Restore") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { pendingRestoreUri = null; restorePassword = "" }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoundedSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onCollapse: () -> Unit
) {
    androidx.compose.runtime.LaunchedEffect(Unit) { focusRequester.requestFocus() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text("Search title, prompt, keywords…") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onCollapse) { Icon(Icons.Filled.Close, contentDescription = "Close search") }
        },
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
    )
}
