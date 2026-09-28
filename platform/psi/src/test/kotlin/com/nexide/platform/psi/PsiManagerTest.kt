/*
 * Copyright 2026 NexIDE Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexide.platform.psi

import com.nexide.platform.core.vfs.LocalFileSystem
import com.nexide.platform.plugin.Extensions
import com.nexide.platform.plugin.filetype.FILE_TYPE_EP
import com.nexide.platform.plugin.filetype.FileType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class PsiManagerTest {

  @TempDir
  lateinit var tempDir: Path

  @AfterEach
  fun cleanup() {
    Extensions.clearAll()
    PsiManager.getInstance().clearCache()
  }

  @Test
  fun `findFile creates plain text PSI`() {
    val testFile = tempDir.resolve("sample.txt")
    Files.writeString(testFile, "Hello PSI")

    val virtualFile = LocalFileSystem.getInstance()
      .findFileByPath(testFile.toAbsolutePath().toString())!!

    val psiManager = PsiManager.getInstance()
    val psiFile = psiManager.findFile(virtualFile)

    assertNotNull(psiFile)
    assertEquals("sample.txt", psiFile!!.name)
    assertEquals("TEXT", psiFile.language)
    assertEquals("Hello PSI", psiFile.text)
    assertTrue(psiFile.isValid)
    assertEquals(0, psiFile.textRangeStart)
    assertEquals(9, psiFile.textRangeEnd)
  }

  @Test
  fun `findFile returns null for directory`() {
    val virtualFile = LocalFileSystem.getInstance()
      .findFileByPath(tempDir.toAbsolutePath().toString())!!

    val psiFile = PsiManager.getInstance().findFile(virtualFile)
    assertNull(psiFile)
  }

  @Test
  fun `findFile uses cache`() {
    val testFile = tempDir.resolve("cached.kt")
    Files.writeString(testFile, "fun main() {}")

    val virtualFile = LocalFileSystem.getInstance()
      .findFileByPath(testFile.toAbsolutePath().toString())!!

    val psiManager = PsiManager.getInstance()
    val first = psiManager.findFile(virtualFile)
    val second = psiManager.findFile(virtualFile)

    assertSame(first, second)
  }

  @Test
  fun `findFile resolves kotlin language from file type EP`() {
    Extensions.registerExtension(FILE_TYPE_EP, object : FileType {
      override val name = "Kotlin"
      override val defaultExtension = "kt"
      override val language = "kotlin"
    })

    val testFile = tempDir.resolve("Main.kt")
    Files.writeString(testFile, "fun main() {}")

    val virtualFile = LocalFileSystem.getInstance()
      .findFileByPath(testFile.toAbsolutePath().toString())!!

    val psiFile = PsiManager.getInstance().findFile(virtualFile)
    assertNotNull(psiFile)
    assertEquals("kotlin", psiFile!!.language)
  }
}
