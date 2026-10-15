package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.FileCategory
import com.example.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class RecycleBinManager(private val context: Context) {

    private val trashDir = File(context.filesDir, "trash_bin").apply { if (!exists()) mkdirs() }
    private val metaFile = File(trashDir, "trash_meta.json")
    private val prefs: SharedPreferences = context.getSharedPreferences("trash_prefs", Context.MODE_PRIVATE)

    suspend fun moveToTrash(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val originalPath = file.absolutePath
            val trashName = "${System.currentTimeMillis()}_${file.name}"
            val trashTarget = File(trashDir, trashName)

            if (file.isDirectory) {
                file.copyRecursively(trashTarget, overwrite = true)
                file.deleteRecursively()
            } else {
                FileInputStream(file).use { input ->
                    FileOutputStream(trashTarget).use { output ->
                        input.copyTo(output)
                    }
                }
                file.delete()
            }

            // Save metadata
            val metadata = loadMetadata()
            metadata.put(trashName, JSONObject().apply {
                put("originalPath", originalPath)
                put("deletedAt", System.currentTimeMillis())
                put("originalName", file.name)
            })
            saveMetadata(metadata)

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrashItems(): List<TrashItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TrashItem>()
        val metadata = loadMetadata()
        val files = trashDir.listFiles() ?: return@withContext emptyList()

        for (f in files) {
            if (f.name == "trash_meta.json") continue
            val info = metadata.optJSONObject(f.name)
            val originalPath = info?.optString("originalPath") ?: ""
            val originalName = info?.optString("originalName") ?: f.name
            val deletedAt = info?.optLong("deletedAt") ?: f.lastModified()

            list.add(
                TrashItem(
                    trashFile = f,
                    originalName = originalName,
                    originalPath = originalPath,
                    deletedAt = deletedAt,
                    size = f.length(),
                    isDirectory = f.isDirectory
                )
            )
        }
        list.sortedByDescending { it.deletedAt }
    }

    suspend fun restoreItem(item: TrashItem): Result<File> = withContext(Dispatchers.IO) {
        try {
            val originalTarget = File(item.originalPath)
            originalTarget.parentFile?.mkdirs()

            if (item.trashFile.isDirectory) {
                item.trashFile.copyRecursively(originalTarget, overwrite = true)
                item.trashFile.deleteRecursively()
            } else {
                FileInputStream(item.trashFile).use { input ->
                    FileOutputStream(originalTarget).use { output ->
                        input.copyTo(output)
                    }
                }
                item.trashFile.delete()
            }

            val meta = loadMetadata()
            meta.remove(item.trashFile.name)
            saveMetadata(meta)

            Result.success(originalTarget)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePermanently(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        val deleted = if (item.trashFile.isDirectory) {
            item.trashFile.deleteRecursively()
        } else {
            item.trashFile.delete()
        }
        val meta = loadMetadata()
        meta.remove(item.trashFile.name)
        saveMetadata(meta)
        deleted
    }

    suspend fun emptyTrash(): Boolean = withContext(Dispatchers.IO) {
        val files = trashDir.listFiles() ?: return@withContext true
        for (f in files) {
            if (f.name == "trash_meta.json") {
                f.delete()
            } else if (f.isDirectory) {
                f.deleteRecursively()
            } else {
                f.delete()
            }
        }
        true
    }

    private fun loadMetadata(): JSONObject {
        return if (metaFile.exists()) {
            try {
                JSONObject(metaFile.readText())
            } catch (e: Exception) {
                JSONObject()
            }
        } else {
            JSONObject()
        }
    }

    private fun saveMetadata(data: JSONObject) {
        try {
            metaFile.writeText(data.toString())
        } catch (e: Exception) {
            // ignore
        }
    }
}

data class TrashItem(
    val trashFile: File,
    val originalName: String,
    val originalPath: String,
    val deletedAt: Long,
    val size: Long,
    val isDirectory: Boolean
)
