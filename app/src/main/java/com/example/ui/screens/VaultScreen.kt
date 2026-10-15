package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.VaultManager
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.ui.components.FileActionBottomSheet
import com.example.ui.components.FileRowItem
import com.example.ui.components.FrostedGlassBox
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LavenderCategory
import com.example.ui.theme.PeachCategory
import com.example.ui.theme.SkyBlueCategory
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewers.shareFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    vaultManager: VaultManager,
    onBack: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    onOpenCategoryGallery: (FileCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    val isPinEnabled = vaultManager.isPinProtectionEnabled()
    var isUnlocked by remember { mutableStateOf(!isPinEnabled || vaultManager.isSessionUnlocked) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var lockedFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var lockedCounts by remember { mutableStateOf<Map<FileCategory, Int>>(emptyMap()) }
    var showImportBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForAction by remember { mutableStateOf<FileItem?>(null) }

    fun refreshVault() {
        scope.launch {
            lockedFiles = vaultManager.getLockedFiles()
            lockedCounts = vaultManager.getLockedCounts()
        }
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            refreshVault()
        }
    }

    // Pick visual media from gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                    val temp = File(context.cacheDir, "import_${System.currentTimeMillis()}.jpg")
                    input?.use { inp ->
                        FileOutputStream(temp).use { out -> inp.copyTo(out) }
                    }
                    vaultManager.lockFile(temp)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Added to Vault", Toast.LENGTH_SHORT).show()
                        refreshVault()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Pick arbitrary file launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                    val fileName = "doc_${System.currentTimeMillis()}"
                    val temp = File(context.cacheDir, fileName)
                    input?.use { inp ->
                        FileOutputStream(temp).use { out -> inp.copyTo(out) }
                    }
                    vaultManager.lockFile(temp)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Added to Vault", Toast.LENGTH_SHORT).show()
                        refreshVault()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F141C) else Color(0xFFF3F5F9))
    ) {
        if (!isUnlocked) {
            // Passcode Lock Screen matching Image 6 screen 2
            PasscodeLockView(
                pin = pinInput,
                isError = pinError,
                onNumberClick = { digit ->
                    if (pinInput.length < 4) {
                        val newPin = pinInput + digit
                        pinInput = newPin
                        pinError = false
                        if (newPin.length == 4) {
                            if (vaultManager.verifyPin(newPin)) {
                                isUnlocked = true
                            } else {
                                pinError = true
                                Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                                pinInput = ""
                            }
                        }
                    }
                },
                onBackspace = {
                    if (pinInput.isNotEmpty()) {
                        pinInput = pinInput.dropLast(1)
                        pinError = false
                    }
                },
                onBack = onBack
            )
        } else {
            // Secured Storage Hub matching Image 6 screen 3
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top App Bar
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
                            .testTag("vault_back"),
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
                        text = "Locked Files",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    IconButton(onClick = { showImportBottomSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Vault",
                            tint = CyanGlow
                        )
                    }
                }

                // Unlocked status pill
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Unlocked",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Secured Storage Unlocked for this session",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Locked Categories row matching Image 6 screen 3
                Text(
                    text = "Locked Categories",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    VaultCategoryTile(
                        title = "Private Photos",
                        count = lockedCounts[FileCategory.IMAGE] ?: 0,
                        color = PeachCategory,
                        onClick = { onOpenCategoryGallery(FileCategory.IMAGE) },
                        modifier = Modifier.weight(1f)
                    )
                    VaultCategoryTile(
                        title = "Secure Video",
                        count = lockedCounts[FileCategory.VIDEO] ?: 0,
                        color = LavenderCategory,
                        onClick = { onOpenCategoryGallery(FileCategory.VIDEO) },
                        modifier = Modifier.weight(1f)
                    )
                    VaultCategoryTile(
                        title = "Documents",
                        count = lockedCounts[FileCategory.DOCUMENT] ?: 0,
                        color = SkyBlueCategory,
                        onClick = { onOpenCategoryGallery(FileCategory.DOCUMENT) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Locked Files List
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Locked Files",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "${lockedFiles.size} items",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (lockedFiles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyanGlow.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your Safe Vault is empty",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Lock files from the file browser or tap 'Add' to import private photos and documents.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
                    ) {
                        items(lockedFiles, key = { it.path }) { item ->
                            FileRowItem(
                                item = item,
                                onClick = { onOpenFile(item) },
                                onMenuClick = { selectedItemForAction = item }
                            )
                        }
                    }
                }
            }
        }

        // Upload Bottom Sheet matching Image 6 screen 5
        if (showImportBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showImportBottomSheet = false },
                containerColor = if (isDark) Color(0xFF161E2B) else Color.White,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "Add to Private Vault",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    VaultUploadOption(
                        icon = Icons.Default.PhotoLibrary,
                        title = "Import from Gallery",
                        subtitle = "Select from main device's library",
                        onClick = {
                            showImportBottomSheet = false
                            galleryLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageAndVideo
                                )
                            )
                        }
                    )

                    VaultUploadOption(
                        icon = Icons.Default.FolderOpen,
                        title = "Upload from Files",
                        subtitle = "Choose from device storage",
                        onClick = {
                            showImportBottomSheet = false
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(
                        onClick = { showImportBottomSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", fontSize = 15.sp, color = Color(0xFF94A3B8))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // Vault item action bottom sheet
        selectedItemForAction?.let { item ->
            FileActionBottomSheet(
                item = item,
                onDismiss = { selectedItemForAction = null },
                onOpen = {
                    selectedItemForAction = null
                    onOpenFile(item)
                },
                onCopy = { selectedItemForAction = null },
                onCut = { selectedItemForAction = null },
                onRename = { selectedItemForAction = null },
                onDelete = {
                    selectedItemForAction = null
                    scope.launch(Dispatchers.IO) {
                        vaultManager.deletePermanently(File(item.path))
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Deleted from vault", Toast.LENGTH_SHORT).show()
                            refreshVault()
                        }
                    }
                },
                onLockToVault = {},
                onArchive = { selectedItemForAction = null },
                onExtract = { selectedItemForAction = null },
                onShare = {
                    selectedItemForAction = null
                    shareFile(context, File(item.path))
                },
                onDetails = { selectedItemForAction = null },
                onToggleFavorite = { selectedItemForAction = null }
            )
        }
    }
}

@Composable
private fun VaultCategoryTile(
    title: String,
    count: Int,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(84.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(color)
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$count items",
                fontSize = 12.sp,
                color = Color(0xFF334155)
            )
        }
    }
}

@Composable
private fun VaultUploadOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CyanGlow.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyanGlow,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun PasscodeLockView(
    pin: String,
    isError: Boolean,
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x33FFFFFF) else Color(0x1F000000))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Lock icon with glowing gradient disc (matching Image 6 screen 2)
        Box(
            modifier = Modifier
                .size(90.dp)
                .shadow(elevation = 16.dp, shape = CircleShape, spotColor = CyanGlow.copy(alpha = 0.6f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(CyanGlow.copy(alpha = 0.4f), Color(0xFF1E293B))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = CyanGlow,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Your Secured Storage\nis Locked",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your 4-digit passcode to unlock",
            fontSize = 13.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 4 Pin Dots Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val filled = i < pin.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isError) DangerRed
                            else if (filled) CyanGlow
                            else if (isDark) Color(0xFF263245)
                            else Color(0xFFCBD5E1)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.6f))

        // Numeric Keypad 1-9, Clear, 0, Backspace (matching Image 6 screen 2)
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "⌫")
        )

        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            keys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowKeys.forEach { k ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33222D3E) else Color(0xFFE2E8F0))
                                .clickable {
                                    when (k) {
                                        "C" -> {}
                                        "⌫" -> onBackspace()
                                        else -> onNumberClick(k)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (k == "⌫") {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = k,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
