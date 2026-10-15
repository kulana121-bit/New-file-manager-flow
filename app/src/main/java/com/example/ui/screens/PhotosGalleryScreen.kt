package com.example.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.components.FileGridCard
import com.example.ui.components.FrostedSearchBar
import com.example.ui.theme.CyanGlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotosGalleryScreen(
    photos: List<FileItem>,
    title: String = "Photos",
    isVaultGallery: Boolean = false,
    onBack: () -> Unit,
    onPhotoClick: (FileItem) -> Unit,
    onAddPhoto: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val isDark = isSystemInDarkTheme()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }

    val filterChips = listOf("All", "Camera", "Screenshots", "Downloads", "Favorites")

    // Filter photos
    val filteredPhotos = photos.filter { item ->
        val matchesSearch = searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Camera" -> item.path.contains("DCIM", ignoreCase = true) || item.path.contains("Camera", ignoreCase = true)
            "Screenshots" -> item.path.contains("Screenshot", ignoreCase = true)
            "Downloads" -> item.path.contains("Download", ignoreCase = true)
            "Favorites" -> item.isFavorite
            else -> true
        }
        matchesSearch && matchesFilter
    }

    // Group photos by date
    val groupedPhotos = filteredPhotos.groupBy { item ->
        val itemDate = Date(item.lastModified)
        val now = Date()
        val diffDays = ((now.time - itemDate.time) / (1000 * 60 * 60 * 24)).toInt()
        when (diffDays) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(itemDate)
        }
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
            // Header Bar matching Image 5
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
                        .clickable { onBack() }
                        .testTag("gallery_back"),
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
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )

                IconButton(
                    onClick = {
                        isSelectionMode = !isSelectionMode
                        if (!isSelectionMode) selectedPaths = emptySet()
                    }
                ) {
                    Icon(
                        imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.EditNote,
                        contentDescription = "Select",
                        tint = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            // Search Bar
            FrostedSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                hintText = "Search photos…"
            )

            // Filter Chips Row matching Image 5
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterChips.forEach { chip ->
                    val isSelected = selectedFilter == chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    if (isDark) CyanGlow else Color(0xFF3B82F6)
                                } else {
                                    if (isDark) Color(0xFF1E2837) else Color(0xFFE2E8F0)
                                }
                            )
                            .clickable { selectedFilter = chip }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = chip,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (isDark) Color.Black else Color.White
                            } else {
                                if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Photo Grid grouped by date (Today, Yesterday...)
            if (filteredPhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isVaultGallery) "No private photos yet.\nTap 'Add Photo' to import." else "No photos found.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    groupedPhotos.forEach { (dateHeader, itemsInDate) ->
                        // Header row (e.g. "Today" with chevron)
                        item(span = { GridItemSpan(3) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Grid items
                        items(itemsInDate, key = { it.path }) { photoItem ->
                            val isSelected = selectedPaths.contains(photoItem.path)
                            FileGridCard(
                                item = photoItem,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedPaths = if (isSelected) {
                                            selectedPaths - photoItem.path
                                        } else {
                                            selectedPaths + photoItem.path
                                        }
                                    } else {
                                        onPhotoClick(photoItem)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
