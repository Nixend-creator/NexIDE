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

/**
 * Represents a file or directory in the Virtual File System.
 * Analogous to com.intellij.openapi.vfs.VirtualFile.
 *
 * Instances are obtained from a [VirtualFileSystem].
 */
interface VirtualFile {
  /** Unique path within the file system (always uses '/' as separator). */
  val path: String

  /** File name (last segment of the path). */
  val name: String

  /** Parent directory, or null for the root. */
  val parent: VirtualFile?

  /** True if this entry represents a directory. */
  val isDirectory: Boolean

  /** True while the file is still present and usable. */
  val isValid: Boolean

  /** Owning file system. */
  val fileSystem: VirtualFileSystem

  /** Direct children (empty for files). Order is not guaranteed. */
  fun getChildren(): Array<VirtualFile>

  /**
   * Returns the file content as a byte array.
   * Throws if the file is a directory or cannot be read.
   */
  fun contentsToByteArray(): ByteArray

  /**
   * Returns the file content as text using UTF-8.
   */
  fun contentsToString(): String = contentsToByteArray().toString(Charsets.UTF_8)

  /** Finds a child by name, or null if not found. */
  fun findChild(name: String): VirtualFile? {
    return getChildren().firstOrNull { it.name == name }
  }

  override fun equals(other: Any?): Boolean
  override fun hashCode(): Int
}
