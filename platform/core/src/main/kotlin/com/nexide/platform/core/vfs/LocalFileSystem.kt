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

import com.nexide.platform.util.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Local file system implementation backed by java.io.File / java.nio.file.
 * Protocol: "file"
 */
class LocalFileSystem private constructor() : VirtualFileSystem {

  override val protocol: String = "file"

  private val myCache = ConcurrentHashMap<String, LocalVirtualFile>()

  override fun findFileByPath(path: String): VirtualFile? {
    val normalized = normalizePath(path)
    if (normalized.isEmpty()) return null

    val cached = myCache[normalized]
    if (cached != null && cached.isValid) {
      return cached
    }

    val ioFile = File(normalized)
    if (!ioFile.exists()) {
      myCache.remove(normalized)
      return null
    }

    val virtualFile = LocalVirtualFile(this, ioFile)
    myCache[normalized] = virtualFile
    return virtualFile
  }

  override fun refresh(file: VirtualFile) {
    if (file !is LocalVirtualFile) return
    file.refreshFromDisk()
  }

  internal fun removeFromCache(path: String) {
    myCache.remove(normalizePath(path))
  }

  companion object {
    private val ourInstance = LocalFileSystem()

    fun getInstance(): LocalFileSystem = ourInstance

    /** Normalizes path to use '/' and removes trailing slashes (except root). */
    fun normalizePath(path: String): String {
      var result = path.replace('\\', '/')
      while (result.contains("//")) {
        result = result.replace("//", "/")
      }
      if (result.length > 1 && result.endsWith('/')) {
        result = result.dropLast(1)
      }
      return result
    }
  }
}

/**
 * Concrete [VirtualFile] for the local file system.
 */
internal class LocalVirtualFile(
  override val fileSystem: LocalFileSystem,
  private var myIoFile: File
) : VirtualFile {

  private var myValid = myIoFile.exists()

  override val path: String
    get() = LocalFileSystem.normalizePath(myIoFile.absolutePath)

  override val name: String
    get() = myIoFile.name

  override val parent: VirtualFile?
    get() {
      val parentFile = myIoFile.parentFile ?: return null
      return fileSystem.findFileByPath(parentFile.absolutePath)
    }

  override val isDirectory: Boolean
    get() = myIoFile.isDirectory

  override val isValid: Boolean
    get() = myValid && myIoFile.exists()

  override fun getChildren(): Array<VirtualFile> {
    if (!isDirectory || !isValid) return emptyArray()
    val children = myIoFile.listFiles() ?: return emptyArray()
    return children.mapNotNull { child ->
      fileSystem.findFileByPath(child.absolutePath)
    }.toTypedArray()
  }

  override fun contentsToByteArray(): ByteArray {
    if (isDirectory) {
      throw IllegalStateException("Cannot read contents of a directory: $path")
    }
    if (!isValid) {
      throw IllegalStateException("File is no longer valid: $path")
    }
    return Files.readAllBytes(myIoFile.toPath())
  }

  override fun setBinaryContent(content: ByteArray) {
    if (isDirectory) {
      throw IllegalStateException("Cannot write contents to a directory: $path")
    }
    if (!isValid) {
      throw IllegalStateException("File is no longer valid: $path")
    }
    Files.write(myIoFile.toPath(), content)
  }

  internal fun refreshFromDisk() {
    myValid = myIoFile.exists()
    if (!myValid) {
      fileSystem.removeFromCache(path)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LocalVirtualFile) return false
    return path == other.path
  }

  override fun hashCode(): Int = path.hashCode()

  override fun toString(): String = "file://$path"
}
