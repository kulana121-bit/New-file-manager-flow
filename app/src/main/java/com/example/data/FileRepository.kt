package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.SortOrder
import com.example.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class FileRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("file_flow_prefs", Context.MODE_PRIVATE)

    val defaultRoot: File
        get() {
            val ext = Environment.getExternalStorageDirectory()
            return if (ext.exists() && ext.canRead()) ext else context.filesDir
        }

    fun getStorageStats(): StorageStats {
        return try {
            val root = Environment.getExternalStorageDirectory()
            val stat = StatFs(root.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes
            val percentage = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes) else 0.5f

            StorageStats(
                totalBytes = totalBytes,
                usedBytes = usedBytes,
                freeBytes = freeBytes,
                usedPercentage = percentage
            )
        } catch (e: Exception) {
            val total = 128L * 1024 * 1024 * 1024
            val used = 64L * 1024 * 1024 * 1024
            StorageStats(
                totalBytes = total,
                usedBytes = used,
                freeBytes = total - used,
                usedPercentage = 0.5f
            )
        }
    }

    suspend fun listFiles(
        directory: File,
        showHidden: Boolean = false,
        sortOrder: SortOrder = SortOrder.NAME_ASC
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val rawFiles = directory.listFiles() ?: return@withContext emptyList()
        val favorites = getFavoritePaths()

        val items = rawFiles
            .filter { showHidden || !it.name.startsWith(".") }
            .map { file ->
                val ext = file.extension
                val isDir = file.isDirectory
                val itemCount = if (isDir) (file.listFiles()?.size ?: 0) else 0
                val category = if (isDir) FileCategory.OTHER else FileCategory.fromExtension(ext)
                val mime = if (isDir) "resource/folder" else getMimeType(ext)

                FileItem(
                    path = file.absolutePath,
                    name = file.name,
                    extension = ext,
                    size = if (isDir) 0L else file.length(),
                    lastModified = file.lastModified(),
                    isDirectory = isDir,
                    mimeType = mime,
                    category = category,
                    itemCount = itemCount,
                    isFavorite = favorites.contains(file.absolutePath)
                )
            }

        sortItems(items, sortOrder)
    }

    suspend fun getCategoryFiles(category: FileCategory): List<FileItem> = withContext(Dispatchers.IO) {
        val root = defaultRoot
        val result = mutableListOf<FileItem>()
        val favorites = getFavoritePaths()

        fun scan(dir: File, depth: Int = 0) {
            if (depth > 4 || result.size >= 300) return
            val children = dir.listFiles() ?: return
            for (f in children) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory) {
                    // Skip hidden or system heavy folders
                    if (!f.name.equals("Android", ignoreCase = true)) {
                        scan(f, depth + 1)
                    }
                } else if (f.isFile) {
                    val cat = FileCategory.fromExtension(f.extension)
                    if (cat == category) {
                        result.add(
                            FileItem(
                                path = f.absolutePath,
                                name = f.name,
                                extension = f.extension,
                                size = f.length(),
                                lastModified = f.lastModified(),
                                isDirectory = false,
                                mimeType = getMimeType(f.extension),
                                category = cat,
                                isFavorite = favorites.contains(f.absolutePath)
                            )
                        )
                    }
                }
            }
        }

        // Targeted search in common standard folders first for speed
        val targetFolders = when (category) {
            FileCategory.IMAGE -> listOf(
                File(root, "DCIM"),
                File(root, "Pictures"),
                File(root, "Download")
            )
            FileCategory.VIDEO -> listOf(
                File(root, "DCIM"),
                File(root, "Movies"),
                File(root, "Download")
            )
            FileCategory.AUDIO -> listOf(
                File(root, "Music"),
                File(root, "Podcasts"),
                File(root, "Download")
            )
            FileCategory.DOCUMENT -> listOf(
                File(root, "Documents"),
                File(root, "Download")
            )
            else -> listOf(File(root, "Download"), root)
        }

        for (dir in targetFolders) {
            if (dir.exists() && dir.canRead()) {
                scan(dir, 0)
            }
        }

        if (result.isEmpty()) {
            scan(root, 0)
        }

        result.sortedByDescending { it.lastModified }
    }

    suspend fun searchFiles(query: String, startDir: File = defaultRoot): List<FileItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val result = mutableListOf<FileItem>()
        val lowerQuery = query.lowercase()

        fun searchDir(dir: File, depth: Int = 0) {
            if (depth > 4 || result.size >= 100) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.name.lowercase().contains(lowerQuery)) {
                    result.add(
                        FileItem(
                            path = f.absolutePath,
                            name = f.name,
                            extension = f.extension,
                            size = if (f.isDirectory) 0 else f.length(),
                            lastModified = f.lastModified(),
                            isDirectory = f.isDirectory,
                            mimeType = if (f.isDirectory) "resource/folder" else getMimeType(f.extension),
                            category = if (f.isDirectory) FileCategory.OTHER else FileCategory.fromExtension(f.extension),
                            itemCount = if (f.isDirectory) (f.listFiles()?.size ?: 0) else 0
                        )
                    )
                }
                if (f.isDirectory && !f.name.equals("Android", ignoreCase = true)) {
                    searchDir(f, depth + 1)
                }
            }
        }

        searchDir(startDir, 0)
        result.sortedByDescending { it.lastModified }
    }

    suspend fun getRecentFiles(limit: Int = 10): List<FileItem> = withContext(Dispatchers.IO) {
        val paths = getRecentPaths()
        val list = mutableListOf<FileItem>()
        val favorites = getFavoritePaths()

        for (p in paths) {
            val f = File(p)
            if (f.exists() && f.isFile) {
                list.add(
                    FileItem(
                        path = f.absolutePath,
                        name = f.name,
                        extension = f.extension,
                        size = f.length(),
                        lastModified = f.lastModified(),
                        isDirectory = false,
                        mimeType = getMimeType(f.extension),
                        category = FileCategory.fromExtension(f.extension),
                        isFavorite = favorites.contains(f.absolutePath)
                    )
                )
            }
        }

        // If user hasn't opened many recent files yet, populate from Downloads/DCIM
        if (list.size < 3) {
            val downloadDir = File(defaultRoot, "Download")
            val dcimDir = File(defaultRoot, "DCIM")
            val sampleDirs = listOf(downloadDir, dcimDir, defaultRoot)
            for (d in sampleDirs) {
                val files = d.listFiles()?.filter { it.isFile && !it.name.startsWith(".") } ?: emptyList()
                for (f in files) {
                    if (list.none { it.path == f.absolutePath }) {
                        list.add(
                            FileItem(
                                path = f.absolutePath,
                                name = f.name,
                                extension = f.extension,
                                size = f.length(),
                                lastModified = f.lastModified(),
                                isDirectory = false,
                                mimeType = getMimeType(f.extension),
                                category = FileCategory.fromExtension(f.extension),
                                isFavorite = favorites.contains(f.absolutePath)
                            )
                        )
                    }
                    if (list.size >= limit) break
                }
                if (list.size >= limit) break
            }
        }

        list.sortedByDescending { it.lastModified }.take(limit)
    }

    fun recordRecentFile(path: String) {
        val recents = getRecentPaths().toMutableList()
        recents.remove(path)
        recents.add(0, path)
        val limited = recents.take(30)
        val arr = JSONArray(limited)
        prefs.edit().putString("recent_files_json", arr.toString()).apply()
    }

    private fun getRecentPaths(): List<String> {
        val raw = prefs.getString("recent_files_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun toggleFavorite(path: String): Boolean {
        val favs = getFavoritePaths().toMutableSet()
        val isNowFav = if (favs.contains(path)) {
            favs.remove(path)
            false
        } else {
            favs.add(path)
            true
        }
        val arr = JSONArray(favs.toList())
        prefs.edit().putString("favorite_paths_json", arr.toString()).apply()
        return isNowFav
    }

    fun getFavoritePaths(): Set<String> {
        val raw = prefs.getString("favorite_paths_json", null) ?: return emptySet()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun getFavoriteFiles(): List<FileItem> = withContext(Dispatchers.IO) {
        val favs = getFavoritePaths()
        favs.mapNotNull { path ->
            val f = File(path)
            if (f.exists()) {
                FileItem(
                    path = f.absolutePath,
                    name = f.name,
                    extension = f.extension,
                    size = if (f.isDirectory) 0 else f.length(),
                    lastModified = f.lastModified(),
                    isDirectory = f.isDirectory,
                    mimeType = if (f.isDirectory) "resource/folder" else getMimeType(f.extension),
                    category = if (f.isDirectory) FileCategory.OTHER else FileCategory.fromExtension(f.extension),
                    itemCount = if (f.isDirectory) (f.listFiles()?.size ?: 0) else 0,
                    isFavorite = true
                )
            } else null
        }
    }

    suspend fun findLargeFiles(thresholdBytes: Long = 20L * 1024 * 1024): List<FileItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<FileItem>()
        fun scan(dir: File, depth: Int = 0) {
            if (depth > 5 || result.size >= 50) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory && !f.name.equals("Android", ignoreCase = true)) {
                    scan(f, depth + 1)
                } else if (f.isFile && f.length() >= thresholdBytes) {
                    result.add(
                        FileItem(
                            path = f.absolutePath,
                            name = f.name,
                            extension = f.extension,
                            size = f.length(),
                            lastModified = f.lastModified(),
                            isDirectory = false,
                            mimeType = getMimeType(f.extension),
                            category = FileCategory.fromExtension(f.extension)
                        )
                    )
                }
            }
        }
        scan(defaultRoot, 0)
        result.sortedByDescending { it.size }
    }

    suspend fun findDuplicateFiles(): List<List<FileItem>> = withContext(Dispatchers.IO) {
        val sizeMap = mutableMapOf<Long, MutableList<File>>()
        fun scan(dir: File, depth: Int = 0) {
            if (depth > 4) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.name.startsWith(".")) continue
                if (f.isDirectory && !f.name.equals("Android", ignoreCase = true)) {
                    scan(f, depth + 1)
                } else if (f.isFile && f.length() > 1024) {
                    val list = sizeMap.getOrPut(f.length()) { mutableListOf() }
                    list.add(f)
                }
            }
        }
        scan(defaultRoot, 0)

        val potentialDuplicates = sizeMap.filter { it.value.size > 1 }
        val duplicates = mutableListOf<List<FileItem>>()

        for ((_, fileList) in potentialDuplicates) {
            val hashMap = mutableMapOf<String, MutableList<FileItem>>()
            for (f in fileList) {
                val hash = getFileChecksum(f)
                val item = FileItem(
                    path = f.absolutePath,
                    name = f.name,
                    extension = f.extension,
                    size = f.length(),
                    lastModified = f.lastModified(),
                    isDirectory = false,
                    mimeType = getMimeType(f.extension),
                    category = FileCategory.fromExtension(f.extension)
                )
                hashMap.getOrPut(hash) { mutableListOf() }.add(item)
            }
            for ((_, matches) in hashMap) {
                if (matches.size > 1) {
                    duplicates.add(matches)
                }
            }
        }

        duplicates
    }

    private fun getFileChecksum(file: File): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(4096)
                val read = fis.read(buffer)
                if (read > 0) md.update(buffer, 0, read)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            file.name + file.length()
        }
    }

    suspend fun createFolder(parentDir: File, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val newDir = File(parentDir, folderName)
            if (newDir.exists()) {
                Result.failure(IllegalArgumentException("Folder already exists"))
            } else if (newDir.mkdirs()) {
                Result.success(newDir)
            } else {
                Result.failure(IllegalStateException("Failed to create folder"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFile(parentDir: File, fileName: String, content: String = ""): Result<File> = withContext(Dispatchers.IO) {
        try {
            val newFile = File(parentDir, fileName)
            if (newFile.exists()) {
                Result.failure(IllegalArgumentException("File already exists"))
            } else {
                newFile.writeText(content)
                Result.success(newFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val parent = file.parentFile ?: return@withContext Result.failure(IllegalStateException("No parent directory"))
            val target = File(parent, newName)
            if (target.exists()) {
                Result.failure(IllegalArgumentException("A file with this name already exists"))
            } else if (file.renameTo(target)) {
                Result.success(target)
            } else {
                Result.failure(IllegalStateException("Rename failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFiles(sources: List<File>, targetDir: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            for (src in sources) {
                val dest = File(targetDir, src.name)
                if (src.isDirectory) {
                    src.copyRecursively(dest, overwrite = true)
                } else {
                    FileInputStream(src).use { inStream ->
                        FileOutputStream(dest).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                }
                count++
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFiles(sources: List<File>, targetDir: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            for (src in sources) {
                val dest = File(targetDir, src.name)
                if (src.renameTo(dest)) {
                    count++
                } else {
                    // Fallback copy + delete
                    if (src.isDirectory) {
                        src.copyRecursively(dest, overwrite = true)
                        src.deleteRecursively()
                    } else {
                        FileInputStream(src).use { inStream ->
                            FileOutputStream(dest).use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        src.delete()
                    }
                    count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sortItems(items: List<FileItem>, order: SortOrder): List<FileItem> {
        val dirs = items.filter { it.isDirectory }
        val files = items.filter { !it.isDirectory }

        fun sortList(list: List<FileItem>): List<FileItem> {
            return when (order) {
                SortOrder.NAME_ASC -> list.sortedBy { it.name.lowercase() }
                SortOrder.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
                SortOrder.DATE_DESC -> list.sortedByDescending { it.lastModified }
                SortOrder.DATE_ASC -> list.sortedBy { it.lastModified }
                SortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
                SortOrder.SIZE_ASC -> list.sortedBy { it.size }
                SortOrder.TYPE -> list.sortedBy { it.extension.lowercase() }
            }
        }

        // Directories always appear on top, sorted according to the order
        return sortList(dirs) + sortList(files)
    }

    private fun getMimeType(ext: String): String {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            ?: "application/octet-stream"
    }
}
