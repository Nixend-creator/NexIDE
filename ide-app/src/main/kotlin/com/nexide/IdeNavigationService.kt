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

package com.nexide

import com.nexide.platform.core.Bookmarks
import com.nexide.platform.core.build.Problem
import com.nexide.platform.core.debug.SuspendedThreadInfo
import com.nexide.platform.core.navigation.GotoDeclaration
import com.nexide.platform.core.navigation.NavigationTarget
import com.nexide.platform.core.notify.NotificationService
import com.nexide.platform.core.vfs.LocalFileSystem
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.editor.Editor
import com.nexide.platform.util.Logger
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File

/**
 * Navigation helpers: goto, usages, bookmarks, reveal, clipboard paths.
 */
object IdeNavigationService {

  fun lineNumberAt(text: String, offset: Int): Int {
    val o = offset.coerceIn(0, text.length)
    return text.substring(0, o).count { it == '\n' } + 1
  }

  fun findSourceFile(projectRoot: File, sourceName: String): VirtualFile? {
    val match = projectRoot.walkTopDown()
      .onEnter { d -> d.name !in setOf(".git", ".gradle", "build", "out", "node_modules") }
      .firstOrNull { it.isFile && it.name == sourceName }
      ?: return null
    return LocalFileSystem.getInstance().findFileByPath(match.absolutePath)
  }

  fun jumpToSource(
    info: SuspendedThreadInfo,
    projectPath: String,
    openFile: (VirtualFile, Int) -> Unit
  ) {
    val name = info.sourceName ?: return
    val line = info.lineNumber ?: return
    val file = findSourceFile(File(projectPath), name) ?: return
    openFile(file, line)
    Logger.info("Debug jump: ${file.name}:$line")
  }

  fun toggleBookmark(editor: Editor?, onChanged: () -> Unit) {
    val ed = editor ?: return
    val line = lineNumberAt(ed.document.getText(), ed.caretOffset)
    val added = Bookmarks.toggle(ed.file.path, line)
    onChanged()
    NotificationService.info(
      "Bookmark",
      if (added) "Added ${ed.file.name}:$line" else "Removed ${ed.file.name}:$line"
    )
  }

  fun goToNextBookmark(
    editor: Editor?,
    openFile: (VirtualFile, Int) -> Unit
  ) {
    val line = if (editor != null) {
      lineNumberAt(editor.document.getText(), editor.caretOffset)
    } else {
      0
    }
    val next = Bookmarks.next(editor?.file?.path, line) ?: run {
      NotificationService.warn("Bookmarks", "None")
      return
    }
    val vf = LocalFileSystem.getInstance().findFileByPath(next.filePath) ?: return
    openFile(vf, next.line)
  }

  fun copyToClipboard(text: String, label: String) {
    try {
      Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
      NotificationService.info("Copied", label)
    } catch (e: Exception) {
      NotificationService.warn("Copy failed", e.message ?: "")
    }
  }

  fun pathText(kind: String, filePath: String, fileName: String, projectRoot: String): String {
    return when (kind) {
      "name" -> fileName
      "relative" -> {
        if (filePath.startsWith(projectRoot)) {
          filePath.removePrefix(projectRoot).trimStart('/', '\\')
        } else {
          filePath
        }
      }
      else -> filePath
    }
  }

  fun ancestorPaths(filePath: String, projectRoot: String): Set<String> {
    val ancestors = mutableSetOf<String>()
    var f: File? = File(filePath).parentFile
    val rootFile = File(projectRoot)
    while (f != null) {
      ancestors.add(f.absolutePath)
      if (f.absolutePath == rootFile.absolutePath) break
      f = f.parentFile
    }
    ancestors.add(projectRoot)
    return ancestors
  }

  /**
   * @return null if handled (0 or 1 target); or Pair(titleSymbol, targets) for dialog
   */
  fun gotoDeclaration(
    editor: Editor?,
    projectPath: String,
    navigate: (NavigationTarget) -> Unit
  ): Pair<String, List<NavigationTarget>>? {
    val ed = editor ?: return null
    val symbol = GotoDeclaration.symbolAt(ed.document.getText(), ed.caretOffset)
    if (symbol == null) {
      Logger.info("Go to Declaration: no symbol under caret")
      return null
    }
    val targets = GotoDeclaration.findDeclarations(File(projectPath), symbol)
    return when {
      targets.isEmpty() -> {
        Logger.info("No declaration found for '$symbol'")
        null
      }
      targets.size == 1 -> {
        navigate(targets.first())
        null
      }
      else -> symbol to targets
    }
  }

  fun findUsages(
    editor: Editor?,
    projectPath: String
  ): Pair<String, List<NavigationTarget>>? {
    val ed = editor ?: return null
    val symbol = GotoDeclaration.symbolAt(ed.document.getText(), ed.caretOffset) ?: return null
    val targets = GotoDeclaration.findUsages(File(projectPath), symbol)
    if (targets.isEmpty()) {
      Logger.info("No usages for '$symbol'")
      return null
    }
    return symbol to targets
  }

  fun symbolForRename(editor: Editor?): String? {
    val ed = editor ?: return null
    val symbol = GotoDeclaration.symbolAt(ed.document.getText(), ed.caretOffset)
    if (symbol == null) {
      Logger.info("Rename: no symbol under caret")
    }
    return symbol
  }
}

  /**
   * Next/previous diagnostic relative to caret line in the same file, then other files.
   * @param forward true = next (F2), false = previous (Shift+F2)
   */
  fun stepProblem(
    problems: List<Problem>,
    currentFile: String?,
    currentLine: Int,
    forward: Boolean
  ): Problem? {
    val sorted = problems
      .filter { it.filePath != null && it.line != null }
      .sortedWith(
        compareBy<Problem> { it.filePath ?: "" }
          .thenBy { it.line ?: 0 }
      )
    if (sorted.isEmpty()) return null
    if (currentFile == null) return if (forward) sorted.first() else sorted.last()

    return if (forward) {
      sorted.firstOrNull { p ->
        val fp = p.filePath!!
        val ln = p.line!!
        fp > currentFile || (fp == currentFile && ln > currentLine)
      } ?: sorted.first()
    } else {
      sorted.lastOrNull { p ->
        val fp = p.filePath!!
        val ln = p.line!!
        fp < currentFile || (fp == currentFile && ln < currentLine)
      } ?: sorted.last()
    }
  }
}
