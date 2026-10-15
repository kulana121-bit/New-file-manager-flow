package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FileRepository
import com.example.data.RecycleBinManager
import com.example.data.TrashItem
import com.example.model.FileItem
import com.example.ui.components.FileRowItem
import com.example.ui.components.FrostedGlassBox
import com.example.ui.components.StorageGaugeCard
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StorageAnalyzerScreen(
    repository: FileRepository,
    recycleBinManager: RecycleBinManager,
    onBack: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var stats by remember { mutableStateOf(repository.getStorageStats()) }
    var largeFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var duplicateGroups by remember { mutableStateOf<List<List<FileItem>>>(emptyList()) }
    var trashItems by remember { mutableStateOf<List<TrashItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshAll() {
        scope.launch {
            isLoading = true
            stats = repository.getStorageStats()
            largeFiles = repository.findLargeFiles(20L * 1024 * 1024)
            duplicateGroups = repository.findDuplicateFiles()
            trashItems = recycleBinManager.getTrashItems()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
    }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                        .clickable { onBack() }
                        .testTag("analyzer_back"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Storage Analyzer",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanGlow)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Analyzing storage and scanning duplicates…", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Gauge Card
                    item {
                        StorageGaugeCard(stats = stats)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Recycle Bin Section
                    item {
                        FrostedGlassBox(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Recycle Bin",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color.White else Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${trashItems.size} items in trash",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    if (trashItems.isNotEmpty()) {
                                        Button(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    recycleBinManager.emptyTrash()
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Emptied Recycle Bin", Toast.LENGTH_SHORT).show()
                                                        refreshAll()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                                        ) {
                                            Text("Empty Trash")
                                        }
                                    }
                                }

                                if (trashItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    trashItems.take(5).forEach { trash ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = trash.originalName,
                                                fontSize = 13.sp,
                                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    recycleBinManager.restoreItem(trash)
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(context, "Restored ${trash.originalName}", Toast.LENGTH_SHORT).show()
                                                        refreshAll()
                                                    }
                                                }
                                            }) {
                                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = CyanGlow)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Duplicate Files Section
                    item {
                        Text(
                            text = "Duplicate Files (${duplicateGroups.size} groups)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (duplicateGroups.isEmpty()) {
                        item {
                            Text("No duplicate files detected.", fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    } else {
                        items(duplicateGroups) { group ->
                            FrostedGlassBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "${group.first().name} (${group.first().formattedSize})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    group.forEachIndexed { idx, dup ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = dup.path,
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (idx > 0) {
                                                IconButton(onClick = {
                                                    scope.launch(Dispatchers.IO) {
                                                        recycleBinManager.moveToTrash(java.io.File(dup.path))
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Moved duplicate to Trash", Toast.LENGTH_SHORT).show()
                                                            refreshAll()
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    // Large Files Section
                    item {
                        Text(
                            text = "Large Files (> 20 MB)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (largeFiles.isEmpty()) {
                        item {
                            Text("No large files (> 20MB) found.", fontSize = 13.sp, color = Color.Gray)
                        }
                    } else {
                        items(largeFiles) { item ->
                            FileRowItem(
                                item = item,
                                onClick = { onOpenFile(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}
