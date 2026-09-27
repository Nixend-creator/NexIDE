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

package com.nexide.platform.core.vfs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LocalFileSystemTest {

  @TempDir
  lateinit var tempDir: Path

  @Test
  fun `findFileByPath returns valid file`() {
    val testFile = tempDir.resolve("hello.txt")
    Files.writeString(testFile, "Hello NexIDE")

    val vfs = LocalFileSystem.getInstance()
    val virtualFile = vfs.findFileByPath(testFile.toAbsolutePath().toString())

    assertNotNull(virtualFile)
    assertTrue(virtualFile!!.isValid)
    assertFalse(virtualFile.isDirectory)
    assertEquals("hello.txt", virtualFile.name)
    assertEquals("Hello NexIDE", virtualFile.contentsToString())
  }

  @Test
  fun `findFileByPath returns directory with children`() {
    val child = tempDir.resolve("child.txt")
    Files.writeString(child, "child content")

    val vfs = LocalFileSystem.getInstance()
    val dir = vfs.findFileByPath(tempDir.toAbsolutePath().toString())

    assertNotNull(dir)
    assertTrue(dir!!.isDirectory)
    assertTrue(dir.isValid)

    val children = dir.getChildren()
    assertTrue(children.any { it.name == "child.txt" })
  }

  @Test
  fun `findFileByPath returns null for non-existing path`() {
    val vfs = LocalFileSystem.getInstance()
    val result = vfs.findFileByPath("/this/path/does/not/exist/nexide-test-12345")
    assertNull(result)
  }

  @Test
  fun `parent is resolved correctly`() {
    val testFile = tempDir.resolve("nested/file.txt")
    Files.createDirectories(testFile.parent)
    Files.writeString(testFile, "nested")

    val vfs = LocalFileSystem.getInstance()
    val virtualFile = vfs.findFileByPath(testFile.toAbsolutePath().toString())!!

    val parent = virtualFile.parent
    assertNotNull(parent)
    assertTrue(parent!!.isDirectory)
    assertEquals("nested", parent.name)
  }
}
