package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FileRepository
import com.example.data.RecycleBinManager
import com.example.data.VaultManager
import com.example.model.ArchiveFormat
import com.example.model.FileItem
import com.example.model.SortOrder
import com.example.model.StorageStats
import com.example.model.ViewMode
import com.example.service.ArchiveService
import com.example.ui.components.CreateArchiveDialog
import com.example.ui.components.DeleteConfirmationDialog
import com.example.ui.components.ExtractArchiveDialog
import com.example.ui.components.FileActionBottomSheet
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.FileGridCard
import com.example.ui.components.FileRowItem
import com.example.ui.components.FrostedGlassBox
import com.example.ui.components.FrostedSearchBar
import com.example.ui.components.TextInputDialog
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import com.example.ui.viewers.shareFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileBrowserScreen(
    initialDirectory: File,
    repository: FileRepository,
    archiveService: ArchiveService,
    vaultManager: VaultManager,
    recycleBinManager: RecycleBinManager,
    onBack: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var currentDir by remember { mutableStateOf(initialDirectory) }
    var fileList by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var stats by remember { mutableStateOf(repository.getStorageStats()) }
    var showHidden by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    var sortOrder by remember { mutableStateOf(SortOrder.NAME_ASC) }
    var searchQuery by remember { mutableStateOf("") }

    // Multi-selection state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }

    // Clipboard (Copy / Cut)
    var clipboardFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isCutOperation by remember { mutableStateOf(false) }

    // Dialogs
    var selectedFileForAction by remember { mutableStateOf<FileItem?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<FileItem?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf<FileItem?>(null) }
    var showArchiveDialog by remember { mutableStateOf<List<FileItem>?>(null) }
    var showExtractDialog by remember { mutableStateOf<FileItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    // Refresh directory contents
    fun refreshCurrentDir() {
        scope.launch {
            fileList = repository.listFiles(currentDir, showHidden, sortOrder)
            stats = repository.getStorageStats()
        }
    }

    LaunchedEffect(currentDir, showHidden, sortOrder) {
        refreshCurrentDir()
    }

    // Handle back button: navigate up directory if possible
    BackHandler {
        val parent = currentDir.parentFile
        if (parent != null && parent.canRead() && currentDir.absolutePath != repository.defaultRoot.absolutePath) {
            currentDir = parent
            selectedPaths = emptySet()
            isSelectionMode = false
        } else {
            onBack()
        }
    }

    val displayFiles = if (searchQuery.isBlank()) {
        fileList
    } else {
        fileList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val folderCount = displayFiles.count { it.isDirectory }
    val regularFileCount = displayFiles.size - folderCount

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F141C) else Color(0xFFF3F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Header Bar matching Image 6 screen 6 "Internal Storage"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                        .clickable {
                            val parent = currentDir.parentFile
                            if (parent != null && parent.canRead() && currentDir.absolutePath != repository.defaultRoot.absolutePath) {
                                currentDir = parent
                            } else {
                                onBack()
                            }
                        }
                        .testTag("browser_back"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (currentDir.absolutePath == repository.defaultRoot.absolutePath) "Internal Storage" else currentDir.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Grid / List View Toggle
                IconButton(onClick = {
                    viewMode = if (viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                }) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                        contentDescription = "Toggle View",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }

                // Sort & More
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (showHidden) "Hide hidden files" else "Show hidden files") },
                            leadingIcon = {
                                Icon(if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            },
                            onClick = {
                                showHidden = !showHidden
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Name") },
                            onClick = {
                                sortOrder = if (sortOrder == SortOrder.NAME_ASC) SortOrder.NAME_DESC else SortOrder.NAME_ASC
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Date") },
                            onClick = {
                                sortOrder = if (sortOrder == SortOrder.DATE_DESC) SortOrder.DATE_ASC else SortOrder.DATE_DESC
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Size") },
                            onClick = {
                                sortOrder = if (sortOrder == SortOrder.SIZE_DESC) SortOrder.SIZE_ASC else SortOrder.SIZE_DESC
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isSelectionMode) "Exit Selection" else "Multi-select") },
                            leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                            onClick = {
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedPaths = emptySet()
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Search pill matching Image 6 screen 6: "Search in Internal Storage..."
            FrostedSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                hintText = "Search in ${currentDir.name}…"
            )

            // Storage Details Strip matching Image 6 screen 6: "48.2 GB free of 128 GB" | "24 Folders • 1,429 Files"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${stats.formattedFree} free of ${stats.formattedTotal}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
                Text(
                    text = "$folderCount Folders • $regularFileCount Files",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }

            // Breadcrumb path scroll
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pathSegments = getBreadcrumbs(currentDir, repository.defaultRoot)
                pathSegments.forEachIndexed { index, pair ->
                    val (title, file) = pair
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (index == pathSegments.lastIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == pathSegments.lastIndex) CyanGlow else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { currentDir = file }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    if (index < pathSegments.lastIndex) {
                        Text(
                            text = " / ",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Selection Mode Bar
            if (isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF161E2B) else Color(0xFFE2E8F0))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedPaths.size} selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Row {
                        IconButton(onClick = {
                            selectedPaths = if (selectedPaths.size == displayFiles.size) {
                                emptySet()
                            } else {
                                displayFiles.map { it.path }.toSet()
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }

                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedPaths = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // File items: List or Grid view
            if (displayFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No files match \"$searchQuery\"" else "Folder is empty",
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else if (viewMode == ViewMode.LIST) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(displayFiles, key = { it.path }) { item ->
                        val isSelected = selectedPaths.contains(item.path)
                        FileRowItem(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                            },
                            onClick = {
                                if (item.isDirectory) {
                                    currentDir = File(item.path)
                                    selectedPaths = emptySet()
                                } else {
                                    onOpenFile(item)
                                }
                            },
                            onMenuClick = { selectedFileForAction = item }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayFiles, key = { it.path }) { item ->
                        val isSelected = selectedPaths.contains(item.path)
                        FileGridCard(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                } else if (item.isDirectory) {
                                    currentDir = File(item.path)
                                } else {
                                    onOpenFile(item)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Bottom Action Bar for Multi-Select (Copy, Cut, Delete, Archive, Share)
        if (isSelectionMode && selectedPaths.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xCC1A2332))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Batch Copy
                    IconButton(onClick = {
                        clipboardFiles = selectedPaths.map { File(it) }
                        isCutOperation = false
                        isSelectionMode = false
                        Toast.makeText(context, "${clipboardFiles.size} items copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Archive, contentDescription = "Copy", tint = Color.White)
                    }

                    // Batch Compress
                    IconButton(onClick = {
                        val items = selectedPaths.mapNotNull { p -> fileList.find { it.path == p } }
                        showArchiveDialog = items
                    }) {
                        Icon(Icons.Default.Archive, contentDescription = "Compress", tint = CyanGlow)
                    }

                    // Batch Delete
                    IconButton(onClick = { showBatchDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                    }
                }
            }
        } else {
            // Floating Action Button with Menu (New Folder, New File, Paste)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(bottom = 80.dp, end = 20.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (clipboardFiles.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    if (isCutOperation) {
                                        repository.moveFiles(clipboardFiles, currentDir)
                                        clipboardFiles = emptyList()
                                        isCutOperation = false
                                    } else {
                                        repository.copyFiles(clipboardFiles, currentDir)
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Pasted into ${currentDir.name}", Toast.LENGTH_SHORT).show()
                                        refreshCurrentDir()
                                    }
                                }
                            },
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    }

                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = CyanGlow,
                        contentColor = Color.Black
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add New")
                    }

                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Folder") },
                            leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                showCreateFolderDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New Text File") },
                            leadingIcon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                            onClick = {
                                showFabMenu = false
                                showCreateFileDialog = true
                            }
                        )
                    }
                }
            }
        }

        // File Action Bottom Sheet
        selectedFileForAction?.let { item ->
            FileActionBottomSheet(
                item = item,
                onDismiss = { selectedFileForAction = null },
                onOpen = {
                    selectedFileForAction = null
                    if (item.isDirectory) currentDir = File(item.path) else onOpenFile(item)
                },
                onCopy = {
                    clipboardFiles = listOf(File(item.path))
                    isCutOperation = false
                    selectedFileForAction = null
                    Toast.makeText(context, "Copied ${item.name}", Toast.LENGTH_SHORT).show()
                },
                onCut = {
                    clipboardFiles = listOf(File(item.path))
                    isCutOperation = true
                    selectedFileForAction = null
                    Toast.makeText(context, "Cut ${item.name}", Toast.LENGTH_SHORT).show()
                },
                onRename = {
                    showRenameDialog = item
                    selectedFileForAction = null
                },
                onDelete = {
                    showDeleteConfirmDialog = item
                    selectedFileForAction = null
                },
                onLockToVault = {
                    selectedFileForAction = null
                    scope.launch(Dispatchers.IO) {
                        vaultManager.lockFile(File(item.path))
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Moved ${item.name} to Safe Vault", Toast.LENGTH_SHORT).show()
                            refreshCurrentDir()
                        }
                    }
                },
                onArchive = {
                    showArchiveDialog = listOf(item)
                    selectedFileForAction = null
                },
                onExtract = {
                    showExtractDialog = item
                    selectedFileForAction = null
                },
                onShare = {
                    selectedFileForAction = null
                    shareFile(context, File(item.path))
                },
                onDetails = {
                    showDetailsDialog = item
                    selectedFileForAction = null
                },
                onToggleFavorite = {
                    repository.toggleFavorite(item.path)
                    refreshCurrentDir()
                    selectedFileForAction = null
                }
            )
        }

        // Create Folder Dialog
        if (showCreateFolderDialog) {
            TextInputDialog(
                title = "Create New Folder",
                initialValue = "New Folder",
                onDismiss = { showCreateFolderDialog = false },
                onConfirm = { name ->
                    scope.launch(Dispatchers.IO) {
                        val res = repository.createFolder(currentDir, name)
                        withContext(Dispatchers.Main) {
                            showCreateFolderDialog = false
                            if (res.isSuccess) {
                                refreshCurrentDir()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }

        // Create Text File Dialog
        if (showCreateFileDialog) {
            TextInputDialog(
                title = "Create Text File",
                initialValue = "notes.txt",
                onDismiss = { showCreateFileDialog = false },
                onConfirm = { name ->
                    scope.launch(Dispatchers.IO) {
                        val res = repository.createFile(currentDir, name, "")
                        withContext(Dispatchers.Main) {
                            showCreateFileDialog = false
                            if (res.isSuccess) {
                                refreshCurrentDir()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }

        // Rename Dialog
        showRenameDialog?.let { item ->
            TextInputDialog(
                title = "Rename",
                initialValue = item.name,
                onDismiss = { showRenameDialog = null },
                onConfirm = { newName ->
                    scope.launch(Dispatchers.IO) {
                        val res = repository.renameFile(File(item.path), newName)
                        withContext(Dispatchers.Main) {
                            showRenameDialog = null
                            if (res.isSuccess) {
                                refreshCurrentDir()
                            } else {
                                Toast.makeText(context, res.exceptionOrNull()?.message ?: "Rename failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }

        // Delete Confirm Dialog (Single)
        showDeleteConfirmDialog?.let { item ->
            DeleteConfirmationDialog(
                itemName = item.name,
                onDismiss = { showDeleteConfirmDialog = null },
                onMoveToTrash = {
                    scope.launch(Dispatchers.IO) {
                        recycleBinManager.moveToTrash(File(item.path))
                        withContext(Dispatchers.Main) {
                            showDeleteConfirmDialog = null
                            Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                            refreshCurrentDir()
                        }
                    }
                },
                onDeletePermanently = {
                    scope.launch(Dispatchers.IO) {
                        File(item.path).deleteRecursively()
                        withContext(Dispatchers.Main) {
                            showDeleteConfirmDialog = null
                            Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
                            refreshCurrentDir()
                        }
                    }
                }
            )
        }

        // Batch Delete Confirm Dialog
        if (showBatchDeleteDialog) {
            DeleteConfirmationDialog(
                itemName = "${selectedPaths.size} items",
                isMultiple = true,
                onDismiss = { showBatchDeleteDialog = false },
                onMoveToTrash = {
                    scope.launch(Dispatchers.IO) {
                        for (p in selectedPaths) {
                            recycleBinManager.moveToTrash(File(p))
                        }
                        withContext(Dispatchers.Main) {
                            showBatchDeleteDialog = false
                            isSelectionMode = false
                            selectedPaths = emptySet()
                            Toast.makeText(context, "Items moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                            refreshCurrentDir()
                        }
                    }
                },
                onDeletePermanently = {
                    scope.launch(Dispatchers.IO) {
                        for (p in selectedPaths) {
                            File(p).deleteRecursively()
                        }
                        withContext(Dispatchers.Main) {
                            showBatchDeleteDialog = false
                            isSelectionMode = false
                            selectedPaths = emptySet()
                            Toast.makeText(context, "Items deleted permanently", Toast.LENGTH_SHORT).show()
                            refreshCurrentDir()
                        }
                    }
                }
            )
        }

        // Details Dialog
        showDetailsDialog?.let { item ->
            FileDetailsDialog(
                item = item,
                onDismiss = { showDetailsDialog = null }
            )
        }

        // Create Archive Dialog
        showArchiveDialog?.let { itemsToCompress ->
            CreateArchiveDialog(
                initialName = if (itemsToCompress.size == 1) itemsToCompress.first().nameWithoutExt() else "Archive",
                onDismiss = { showArchiveDialog = null },
                onConfirm = { archiveName, format, password ->
                    scope.launch(Dispatchers.IO) {
                        val destination = File(currentDir, archiveName)
                        val sourceFiles = itemsToCompress.map { File(it.path) }
                        val result = archiveService.createArchive(sourceFiles, destination, password, format)
                        withContext(Dispatchers.Main) {
                            showArchiveDialog = null
                            isSelectionMode = false
                            selectedPaths = emptySet()
                            if (result.isSuccess) {
                                Toast.makeText(context, "Created archive: $archiveName", Toast.LENGTH_SHORT).show()
                                refreshCurrentDir()
                            } else {
                                Toast.makeText(context, "Compression failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }

        // Extract Archive Dialog
        showExtractDialog?.let { archiveItem ->
            val isEncrypted = remember(archiveItem.path) {
                var enc = false
                scope.launch { enc = archiveService.isArchiveEncrypted(File(archiveItem.path)) }
                enc
            }

            ExtractArchiveDialog(
                archiveName = archiveItem.name,
                isEncrypted = isEncrypted,
                onDismiss = { showExtractDialog = null },
                onConfirm = { password ->
                    scope.launch(Dispatchers.IO) {
                        val destFolder = File(currentDir, archiveItem.nameWithoutExt())
                        val result = archiveService.extractArchive(File(archiveItem.path), destFolder, password)
                        withContext(Dispatchers.Main) {
                            showExtractDialog = null
                            if (result.isSuccess) {
                                Toast.makeText(context, "Extracted to ${destFolder.name}", Toast.LENGTH_SHORT).show()
                                refreshCurrentDir()
                            } else {
                                val err = result.exceptionOrNull()
                                if (err?.message == "ARCHIVE_PASSWORD_REQUIRED") {
                                    Toast.makeText(context, "Password is required to extract this archive", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Extraction error: ${err?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}

private fun FileItem.nameWithoutExt(): String {
    return if (isDirectory) name else name.substringBeforeLast('.', name)
}

private fun getBreadcrumbs(dir: File, root: File): List<Pair<String, File>> {
    val list = mutableListOf<Pair<String, File>>()
    var curr: File? = dir
    while (curr != null) {
        val label = if (curr.absolutePath == root.absolutePath) "Storage" else curr.name
        list.add(0, label to curr)
        if (curr.absolutePath == root.absolutePath) break
        curr = curr.parentFile
    }
    return list
}
