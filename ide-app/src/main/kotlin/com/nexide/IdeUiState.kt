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

import com.nexide.platform.core.Project
import com.nexide.platform.core.build.Problem
import com.nexide.platform.core.debug.DebugSessionManager
import com.nexide.platform.core.debug.JdiConnector
import com.nexide.platform.core.run.ProcessRunner
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.editor.Editor
import com.nexide.platform.ui.dialog.CloseConfirmResult
import com.nexide.platform.ui.search.SearchHit

/**
 * Snapshot of main-window UI state passed into [NexIdeApp].
 */
data class IdeUiState(
  val project: Project,
  val openEditors: List<Editor>,
  val activeEditor: Editor?,
  val selectedFile: VirtualFile?,
  val pendingCloseEditor: Editor?,
  val processRunner: ProcessRunner,
  val debugSessionManager: DebugSessionManager,
  val jdiConnector: JdiConnector,
  val problems: List<Problem> = emptyList(),
  val building: Boolean = false,
  val settingsRevision: Int = 0,
  val pendingGoToLine: Pair<Editor, Int>? = null,
  val darkTheme: Boolean = true,
  val splitEditor: Boolean = false,
  val runPanelVisible: Boolean = false,
  val debugPanelVisible: Boolean = false,
  val problemsVisible: Boolean = false,
  val terminalVisible: Boolean = false,
  val gitPanelVisible: Boolean = false,
  val breakpointsVisible: Boolean = false,
  val structureVisible: Boolean = false,
  val findInFilesVisible: Boolean = false,
  val projectExpandPaths: Set<String> = emptySet(),
  val pinnedPaths: Set<String> = emptySet(),
  val moduleForPath: (String) -> String? = { null },
  val gradleModules: List<String> = emptyList()
)

/**
 * Event handlers for [NexIdeApp] — keeps the composable signature small.
 */
data class IdeUiCallbacks(
  val onRunPanelVisibleChange: (Boolean) -> Unit = {},
  val onDebugPanelVisibleChange: (Boolean) -> Unit = {},
  val onProblemsVisibleChange: (Boolean) -> Unit = {},
  val onRebuild: () -> Unit = {},
  val onNavigateProblem: (Problem) -> Unit = {},
  val onTerminalVisibleChange: (Boolean) -> Unit = {},
  val onGitPanelVisibleChange: (Boolean) -> Unit = {},
  val onBreakpointsVisibleChange: (Boolean) -> Unit = {},
  val onBreakpointNavigate: (filePath: String, line: Int) -> Unit = { _, _ -> },
  val onStructureVisibleChange: (Boolean) -> Unit = {},
  val onFindInFilesVisibleChange: (Boolean) -> Unit = {},
  val onPendingGoToLineHandled: () -> Unit = {},
  val onOpenFile: (VirtualFile) -> Unit = {},
  val onOpenSearchHit: (SearchHit) -> Unit = {},
  val onNavigateToLine: (Editor, Int) -> Unit = { _, _ -> },
  val onOpenProject: () -> Unit = {},
  val onOpenRecent: (String) -> Unit = {},
  val onSelectEditor: (Editor) -> Unit = {},
  val onRequestClose: (Editor) -> Unit = {},
  val onCloseConfirm: (Editor, CloseConfirmResult) -> Unit = { _, _ -> },
  val onTogglePin: (Editor) -> Unit = {},
  val onRevealModule: (String) -> Unit = {},
  val onBuildModulePath: (String) -> Unit = {},
  val onTestModulePath: (String) -> Unit = {},
  val onOpenModuleBuildFile: (String) -> Unit = {}
)
