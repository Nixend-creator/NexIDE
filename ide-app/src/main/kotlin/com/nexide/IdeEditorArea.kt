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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexide.platform.core.build.Problem
import com.nexide.platform.editor.Editor
import com.nexide.platform.editor.ui.EditorView
import com.nexide.platform.ui.tabs.EditorTabs
import com.nexide.platform.ui.welcome.WelcomeScreen

/**
 * Center editor region: tabs + single/split editor or welcome screen.
 */
@Composable
fun IdeEditorArea(
  openEditors: List<Editor>,
  activeEditor: Editor?,
  splitEditor: Boolean,
  projectPath: String,
  settingsRevision: Int,
  problems: List<Problem>,
  pendingGoToLine: Pair<Editor, Int>?,
  onPendingGoToLineHandled: () -> Unit,
  onSelectEditor: (Editor) -> Unit,
  onRequestClose: (Editor) -> Unit,
  onOpenProject: () -> Unit,
  onOpenRecent: (String) -> Unit,
  pinnedPaths: Set<String> = emptySet(),
  onTogglePin: (Editor) -> Unit = {},
  modifier: Modifier = Modifier
) {
  fun problemLinesFor(ed: Editor): Set<Int> =
    problems
      .filter { it.filePath == ed.file.path && it.line != null }
      .mapNotNull { it.line }
      .toSet()

  Column(modifier = modifier.fillMaxHeight()) {
    EditorTabs(
      editors = openEditors,
      activeEditor = activeEditor,
      onSelect = onSelectEditor,
      onClose = onRequestClose,
      pinnedPaths = pinnedPaths,
      onTogglePin = onTogglePin
    )

    val editor = activeEditor
    if (editor != null && splitEditor && openEditors.size > 1) {
      val secondary = openEditors.firstOrNull { it !== editor } ?: editor
      Row(Modifier.weight(1f).fillMaxSize()) {
        EditorView(
          editor = editor,
          modifier = Modifier.weight(1f).fillMaxSize(),
          projectRootPath = projectPath,
          settingsRevision = settingsRevision,
          problemLines = problemLinesFor(editor)
        )
        VerticalDivider()
        EditorView(
          editor = secondary,
          modifier = Modifier.weight(1f).fillMaxSize(),
          projectRootPath = projectPath,
          settingsRevision = settingsRevision,
          problemLines = problemLinesFor(secondary)
        )
      }
    } else if (editor != null) {
      val gotoLine = pendingGoToLine?.takeIf { it.first === editor }?.second
      EditorView(
        editor = editor,
        modifier = Modifier.weight(1f).fillMaxSize(),
        gotoLineRequest = gotoLine,
        onGotoLineHandled = onPendingGoToLineHandled,
        projectRootPath = projectPath,
        settingsRevision = settingsRevision,
        problemLines = problemLinesFor(editor)
      )
    } else {
      WelcomeScreen(
        currentProjectPath = projectPath,
        onOpenProject = onOpenProject,
        onOpenRecent = onOpenRecent,
        modifier = Modifier.weight(1f).fillMaxSize()
      )
    }
  }
}
