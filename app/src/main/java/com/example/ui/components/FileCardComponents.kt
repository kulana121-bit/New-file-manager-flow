package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.theme.AmberZip
import com.example.ui.theme.BlueVideo
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GrayTextFile
import com.example.ui.theme.GreenSheet
import com.example.ui.theme.PurpleAudio
import com.example.ui.theme.RedPdf
import com.example.ui.theme.TealCode
import java.io.File

@Composable
fun FileRowItem(
    item: FileItem,
    onClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode && onToggleSelect != null) {
                    onToggleSelect()
                } else {
                    onClick()
                }
            }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect?.invoke() },
                colors = CheckboxDefaults.colors(
                    checkedColor = CyanGlow,
                    checkmarkColor = Color.Black
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // File icon badge matching screenshot designs
        FileIconBadge(item = item)

        Spacer(modifier = Modifier.width(16.dp))

        // File name & meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (item.isDirectory) {
                    "${item.itemCount} items • ${item.formattedDate}"
                } else {
                    "${item.formattedSize} • ${item.formattedDate}"
                },
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }

        if (onMenuClick != null && !isSelectionMode) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreHoriz,
                    contentDescription = "Options",
                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }
        }
    }
}

@Composable
fun FileIconBadge(
    item: FileItem,
    modifier: Modifier = Modifier
) {
    val (icon, badgeColor) = getFileVisuals(item)

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(badgeColor.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

fun getFileVisuals(item: FileItem): Pair<ImageVector, Color> {
    if (item.isDirectory) {
        return Icons.Default.Folder to Color(0xFF3B82F6)
    }
    val ext = item.extension.lowercase()
    return when {
        ext == "pdf" -> Icons.Default.PictureAsPdf to RedPdf
        ext in listOf("mp4", "mkv", "mov", "avi", "webm") -> Icons.Default.VideoFile to BlueVideo
        ext in listOf("xls", "xlsx", "csv") -> Icons.Default.TableChart to GreenSheet
        ext in listOf("zip", "7z", "tar", "gz", "rar") -> Icons.Default.Archive to AmberZip
        ext in listOf("mp3", "wav", "m4a", "flac", "aac", "ogg") -> Icons.Default.AudioFile to PurpleAudio
        ext in listOf("jpg", "jpeg", "png", "webp", "gif") -> Icons.Default.Image to Color(0xFFEC4899)
        ext in listOf("html", "js", "ts", "kt", "java", "json", "xml", "css", "py") -> Icons.Default.Code to TealCode
        else -> Icons.Default.Description to GrayTextFile
    }
}

@Composable
fun FileGridCard(
    item: FileItem,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val isImageOrVideo = item.category == FileCategory.IMAGE || item.category == FileCategory.VIDEO

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1E2836) else Color(0xFFE2E8F0))
            .clickable { onClick() }
    ) {
        if (isImageOrVideo && File(item.path).exists()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(item.path))
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video indicator badge in top right (matching screenshot 5)
            if (item.category == FileCategory.VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FileIconBadge(item = item)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isSelected) "Selected" else "Unselected",
                    tint = if (isSelected) CyanGlow else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
