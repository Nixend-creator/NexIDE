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
import com.nexide.platform.plugin.filetype.FileTypeRegistry
import com.nexide.platform.psi.impl.PsiPlainTextFile
import com.nexide.platform.util.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages PSI files for the application.
 * Analogous to com.intellij.psi.PsiManager (simplified).
 *
 * Resolves language via [FileTypeRegistry] and dispatches to
 * [LANGUAGE_PARSER_EP] when a language-specific parser is registered.
 */
class PsiManager private constructor() {

  private val myCache = ConcurrentHashMap<String, PsiFile>()

  /**
   * Returns a PSI file for the given VirtualFile.
   * Uses cache; creates PSI via language parser or plain-text fallback.
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
      val language = FileTypeRegistry.getLanguageByFileName(virtualFile.name)
      val psiFile = createPsiFile(virtualFile, text, language)
      myCache[path] = psiFile
      psiFile
    } catch (e: Exception) {
      Logger.error("Failed to create PSI for ${virtualFile.path}", e)
      null
    }
  }

  private fun createPsiFile(virtualFile: VirtualFile, text: String, language: String): PsiFile {
    // Try language-specific parsers first
    for (parser in LANGUAGE_PARSER_EP.extensionList) {
      if (parser.language.equals(language, ignoreCase = true)) {
        val parsed = parser.parse(virtualFile, text)
        if (parsed != null) {
          return parsed
        }
      }
    }
    // Fallback: plain text with correct language id
    return PsiPlainTextFile(virtualFile, text, language)
  }

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
