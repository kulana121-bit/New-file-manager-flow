package com.example.ui.viewers

import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.FileItem
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    item: FileItem,
    onBack: () -> Unit,
    onLockToggle: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val file = File(item.path)

    var content by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(1) } // 0: Preview, 1: Source Code
    var isEditing by remember { mutableStateOf(false) }
    var showFindReplaceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(item.path) {
        withContext(Dispatchers.IO) {
            try {
                if (file.exists()) {
                    content = file.readText()
                }
            } catch (e: Exception) {
                content = "Error loading file content: ${e.message}"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F141C) else Color(0xFF1E2530))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Navigation Bar matching Image 4 right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onBack() }
                        .testTag("code_back"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = CyanGlow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Source",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { shareFile(context, file) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // File Title & Info
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
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onDetails() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Segmented Tab [Preview] [Source Code]
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF263042))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedTab == 0) Color(0xFF38455A) else Color.Transparent)
                            .clickable { selectedTab = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Preview",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color.White else Color(0xFF94A3B8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selectedTab == 1) Color(0xFF38455A) else Color.Transparent)
                            .clickable { selectedTab = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Source Code",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Editor / Viewer Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF141A24))
            ) {
                if (selectedTab == 0) {
                    // Preview (Rendered HTML or plaintext)
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                loadDataWithBaseURL(null, content, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Source Code View with line numbers & syntax styling
                    val lines = content.lines()
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    if (isEditing) {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0)
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanGlow,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(verticalScroll)
                                .horizontalScroll(horizontalScroll)
                                .padding(vertical = 12.dp)
                        ) {
                            // Line numbers gutter
                            Column(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp)
                                    .width(36.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                lines.indices.forEach { index ->
                                    Text(
                                        text = "${index + 1}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = Color(0xFF4B5563)
                                    )
                                }
                            }

                            // Code Lines with syntax coloring
                            Column(modifier = Modifier.padding(end = 20.dp)) {
                                lines.forEach { line ->
                                    Text(
                                        text = highlightSyntax(line),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Bar matching Image 4 right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(Color(0xCC1A2332))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomCodeAction(
                        icon = if (item.isSecured) Icons.Default.LockOpen else Icons.Default.Lock,
                        label = if (item.isSecured) "Unhide" else "Lock",
                        onClick = onLockToggle
                    )

                    BottomCodeAction(
                        icon = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                        label = if (isEditing) "Save" else "Edit",
                        onClick = {
                            if (isEditing) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        file.writeText(content)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Saved changes", Toast.LENGTH_SHORT).show()
                                            isEditing = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                isEditing = true
                            }
                        },
                        tint = if (isEditing) CyanGlow else null
                    )

                    BottomCodeAction(
                        icon = Icons.Default.FindReplace,
                        label = "Find/Replace",
                        onClick = { showFindReplaceDialog = true }
                    )

                    BottomCodeAction(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = { shareFile(context, file) }
                    )

                    BottomCodeAction(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        onClick = onDelete,
                        tint = DangerRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Find & Replace Modal
        if (showFindReplaceDialog) {
            FindReplaceModal(
                onDismiss = { showFindReplaceDialog = false },
                onReplace = { find, replace ->
                    if (find.isNotEmpty()) {
                        content = content.replace(find, replace)
                        scope.launch(Dispatchers.IO) {
                            file.writeText(content)
                        }
                        Toast.makeText(context, "Replaced occurrences", Toast.LENGTH_SHORT).show()
                    }
                    showFindReplaceDialog = false
                }
            )
        }
    }
}

@Composable
private fun BottomCodeAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null
) {
    val finalTint = tint ?: Color.White

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

@Composable
private fun FindReplaceModal(
    onDismiss: () -> Unit,
    onReplace: (find: String, replace: String) -> Unit
) {
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find & Replace", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = findText,
                    onValueChange = { findText = it },
                    label = { Text("Find") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = replaceText,
                    onValueChange = { replaceText = it },
                    label = { Text("Replace with") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onReplace(findText, replaceText) },
                enabled = findText.isNotEmpty()
            ) {
                Text("Replace All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun highlightSyntax(line: String): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val tagRegex = Regex("""(</?[a-zA-Z0-9_-]+)|([a-zA-Z0-9_-]+(?==))|("(?:[^"\\]|\\.)*")|(<!--.*?-->)|(//.*)|(/\*.*?\*/)""")
        val matches = tagRegex.findAll(line).toList()

        for (match in matches) {
            if (match.range.first > cursor) {
                append(line.substring(cursor, match.range.first))
            }

            val text = match.value
            when {
                text.startsWith("<") -> {
                    pushStyle(SpanStyle(color = Color(0xFFFF6484), fontWeight = FontWeight.SemiBold))
                    append(text)
                    pop()
                }
                text.startsWith("\"") -> {
                    pushStyle(SpanStyle(color = Color(0xFF68D391)))
                    append(text)
                    pop()
                }
                text.startsWith("//") || text.startsWith("<!--") || text.startsWith("/*") -> {
                    pushStyle(SpanStyle(color = Color(0xFF718096)))
                    append(text)
                    pop()
                }
                match.groups[2] != null -> {
                    pushStyle(SpanStyle(color = Color(0xFFF6AD55)))
                    append(text)
                    pop()
                }
                else -> {
                    append(text)
                }
            }
            cursor = match.range.last + 1
        }

        if (cursor < line.length) {
            append(line.substring(cursor))
        }
    }
}
