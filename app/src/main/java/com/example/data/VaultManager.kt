package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.FileCategory
import com.example.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class VaultManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("vault_prefs", Context.MODE_PRIVATE)
    private val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }

    var isSessionUnlocked: Boolean = false

    fun isPinProtectionEnabled(): Boolean {
        return prefs.getBoolean("pin_protection_enabled", false)
    }

    fun setPinProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("pin_protection_enabled", enabled).apply()
        if (!enabled) {
            isSessionUnlocked = true
        }
    }

    fun hasPinSet(): Boolean {
        return prefs.contains("pin_hash")
    }

    fun setPin(pin: String) {
        val hash = hashPin(pin)
        prefs.edit().putString("pin_hash", hash).putBoolean("pin_protection_enabled", true).apply()
        isSessionUnlocked = true
    }

    fun verifyPin(pin: String): Boolean {
        val savedHash = prefs.getString("pin_hash", null) ?: return true
        val matches = savedHash == hashPin(pin)
        if (matches) {
            isSessionUnlocked = true
        }
        return matches
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("SALT_FILEFLOW_$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun getLockedFiles(categoryFilter: FileCategory? = null): List<FileItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<FileItem>()
        val files = vaultDir.listFiles() ?: return@withContext emptyList()
        for (f in files) {
            if (f.isFile) {
                val ext = f.extension
                val category = FileCategory.fromExtension(ext)
                if (categoryFilter == null || category == categoryFilter) {
                    list.add(
                        FileItem(
                            path = f.absolutePath,
                            name = f.name,
                            extension = ext,
                            size = f.length(),
                            lastModified = f.lastModified(),
                            isDirectory = false,
                            mimeType = getMimeType(ext),
                            category = category,
                            isSecured = true
                        )
                    )
                }
            }
        }
        list.sortedByDescending { it.lastModified }
    }

    suspend fun getLockedCounts(): Map<FileCategory, Int> = withContext(Dispatchers.IO) {
        val counts = mutableMapOf<FileCategory, Int>()
        val files = vaultDir.listFiles() ?: return@withContext emptyMap()
        for (f in files) {
            if (f.isFile) {
                val cat = FileCategory.fromExtension(f.extension)
                counts[cat] = (counts[cat] ?: 0) + 1
            }
        }
        counts
    }

    suspend fun lockFile(sourceFile: File): Result<FileItem> = withContext(Dispatchers.IO) {
        try {
            val target = File(vaultDir, sourceFile.name)
            var finalTarget = target
            var counter = 1
            while (finalTarget.exists()) {
                val base = sourceFile.nameWithoutExtension
                val ext = if (sourceFile.extension.isNotEmpty()) ".${sourceFile.extension}" else ""
                finalTarget = File(vaultDir, "${base}_$counter$ext")
                counter++
            }

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(finalTarget).use { output ->
                    input.copyTo(output)
                }
            }
            // Delete original file to securely move into vault
            sourceFile.delete()

            Result.success(
                FileItem(
                    path = finalTarget.absolutePath,
                    name = finalTarget.name,
                    extension = finalTarget.extension,
                    size = finalTarget.length(),
                    lastModified = finalTarget.lastModified(),
                    isDirectory = false,
                    mimeType = getMimeType(finalTarget.extension),
                    category = FileCategory.fromExtension(finalTarget.extension),
                    isSecured = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlockFile(vaultFile: File, destinationDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) destinationDir.mkdirs()
            val target = File(destinationDir, vaultFile.name)
            FileInputStream(vaultFile).use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            vaultFile.delete()
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(file: File): Boolean = withContext(Dispatchers.IO) {
        file.delete()
    }

    private fun getMimeType(ext: String): String {
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            ?: "application/octet-stream"
    }
}
