package com.example.service

import android.content.Context
import com.example.model.ArchiveFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class ArchiveService(private val context: Context) {

    suspend fun createArchive(
        sourceFiles: List<File>,
        destinationZip: File,
        password: String? = null,
        format: ArchiveFormat = ArchiveFormat.ZIP
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (destinationZip.exists()) {
                destinationZip.delete()
            }

            when (format) {
                ArchiveFormat.ZIP -> {
                    val zipFile = if (password.isNullOrBlank()) {
                        ZipFile(destinationZip)
                    } else {
                        ZipFile(destinationZip, password.toCharArray())
                    }

                    val parameters = ZipParameters().apply {
                        compressionMethod = CompressionMethod.DEFLATE
                        compressionLevel = CompressionLevel.NORMAL
                        if (!password.isNullOrBlank()) {
                            isEncryptFiles = true
                            encryptionMethod = EncryptionMethod.AES
                            aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                        }
                    }

                    for (file in sourceFiles) {
                        if (file.isDirectory) {
                            zipFile.addFolder(file, parameters)
                        } else if (file.isFile) {
                            zipFile.addFile(file, parameters)
                        }
                    }
                }
                ArchiveFormat.GZIP -> {
                    // For GZIP, compress the first file
                    if (sourceFiles.isNotEmpty()) {
                        val firstFile = sourceFiles.first()
                        FileInputStream(firstFile).use { fis ->
                            GZIPOutputStream(FileOutputStream(destinationZip)).use { gzos ->
                                fis.copyTo(gzos)
                            }
                        }
                    }
                }
                ArchiveFormat.TAR -> {
                    // Create a tar archive with standard tar header format
                    FileOutputStream(destinationZip).use { fos ->
                        BufferedOutputStream(fos).use { bos ->
                            for (file in sourceFiles) {
                                writeTarFile(file, "", bos)
                            }
                            // 1024 zero bytes end of tar record
                            bos.write(ByteArray(1024))
                        }
                    }
                }
            }

            Result.success(destinationZip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun extractArchive(
        archiveFile: File,
        destinationDir: File,
        password: String? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            }

            val name = archiveFile.name.lowercase()
            when {
                name.endsWith(".zip") -> {
                    val zipFile = if (password.isNullOrBlank()) {
                        ZipFile(archiveFile)
                    } else {
                        ZipFile(archiveFile, password.toCharArray())
                    }
                    if (zipFile.isEncrypted && password.isNullOrBlank()) {
                        return@withContext Result.failure(IllegalStateException("ARCHIVE_PASSWORD_REQUIRED"))
                    }
                    zipFile.extractAll(destinationDir.absolutePath)
                }
                name.endsWith(".gz") || name.endsWith(".gzip") -> {
                    val targetName = archiveFile.name.removeSuffix(".gz").removeSuffix(".gzip")
                    val outFile = File(destinationDir, targetName)
                    GZIPInputStream(FileInputStream(archiveFile)).use { gzis ->
                        FileOutputStream(outFile).use { fos ->
                            gzis.copyTo(fos)
                        }
                    }
                }
                name.endsWith(".tar") -> {
                    extractTar(archiveFile, destinationDir)
                }
                else -> {
                    // Default attempt as ZIP
                    val zipFile = ZipFile(archiveFile)
                    zipFile.extractAll(destinationDir.absolutePath)
                }
            }
            Result.success(destinationDir)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isArchiveEncrypted(archiveFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (archiveFile.name.lowercase().endsWith(".zip")) {
                ZipFile(archiveFile).isEncrypted
            } else false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun listArchiveEntries(archiveFile: File, password: String? = null): List<String> = withContext(Dispatchers.IO) {
        try {
            if (archiveFile.name.lowercase().endsWith(".zip")) {
                val zipFile = if (password.isNullOrBlank()) ZipFile(archiveFile) else ZipFile(archiveFile, password.toCharArray())
                zipFile.fileHeaders.map { it.fileName }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeTarFile(file: File, parentPrefix: String, out: BufferedOutputStream) {
        val entryName = if (parentPrefix.isEmpty()) file.name else "$parentPrefix/${file.name}"
        if (file.isDirectory) {
            val children = file.listFiles() ?: emptyArray()
            for (child in children) {
                writeTarFile(child, entryName, out)
            }
        } else {
            val header = ByteArray(512)
            val nameBytes = entryName.toByteArray(Charsets.US_ASCII)
            System.arraycopy(nameBytes, 0, header, 0, minOf(nameBytes.size, 100))

            // File mode 0644
            val mode = "0000644\u0000".toByteArray(Charsets.US_ASCII)
            System.arraycopy(mode, 0, header, 100, mode.size)

            // Size octal
            val sizeStr = String.format("%011o", file.length()) + " "
            val sizeBytes = sizeStr.toByteArray(Charsets.US_ASCII)
            System.arraycopy(sizeBytes, 0, header, 124, sizeBytes.size)

            // Modification time octal
            val mtimeStr = String.format("%011o", file.lastModified() / 1000) + " "
            val mtimeBytes = mtimeStr.toByteArray(Charsets.US_ASCII)
            System.arraycopy(mtimeBytes, 0, header, 136, mtimeBytes.size)

            // Checksum placeholder spaces
            for (i in 148..155) header[i] = ' '.code.toByte()

            // Calculate checksum
            var sum = 0
            for (b in header) sum += (b.toInt() and 0xFF)
            val checksumStr = String.format("%06o\u0000 ", sum)
            val checksumBytes = checksumStr.toByteArray(Charsets.US_ASCII)
            System.arraycopy(checksumBytes, 0, header, 148, checksumBytes.size)

            out.write(header)

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(4096)
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }

            // Pad file data to 512-byte boundary
            val remainder = (file.length() % 512).toInt()
            if (remainder > 0) {
                out.write(ByteArray(512 - remainder))
            }
        }
    }

    private fun extractTar(tarFile: File, destDir: File) {
        FileInputStream(tarFile).use { fis ->
            BufferedInputStream(fis).use { bis ->
                val header = ByteArray(512)
                while (bis.read(header) == 512) {
                    if (header.all { it == 0.toByte() }) break

                    var nameLen = 0
                    while (nameLen < 100 && header[nameLen] != 0.toByte()) nameLen++
                    val entryName = String(header, 0, nameLen, Charsets.US_ASCII).trim()
                    if (entryName.isEmpty()) continue

                    var sizeLen = 0
                    while (sizeLen < 12 && header[124 + sizeLen] != 0.toByte() && header[124 + sizeLen] != ' '.code.toByte()) sizeLen++
                    val sizeStr = String(header, 124, sizeLen, Charsets.US_ASCII).trim()
                    val size = if (sizeStr.isNotEmpty()) sizeStr.toLongOrNull(8) ?: 0L else 0L

                    val targetFile = File(destDir, entryName)
                    targetFile.parentFile?.mkdirs()

                    if (size == 0L && entryName.endsWith("/")) {
                        targetFile.mkdirs()
                    } else {
                        FileOutputStream(targetFile).use { fos ->
                            var remaining = size
                            val buf = ByteArray(4096)
                            while (remaining > 0) {
                                val toRead = minOf(buf.size.toLong(), remaining).toInt()
                                val r = bis.read(buf, 0, toRead)
                                if (r == -1) break
                                fos.write(buf, 0, r)
                                remaining -= r
                            }
                        }
                        val pad = (512 - (size % 512)) % 512
                        if (pad > 0) bis.skip(pad)
                    }
                }
            }
        }
    }
}
