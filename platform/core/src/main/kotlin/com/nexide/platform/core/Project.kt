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

package com.nexide.platform.core

import com.nexide.platform.core.vfs.LocalFileSystem
import com.nexide.platform.core.vfs.VirtualFile

/**
 * Represents an open project in NexIDE.
 * Analogous to com.intellij.openapi.project.Project.
 */
class Project(
  val basePath: String
) {
  private var myName: String = basePath.substringAfterLast('/').ifEmpty { "Untitled" }
  private var myIsDisposed = false
  private val myBaseDir: VirtualFile? = LocalFileSystem.getInstance().findFileByPath(basePath)

  val name: String
    get() = myName

  val isDisposed: Boolean
    get() = myIsDisposed

  /** Root directory of the project as a VirtualFile, or null if path is invalid. */
  val baseDir: VirtualFile?
    get() = myBaseDir

  fun setName(name: String) {
    myName = name
  }

  fun dispose() {
    if (myIsDisposed) return
    myIsDisposed = true
  }

  override fun toString(): String = "Project(name=$myName, path=$basePath)"
}
