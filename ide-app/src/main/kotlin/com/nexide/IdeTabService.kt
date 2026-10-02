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

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.nexide.platform.core.Application
import com.nexide.platform.core.Project
import com.nexide.platform.core.RecentFiles
import com.nexide.platform.core.RecentProjects
import com.nexide.platform.core.index.SymbolIndex
import com.nexide.platform.core.notify.NotificationService
import com.nexide.platform.core.run.RunConfigurationManager
import com.nexide.platform.core.state.IdeStateStore
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.editor.Editor
import com.nexide.platform.util.Logger
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

/**
 * Tab and project-open operations extracted from Main.
 */
object IdeTabService {

  fun openFile(
    openEditors: SnapshotStateList<Editor>,
    file: VirtualFile,
    line: Int?,
    setActive: (Editor) -> Unit,
    setSelectedFile: (VirtualFile) -> Unit,
    setPendingGoToLine: (Pair<Editor, Int>?) -> Unit
  ): Editor {
    RecentFiles.add(file.path)
    setSelectedFile(file)
    val existing = openEditors.firstOrNull { it.file.path == file.path }
    val editor = if (existing != null) {
      setActive(existing)
      existing
    } else {
      val created = Editor.create(file)
      openEditors.add(created)
      setActive(created)
      created
    }
    if (line != null && line > 0) {
      setPendingGoToLine(editor to line)
    }
    return editor
  }

  fun doCloseEditor(
    openEditors: SnapshotStateList<Editor>,
    editor: Editor,
    activeEditor: Editor?,
    setActive: (Editor?) -> Unit,
    setSelectedFile: (VirtualFile?) -> Unit
  ) {
    val index = openEditors.indexOf(editor)
    openEditors.remove(editor)
    editor.dispose()
    if (activeEditor === editor) {
      val next = when {
        openEditors.isEmpty() -> null
        index < openEditors.size -> openEditors[index]
        else -> openEditors.lastOrNull()
      }
      setActive(next)
      setSelectedFile(next?.file)
    }
  }

  fun requestCloseEditor(
    editor: Editor,
    setPendingClose: (Editor?) -> Unit,
    doClose: (Editor) -> Unit
  ) {
    if (editor.isModified) {
      setPendingClose(editor)
    } else {
      doClose(editor)
    }
  }

  fun closeOtherTabs(
    openEditors: SnapshotStateList<Editor>,
    activeEditor: Editor?,
    doClose: (Editor) -> Unit
  ) {
    val keep = activeEditor ?: return
    openEditors.toList().filter { it !== keep }.forEach { ed ->
      if (ed.isModified) {
        ed.save()
        Logger.info("Saved before close: ${ed.file.name}")
      }
      doClose(ed)
    }
  }

  fun closeAllEditors(
    openEditors: SnapshotStateList<Editor>,
    pinnedPaths: Set<String>,
    doClose: (Editor) -> Unit
  ) {
    openEditors.toList().forEach { ed ->
      if (ed.file.path in pinnedPaths) return@forEach
      if (ed.isModified) {
        ed.save()
        Logger.info("Saved before close: ${ed.file.name}")
      }
      doClose(ed)
    }
  }

  fun openProjectFromPath(
    path: String,
    application: Application,
    runConfigManager: RunConfigurationManager,
    closeAll: () -> Unit,
    setProject: (Project) -> Unit,
    clearEditors: () -> Unit
  ) {
    closeAll()
    clearEditors()
    setProject(application.openProject(path))
    RecentProjects.add(path)
    runConfigManager.clear()
    runConfigManager.ensureDefaults(File(path))
    SymbolIndex.invalidate()
    try {
      SymbolIndex.ensureIndexed(File(path))
    } catch (_: Exception) {
    }
    IdeStateStore.lastProjectPath = path
    IdeStateStore.save()
    NotificationService.info("Project opened", path)
    Logger.info("Opened project: $path")
  }

  fun chooseAndOpenProject(currentProjectPath: String, openPath: (String) -> Unit) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Exception) {
    }
    val chooser = JFileChooser().apply {
      fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
      dialogTitle = "Open Project"
      currentDirectory = File(currentProjectPath)
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
      openPath(chooser.selectedFile.absolutePath)
    }
  }
}
