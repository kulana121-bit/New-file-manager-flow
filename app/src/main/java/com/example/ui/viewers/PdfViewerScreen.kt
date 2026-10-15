package com.example.ui.viewers

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileItem
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerScreen(
    item: FileItem,
    onBack: () -> Unit,
    onLockToggle: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val file = File(item.path)

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(item.path) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            pageCount = renderer.pageCount
            if (pageCount > 0) {
                val page = renderer.openPage(0)
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                currentBitmap = bmp
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to open PDF"
        }

        onDispose {
            try {
                renderer?.close()
                pfd?.close()
            } catch (e: Exception) {}
        }
    }

    LaunchedEffect(currentPageIndex) {
        if (pageCount > 0 && file.exists()) {
            withContext(Dispatchers.IO) {
                try {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                        PdfRenderer(fd).use { r ->
                            val page = r.openPage(currentPageIndex.coerceIn(0, pageCount - 1))
                            val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            currentBitmap = bmp
                        }
                    }
                } catch (e: Exception) {}
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF10141D) else Color(0xFFF3F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top App Bar matching Image 4 left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                        .clickable { onBack() }
                        .testTag("pdf_back"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x14000000))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (item.isSecured) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (item.isSecured) CyanGlow else if (isDark) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (item.isSecured) "Secured File" else "PDF Document",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                        .clickable { shareFile(context, file) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = if (isDark) Color.White else Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Title with Info button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDetails() }
                )
            }

            // Page Indicator
            Text(
                text = if (pageCount > 0) "Page ${currentPageIndex + 1} of $pageCount" else "Loading...",
                fontSize = 13.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // PDF Canvas / Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                currentBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "PDF Page",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    Text(
                        text = errorMessage ?: "Preparing document…",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }

                // Page Navigation Overlays
                if (pageCount > 1) {
                    if (currentPageIndex > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { currentPageIndex-- },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.White)
                        }
                    }

                    if (currentPageIndex < pageCount - 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { currentPageIndex++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Frosted Action Bar matching Image 4 left
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(if (isDark) Color(0xCC1A2332) else Color(0xE6FFFFFF))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomPdfAction(
                        icon = if (item.isSecured) Icons.Default.LockOpen else Icons.Default.Lock,
                        label = if (item.isSecured) "Unhide" else "Lock",
                        onClick = onLockToggle
                    )

                    BottomPdfAction(
                        icon = Icons.Default.Search,
                        label = "Find",
                        onClick = {}
                    )

                    BottomPdfAction(
                        icon = Icons.Default.Print,
                        label = "Print",
                        onClick = { shareFile(context, file) }
                    )

                    BottomPdfAction(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = { shareFile(context, file) }
                    )

                    BottomPdfAction(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        onClick = onDelete,
                        tint = DangerRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BottomPdfAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null
) {
    val isDark = isSystemInDarkTheme()
    val finalTint = tint ?: if (isDark) Color.White else Color(0xFF334155)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = finalTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = finalTint
        )
    }
}
