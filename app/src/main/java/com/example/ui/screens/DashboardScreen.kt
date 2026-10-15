package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FileRepository
import com.example.data.RecycleBinManager
import com.example.data.VaultManager
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.StorageStats
import com.example.ui.components.FileActionBottomSheet
import com.example.ui.components.FileRowItem
import com.example.ui.components.FloatingBottomBar
import com.example.ui.components.FrostedSearchBar
import com.example.ui.components.QuickAccessGrid
import com.example.ui.components.StorageGaugeCard
import com.example.ui.viewers.shareFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DashboardScreen(
    repository: FileRepository,
    vaultManager: VaultManager,
    recycleBinManager: RecycleBinManager,
    onCategoryClick: (FileCategory) -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenStorageAnalyzer: () -> Unit,
    onOpenSecuredVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var stats by remember { mutableStateOf(repository.getStorageStats()) }
    var recentFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var selectedFileForAction by remember { mutableStateOf<FileItem?>(null) }

    fun refreshDashboard() {
        scope.launch {
            stats = repository.getStorageStats()
            recentFiles = repository.getRecentFiles(8)
        }
    }

    LaunchedEffect(Unit) {
        refreshDashboard()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F141C) else Color(0xFFF3F5F9))
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Top Frosted Search Bar matching Image 1
            item {
                FrostedSearchBar(
                    query = "",
                    onQueryChange = {},
                    onSearchClick = onOpenSearch,
                    onNotificationClick = onOpenSecuredVault,
                    hintText = "Search files, folders…"
                )
            }

            // Storage Section Header & Card matching Image 1
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Storage",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    StorageGaugeCard(
                        stats = stats,
                        onAnalyzeClick = onOpenStorageAnalyzer
                    )
                }
            }

            // Quick Access Section Header & 2x2 Grid matching Image 1
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Quick Access",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    QuickAccessGrid(
                        onCategoryClick = onCategoryClick
                    )
                }
            }

            // Recent Files Section Header matching Image 1
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Files",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            // Recent Files List matching Image 1
            if (recentFiles.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent files yet",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(recentFiles, key = { it.path }) { item ->
                    FileRowItem(
                        item = item,
                        onClick = { onOpenFile(item) },
                        onMenuClick = { selectedFileForAction = item }
                    )
                }
            }
        }

        // Action Bottom Sheet for Recent File
        selectedFileForAction?.let { item ->
            FileActionBottomSheet(
                item = item,
                onDismiss = { selectedFileForAction = null },
                onOpen = {
                    selectedFileForAction = null
                    onOpenFile(item)
                },
                onCopy = { selectedFileForAction = null },
                onCut = { selectedFileForAction = null },
                onRename = { selectedFileForAction = null },
                onDelete = {
                    selectedFileForAction = null
                    scope.launch(Dispatchers.IO) {
                        recycleBinManager.moveToTrash(File(item.path))
                        withContext(Dispatchers.Main) {
                            refreshDashboard()
                        }
                    }
                },
                onLockToVault = {
                    selectedFileForAction = null
                    scope.launch(Dispatchers.IO) {
                        vaultManager.lockFile(File(item.path))
                        withContext(Dispatchers.Main) {
                            refreshDashboard()
                        }
                    }
                },
                onArchive = { selectedFileForAction = null },
                onExtract = { selectedFileForAction = null },
                onShare = {
                    selectedFileForAction = null
                    shareFile(context, File(item.path))
                },
                onDetails = { selectedFileForAction = null },
                onToggleFavorite = {
                    repository.toggleFavorite(item.path)
                    selectedFileForAction = null
                    refreshDashboard()
                }
            )
        }
    }
}
