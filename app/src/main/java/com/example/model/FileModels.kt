package com.example.model

import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    ARCHIVE,
    CODE,
    OTHER;

    companion object {
        fun fromExtension(ext: String): FileCategory {
            return when (ext.lowercase()) {
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "heic", "heif" -> IMAGE
                "mp4", "mkv", "mov", "avi", "webm", "3gp", "flv", "wmv" -> VIDEO
                "mp3", "wav", "m4a", "flac", "aac", "ogg", "wma", "opus" -> AUDIO
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt" -> DOCUMENT
                "zip", "7z", "rar", "tar", "gz", "bz2", "xz", "iso", "apk" -> ARCHIVE
                "html", "htm", "xml", "json", "js", "ts", "kt", "java", "py", "c", "cpp", "h", "css", "sql", "sh", "md" -> CODE
                else -> OTHER
            }
        }
    }
}

data class FileItem(
    val path: String,
    val name: String,
    val extension: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val mimeType: String,
    val category: FileCategory,
    val itemCount: Int = 0,
    val isSecured: Boolean = false,
    val isFavorite: Boolean = false
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "$itemCount items"
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            val format = DecimalFormat("#,##0.#")
            return "${format.format(size / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }

    fun toFile(): File = File(path)
}

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val usedPercentage: Float
) {
    val formattedTotal: String
        get() = formatBytes(totalBytes)

    val formattedUsed: String
        get() = formatBytes(usedBytes)

    val formattedFree: String
        get() = formatBytes(freeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 GB"
            val gb = bytes.toDouble() / (1024 * 1024 * 1024)
            return if (gb >= 1.0) {
                String.format(Locale.US, "%.1f GB", gb).replace(".0 GB", " GB")
            } else {
                val mb = bytes.toDouble() / (1024 * 1024)
                String.format(Locale.US, "%.0f MB", mb)
            }
        }
    }
}

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    DATE_DESC,
    DATE_ASC,
    SIZE_DESC,
    SIZE_ASC,
    TYPE
}

enum class ViewMode {
    LIST,
    GRID
}

enum class ArchiveFormat {
    ZIP,
    TAR,
    GZIP
}
