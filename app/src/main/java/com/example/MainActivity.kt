package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.example.data.FileRepository
import com.example.data.RecycleBinManager
import com.example.data.VaultManager
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.service.ArchiveService
import com.example.ui.components.FileDetailsDialog
import com.example.ui.components.FloatingBottomBar
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FileBrowserScreen
import com.example.ui.screens.PhotosGalleryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageAnalyzerScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.FileFlowTheme
import com.example.ui.viewers.AudioPlayerScreen
import com.example.ui.viewers.CodeEditorScreen
import com.example.ui.viewers.MediaViewerScreen
import com.example.ui.viewers.PdfViewerScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class AppScreen {
    object Dashboard : AppScreen()
    data class Browser(val directory: File) : AppScreen()
    data class Gallery(val category: FileCategory, val title: String) : AppScreen()
    data class MediaViewer(val item: FileItem) : AppScreen()
    data class AudioPlayer(val item: FileItem) : AppScreen()
    data class PdfViewer(val item: FileItem) : AppScreen()
    data class CodeEditor(val item: FileItem) : AppScreen()
    object Vault : AppScreen()
    object StorageAnalyzer : AppScreen()
    object Settings : AppScreen()
}

class MainActivity : ComponentActivity() {

    private lateinit var fileRepository: FileRepository
    private lateinit var archiveService: ArchiveService
    private lateinit var vaultManager: VaultManager
    private lateinit var recycleBinManager: RecycleBinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fileRepository = FileRepository(this)
        archiveService = ArchiveService(this)
        vaultManager = VaultManager(this)
        recycleBinManager = RecycleBinManager(this)

        setContent {
            val scope = rememberCoroutineScope()
            val prefs = remember { getSharedPreferences("app_theme_prefs", Context.MODE_PRIVATE) }
            var themeSetting by remember { mutableStateOf(prefs.getString("theme", "auto") ?: "auto") }

            val isDarkTheme = when (themeSetting) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            FileFlowTheme(darkTheme = isDarkTheme) {
                // Request permissions on startup
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* permissions updated */ }

                LaunchedEffect(Unit) {
                    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                        )
                    } else {
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    }
                    permissionLauncher.launch(permissions)
                }

                var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }
                var currentBottomTab by remember { mutableIntStateOf(0) }
                var fileDetailsItem by remember { mutableStateOf<FileItem?>(null) }
                var categoryPhotos by remember { mutableStateOf<List<FileItem>>(emptyList()) }

                // Router for opening files
                val openFileHandler: (FileItem) -> Unit = { item ->
                    fileRepository.recordRecentFile(item.path)
                    val ext = item.extension.lowercase()
                    when {
                        item.category == FileCategory.IMAGE || item.category == FileCategory.VIDEO -> {
                            currentScreen = AppScreen.MediaViewer(item)
                        }
                        item.category == FileCategory.AUDIO -> {
                            currentScreen = AppScreen.AudioPlayer(item)
                        }
                        ext == "pdf" -> {
                            currentScreen = AppScreen.PdfViewer(item)
                        }
                        item.category == FileCategory.CODE || ext in listOf("txt", "md", "json", "xml", "html", "htm", "css", "js", "ts", "kt", "java", "py", "sh", "sql") -> {
                            currentScreen = AppScreen.CodeEditor(item)
                        }
                        ext in listOf("zip", "7z", "tar", "gz") -> {
                            // Open directory if archive, or stay in browser
                            currentScreen = AppScreen.Browser(item.toFile().parentFile ?: fileRepository.defaultRoot)
                        }
                        else -> {
                            // Default to Code/Text viewer for preview
                            currentScreen = AppScreen.CodeEditor(item)
                        }
                    }
                }

                // Global Back Handler
                BackHandler(enabled = currentScreen !is AppScreen.Dashboard) {
                    when (currentScreen) {
                        is AppScreen.Browser -> {
                            currentScreen = AppScreen.Dashboard
                            currentBottomTab = 0
                        }
                        is AppScreen.Settings -> {
                            currentScreen = AppScreen.Dashboard
                            currentBottomTab = 0
                        }
                        is AppScreen.Gallery -> {
                            currentScreen = AppScreen.Dashboard
                            currentBottomTab = 0
                        }
                        is AppScreen.Vault -> {
                            currentScreen = AppScreen.Dashboard
                            currentBottomTab = 0
                        }
                        is AppScreen.StorageAnalyzer -> {
                            currentScreen = AppScreen.Dashboard
                            currentBottomTab = 0
                        }
                        is AppScreen.MediaViewer, is AppScreen.AudioPlayer, is AppScreen.PdfViewer, is AppScreen.CodeEditor -> {
                            currentScreen = AppScreen.Dashboard
                        }
                        else -> {
                            currentScreen = AppScreen.Dashboard
                            currentBottomTab = 0
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val screen = currentScreen) {
                            is AppScreen.Dashboard -> {
                                DashboardScreen(
                                    repository = fileRepository,
                                    vaultManager = vaultManager,
                                    recycleBinManager = recycleBinManager,
                                    onCategoryClick = { category ->
                                        scope.launch {
                                            categoryPhotos = fileRepository.getCategoryFiles(category)
                                            val title = when (category) {
                                                FileCategory.IMAGE -> "Photos"
                                                FileCategory.VIDEO -> "Videos"
                                                FileCategory.DOCUMENT -> "Documents"
                                                FileCategory.AUDIO -> "Music"
                                                else -> "Files"
                                            }
                                            currentScreen = AppScreen.Gallery(category, title)
                                        }
                                    },
                                    onOpenFile = openFileHandler,
                                    onOpenSearch = {
                                        currentScreen = AppScreen.Browser(fileRepository.defaultRoot)
                                        currentBottomTab = 1
                                    },
                                    onOpenStorageAnalyzer = {
                                        currentScreen = AppScreen.StorageAnalyzer
                                    },
                                    onOpenSecuredVault = {
                                        currentScreen = AppScreen.Vault
                                    }
                                )
                            }

                            is AppScreen.Browser -> {
                                FileBrowserScreen(
                                    initialDirectory = screen.directory,
                                    repository = fileRepository,
                                    archiveService = archiveService,
                                    vaultManager = vaultManager,
                                    recycleBinManager = recycleBinManager,
                                    onBack = {
                                        currentScreen = AppScreen.Dashboard
                                        currentBottomTab = 0
                                    },
                                    onOpenFile = openFileHandler
                                )
                            }

                            is AppScreen.Gallery -> {
                                PhotosGalleryScreen(
                                    photos = categoryPhotos,
                                    title = screen.title,
                                    onBack = {
                                        currentScreen = AppScreen.Dashboard
                                        currentBottomTab = 0
                                    },
                                    onPhotoClick = openFileHandler
                                )
                            }

                            is AppScreen.MediaViewer -> {
                                MediaViewerScreen(
                                    item = screen.item,
                                    onBack = { currentScreen = AppScreen.Dashboard },
                                    onLockToggle = {
                                        scope.launch(Dispatchers.IO) {
                                            if (screen.item.isSecured) {
                                                vaultManager.unlockFile(File(screen.item.path), fileRepository.defaultRoot)
                                            } else {
                                                vaultManager.lockFile(File(screen.item.path))
                                            }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Vault state updated", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        }
                                    },
                                    onDelete = {
                                        scope.launch(Dispatchers.IO) {
                                            recycleBinManager.moveToTrash(File(screen.item.path))
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        }
                                    },
                                    onToggleFavorite = {
                                        fileRepository.toggleFavorite(screen.item.path)
                                        Toast.makeText(this@MainActivity, "Favorite updated", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            is AppScreen.AudioPlayer -> {
                                AudioPlayerScreen(
                                    item = screen.item,
                                    onBack = { currentScreen = AppScreen.Dashboard },
                                    onLockToggle = {
                                        scope.launch(Dispatchers.IO) {
                                            if (screen.item.isSecured) {
                                                vaultManager.unlockFile(File(screen.item.path), fileRepository.defaultRoot)
                                            } else {
                                                vaultManager.lockFile(File(screen.item.path))
                                            }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Vault state updated", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        }
                                    },
                                    onDelete = {
                                        scope.launch(Dispatchers.IO) {
                                            recycleBinManager.moveToTrash(File(screen.item.path))
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        }
                                    }
                                )
                            }

                            is AppScreen.PdfViewer -> {
                                PdfViewerScreen(
                                    item = screen.item,
                                    onBack = { currentScreen = AppScreen.Dashboard },
                                    onLockToggle = {
                                        scope.launch(Dispatchers.IO) {
                                            if (screen.item.isSecured) {
                                                vaultManager.unlockFile(File(screen.item.path), fileRepository.defaultRoot)
                                            } else {
                                                vaultManager.lockFile(File(screen.item.path))
                                            }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Vault state updated", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        }
                                    },
                                    onDelete = {
                                        scope.launch(Dispatchers.IO) {
                                            recycleBinManager.moveToTrash(File(screen.item.path))
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        }
                                    },
                                    onDetails = { fileDetailsItem = screen.item }
                                )
                            }

                            is AppScreen.CodeEditor -> {
                                CodeEditorScreen(
                                    item = screen.item,
                                    onBack = { currentScreen = AppScreen.Dashboard },
                                    onLockToggle = {
                                        scope.launch(Dispatchers.IO) {
                                            if (screen.item.isSecured) {
                                                vaultManager.unlockFile(File(screen.item.path), fileRepository.defaultRoot)
                                            } else {
                                                vaultManager.lockFile(File(screen.item.path))
                                            }
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Vault state updated", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        }
                                    },
                                    onDelete = {
                                        scope.launch(Dispatchers.IO) {
                                            recycleBinManager.moveToTrash(File(screen.item.path))
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(this@MainActivity, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                                currentScreen = AppScreen.Dashboard
                                            }
                                        }
                                    },
                                    onDetails = { fileDetailsItem = screen.item }
                                )
                            }

                            is AppScreen.Vault -> {
                                VaultScreen(
                                    vaultManager = vaultManager,
                                    onBack = {
                                        currentScreen = AppScreen.Dashboard
                                        currentBottomTab = 0
                                    },
                                    onOpenFile = openFileHandler,
                                    onOpenCategoryGallery = { cat ->
                                        scope.launch {
                                            categoryPhotos = vaultManager.getLockedFiles(cat)
                                            currentScreen = AppScreen.Gallery(cat, "Private ${cat.name.lowercase().replaceFirstChar { it.uppercase() }}")
                                        }
                                    }
                                )
                            }

                            is AppScreen.StorageAnalyzer -> {
                                StorageAnalyzerScreen(
                                    repository = fileRepository,
                                    recycleBinManager = recycleBinManager,
                                    onBack = {
                                        currentScreen = AppScreen.Dashboard
                                        currentBottomTab = 0
                                    },
                                    onOpenFile = openFileHandler
                                )
                            }

                            is AppScreen.Settings -> {
                                SettingsScreen(
                                    vaultManager = vaultManager,
                                    recycleBinManager = recycleBinManager,
                                    currentThemeSetting = themeSetting,
                                    onThemeSettingChanged = { newSetting ->
                                        themeSetting = newSetting
                                        prefs.edit().putString("theme", newSetting).apply()
                                    },
                                    onBack = {
                                        currentScreen = AppScreen.Dashboard
                                        currentBottomTab = 0
                                    }
                                )
                            }
                        }

                        // Floating bottom navigation bar matching Image 1
                        val showBottomBar = currentScreen is AppScreen.Dashboard ||
                                currentScreen is AppScreen.Browser ||
                                currentScreen is AppScreen.Settings

                        if (showBottomBar) {
                            FloatingBottomBar(
                                currentTab = currentBottomTab,
                                onTabSelected = { tab ->
                                    currentBottomTab = tab
                                    currentScreen = when (tab) {
                                        0 -> AppScreen.Dashboard
                                        1 -> AppScreen.Browser(fileRepository.defaultRoot)
                                        else -> AppScreen.Settings
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                            )
                        }

                        // File details dialog
                        fileDetailsItem?.let { item ->
                            FileDetailsDialog(
                                item = item,
                                onDismiss = { fileDetailsItem = null }
                            )
                        }
                    }
                }
            }
        }
    }
}
