package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecycleBinManager
import com.example.data.VaultManager
import com.example.ui.components.FrostedGlassBox
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    vaultManager: VaultManager,
    recycleBinManager: RecycleBinManager,
    currentThemeSetting: String,
    onThemeSettingChanged: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    var isPinEnabled by remember { mutableStateOf(vaultManager.isPinProtectionEnabled()) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

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
                        .testTag("settings_back"),
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
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else Color(0xFF0F172A)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Appearance Section
                SettingsSectionHeader("Appearance")

                FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Default.Palette,
                            title = "Theme",
                            subtitle = when (currentThemeSetting) {
                                "dark" -> "Dark Frosted Glass"
                                "light" -> "Light Frosted Glass"
                                else -> "Auto (System Default)"
                            },
                            onClick = { showThemeDialog = true }
                        )
                    }
                }

                // Security & Vault Section
                SettingsSectionHeader("Safe Vault & Security")

                FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = CyanGlow,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Require 4-Digit Passcode",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDark) Color.White else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (isPinEnabled) "Safe Vault locked behind passcode" else "Direct access enabled",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Switch(
                                checked = isPinEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !vaultManager.hasPinSet()) {
                                        showSetPinDialog = true
                                    } else {
                                        isPinEnabled = checked
                                        vaultManager.setPinProtectionEnabled(checked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = CyanGlow
                                )
                            )
                        }

                        if (isPinEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            SettingsRow(
                                icon = Icons.Default.Security,
                                title = "Change 4-Digit Passcode",
                                subtitle = "Update your vault lock code",
                                onClick = { showSetPinDialog = true }
                            )
                        }
                    }
                }

                // Storage & Recycle Bin Section
                SettingsSectionHeader("Storage Management")

                FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Default.DeleteSweep,
                            title = "Empty Recycle Bin",
                            subtitle = "Permanently purge deleted items",
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    recycleBinManager.emptyTrash()
                                }
                                Toast.makeText(context, "Recycle Bin cleared", Toast.LENGTH_SHORT).show()
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        val hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Environment.isExternalStorageManager()
                        } else true

                        SettingsRow(
                            icon = Icons.Default.Security,
                            title = "All Files Access Permission",
                            subtitle = if (hasAllFilesAccess) "Granted (Full internal & SD access)" else "Tap to grant in Android settings",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasAllFilesAccess) {
                                    try {
                                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                        context.startActivity(intent)
                                    }
                                } else {
                                    Toast.makeText(context, "Storage permissions granted", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                // About Section
                SettingsSectionHeader("About")

                FrostedGlassBox(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(
                            icon = Icons.Default.Info,
                            title = "FileFlow",
                            subtitle = "Version 1.0 • Google Files + ZArchiver Suite",
                            onClick = {}
                        )
                    }
                }

                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Set PIN Passcode Dialog
        if (showSetPinDialog) {
            SetPinModal(
                onDismiss = { showSetPinDialog = false },
                onPinSet = { newPin ->
                    vaultManager.setPin(newPin)
                    isPinEnabled = true
                    showSetPinDialog = false
                    Toast.makeText(context, "Passcode updated", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Theme Dialog
        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        ThemeOption(label = "Auto (System Default)", isSelected = currentThemeSetting == "auto") {
                            onThemeSettingChanged("auto")
                            showThemeDialog = false
                        }
                        ThemeOption(label = "Dark Frosted Glass", isSelected = currentThemeSetting == "dark") {
                            onThemeSettingChanged("dark")
                            showThemeDialog = false
                        }
                        ThemeOption(label = "Light Frosted Glass", isSelected = currentThemeSetting == "light") {
                            onThemeSettingChanged("light")
                            showThemeDialog = false
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val isDark = isSystemInDarkTheme()
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = CyanGlow,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color.White else Color(0xFF0F172A),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ThemeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) {
            Icon(Icons.Default.DarkMode, contentDescription = null, tint = CyanGlow)
        }
    }
}

@Composable
private fun SetPinModal(
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set 4-Digit Passcode", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("Enter 4 digits") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm 4 digits") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = DangerRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length != 4) {
                        error = "PIN must be exactly 4 digits"
                    } else if (pin != confirmPin) {
                        error = "PINs do not match"
                    } else {
                        onPinSet(pin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = Color.Black)
            ) {
                Text("Save Passcode")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
