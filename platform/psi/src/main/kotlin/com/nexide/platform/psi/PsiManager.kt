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

import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.psi.impl.PsiPlainTextFile
import com.nexide.platform.util.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages PSI files for the application.
 * Analogous to com.intellij.psi.PsiManager (simplified).
 *
 * Currently creates only plain-text PSI files.
 * Later will dispatch to language-specific parsers.
 */
class PsiManager private constructor() {

  private val myCache = ConcurrentHashMap<String, PsiFile>()

  /**
   * Returns a PSI file for the given VirtualFile.
   * Uses cache; creates a new plain-text PSI if not present.
   */
  fun findFile(virtualFile: VirtualFile): PsiFile? {
    if (!virtualFile.isValid || virtualFile.isDirectory) {
      return null
    }

    val path = virtualFile.path
    val cached = myCache[path]
    if (cached != null && cached.isValid) {
      return cached
    }

    return try {
      val text = virtualFile.contentsToString()
      val psiFile = PsiPlainTextFile(virtualFile, text)
      myCache[path] = psiFile
      psiFile
    } catch (e: Exception) {
      Logger.error("Failed to create PSI for ${virtualFile.path}", e)
      null
    }
  }

  /**
   * Drops cached PSI for the given file (e.g. after external change).
   */
  fun dropFileCache(virtualFile: VirtualFile) {
    myCache.remove(virtualFile.path)
  }

  fun clearCache() {
    myCache.clear()
  }

  companion object {
    private val ourInstance = PsiManager()

    fun getInstance(): PsiManager = ourInstance
  }
}
