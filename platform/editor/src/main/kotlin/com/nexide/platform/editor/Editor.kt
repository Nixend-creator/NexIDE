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

package com.nexide.platform.editor

import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.util.Logger

/**
 * High-level editor that binds a [Document] to a [VirtualFile].
 * Analogous to com.intellij.openapi.editor.Editor (simplified).
 */
class Editor(
  val file: VirtualFile,
  initialText: String = ""
) {
  val document: Document = Document(initialText)

  private var myModified = false
  private var myDisposed = false

  val isModified: Boolean
    get() = myModified

  val isDisposed: Boolean
    get() = myDisposed

  init {
    document.addDocumentListener { _ ->
      myModified = true
    }
  }

  /**
   * Creates an Editor for the given VirtualFile, loading its content.
   */
  companion object {
    fun create(file: VirtualFile): Editor {
      require(file.isValid) { "Cannot create editor for invalid file: ${file.path}" }
      require(!file.isDirectory) { "Cannot create editor for directory: ${file.path}" }

      val text = try {
        file.contentsToString()
      } catch (e: Exception) {
        Logger.error("Failed to read file: ${file.path}", e)
        ""
      }
      return Editor(file, text)
    }
  }

  /**
   * Marks the document as saved (clears modified flag).
   * Actual writing to disk will be added later.
   */
  fun markSaved() {
    myModified = false
  }

  fun dispose() {
    if (myDisposed) return
    myDisposed = true
  }

  override fun toString(): String = "Editor(file=${file.path}, modified=$myModified)"
}
