package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testFileCategoryMapping() {
    assertEquals(com.example.model.FileCategory.IMAGE, com.example.model.FileCategory.fromExtension("jpg"))
    assertEquals(com.example.model.FileCategory.IMAGE, com.example.model.FileCategory.fromExtension("png"))
    assertEquals(com.example.model.FileCategory.VIDEO, com.example.model.FileCategory.fromExtension("mp4"))
    assertEquals(com.example.model.FileCategory.AUDIO, com.example.model.FileCategory.fromExtension("mp3"))
    assertEquals(com.example.model.FileCategory.DOCUMENT, com.example.model.FileCategory.fromExtension("pdf"))
    assertEquals(com.example.model.FileCategory.ARCHIVE, com.example.model.FileCategory.fromExtension("zip"))
    assertEquals(com.example.model.FileCategory.CODE, com.example.model.FileCategory.fromExtension("html"))
    assertEquals(com.example.model.FileCategory.CODE, com.example.model.FileCategory.fromExtension("kt"))
  }

  @Test
  fun testStorageStatsFormatting() {
    val stats = com.example.model.StorageStats(
      totalBytes = 128L * 1024 * 1024 * 1024,
      usedBytes = 64L * 1024 * 1024 * 1024,
      freeBytes = 64L * 1024 * 1024 * 1024,
      usedPercentage = 0.5f
    )
    assertEquals("128 GB", stats.formattedTotal)
    assertEquals("64 GB", stats.formattedUsed)
    assertEquals("64 GB", stats.formattedFree)
    assertEquals(0.5f, stats.usedPercentage, 0.001f)
  }
}
