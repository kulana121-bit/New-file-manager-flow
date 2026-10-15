package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArchiveFormat
import com.example.model.FileItem
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionBottomSheet(
    item: FileItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onLockToVault: () -> Unit,
    onArchive: () -> Unit,
    onExtract: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val isArchive = item.extension.lowercase() in listOf("zip", "tar", "gz", "gzip", "7z")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF161E2B) else Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                FileIconBadge(item = item)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A),
                        maxLines = 1
                    )
                    Text(
                        text = if (item.isDirectory) "${item.itemCount} items" else item.formattedSize,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
            Spacer(modifier = Modifier.height(12.dp))

            // Action Items
            ActionRow(icon = Icons.Default.Share, label = "Share File", onClick = onShare)
            if (isArchive) {
                ActionRow(icon = Icons.Default.Unarchive, label = "Extract Archive…", onClick = onExtract)
            } else {
                ActionRow(icon = Icons.Default.Archive, label = "Compress to ZIP…", onClick = onArchive)
            }
            ActionRow(icon = Icons.Default.Lock, label = "Move to Safe Vault", onClick = onLockToVault)
            ActionRow(icon = Icons.Default.ContentCopy, label = "Copy", onClick = onCopy)
            ActionRow(icon = Icons.Default.ContentCut, label = "Cut (Move)", onClick = onCut)
            ActionRow(icon = Icons.Default.Edit, label = "Rename", onClick = onRename)
            ActionRow(icon = Icons.Default.Info, label = "File Details", onClick = onDetails)
            ActionRow(
                icon = Icons.Default.Delete,
                label = "Delete",
                onClick = onDelete,
                tint = DangerRed
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color? = null
) {
    val isDark = isSystemInDarkTheme()
    val itemTint = tint ?: if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = itemTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = itemTint
        )
    }
}

@Composable
fun CreateArchiveDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, format: ArchiveFormat, password: String?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedFormat by remember { mutableStateOf(ArchiveFormat.ZIP) }
    var password by remember { mutableStateOf("") }
    var enablePassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Archive", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Archive Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Format:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArchiveFormat.values().forEach { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt.name) }
                        )
                    }
                }

                if (selectedFormat == ArchiveFormat.ZIP) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { enablePassword = !enablePassword }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = CyanGlow
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Password Protect (AES-256)", fontSize = 13.sp)
                    }

                    if (enablePassword) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Enter Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (name.endsWith(".${selectedFormat.name.lowercase()}")) name else "$name.${selectedFormat.name.lowercase()}"
                    onConfirm(finalName, selectedFormat, if (enablePassword && password.isNotBlank()) password else null)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = Color.Black)
            ) {
                Text("Create Archive")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExtractArchiveDialog(
    archiveName: String,
    isEncrypted: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (password: String?) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extract Archive", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Extract contents of $archiveName into a new folder?")
                if (isEncrypted) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("This archive is password-protected:", fontSize = 13.sp, color = DangerRed)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Archive Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(if (isEncrypted) password else null) },
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = Color.Black)
            ) {
                Text("Extract")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FileDetailsDialog(
    item: FileItem,
    onDismiss: () -> Unit
) {
    val f = File(item.path)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File Details", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow(label = "Name", value = item.name)
                DetailRow(label = "Type", value = item.extension.ifBlank { if (item.isDirectory) "Folder" else "File" })
                DetailRow(label = "Size", value = item.formattedSize)
                DetailRow(label = "Path", value = item.path)
                DetailRow(label = "Modified", value = item.formattedDate + " " + item.formattedTime)
                DetailRow(label = "Readable", value = f.canRead().toString())
                DetailRow(label = "Writable", value = f.canWrite().toString())
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Normal)
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initialValue: String = "",
    confirmButtonText: String = "OK",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteConfirmationDialog(
    itemName: String,
    isMultiple: Boolean = false,
    onDismiss: () -> Unit,
    onMoveToTrash: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Confirmation", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (isMultiple) "Choose how you want to delete selected items."
                else "How do you want to delete \"$itemName\"?"
            )
        },
        confirmButton = {
            Button(
                onClick = onMoveToTrash,
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = Color.Black)
            ) {
                Text("Move to Recycle Bin")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDeletePermanently,
                colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
            ) {
                Text("Delete Permanently")
            }
        }
    )
}
