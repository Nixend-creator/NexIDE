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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nexide.platform.core.build.Problem
import com.nexide.platform.core.debug.DebugSessionManager
import com.nexide.platform.core.debug.JdiConnector
import com.nexide.platform.core.run.ProcessRunner
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.ui.build.ProblemsToolWindow
import com.nexide.platform.ui.debug.BreakpointsToolWindow
import com.nexide.platform.ui.debug.DebugToolWindow
import com.nexide.platform.ui.git.GitToolWindow
import com.nexide.platform.ui.run.RunToolWindow
import com.nexide.platform.ui.search.FindInFilesPanel
import com.nexide.platform.ui.search.SearchHit
import com.nexide.platform.ui.terminal.TerminalToolWindow

/**
 * Bottom tool-window stack (Find, Problems, Debug, Run, Git, Terminal).
 */
@Composable
fun IdeBottomPanels(
  projectRoot: VirtualFile,
  projectPath: String,
  processRunner: ProcessRunner,
  debugSessionManager: DebugSessionManager,
  jdiConnector: JdiConnector,
  problems: List<Problem>,
  building: Boolean,
  findInFilesVisible: Boolean,
  problemsVisible: Boolean,
  breakpointsVisible: Boolean,
  debugPanelVisible: Boolean,
  runPanelVisible: Boolean,
  gitPanelVisible: Boolean,
  terminalVisible: Boolean,
  onFindInFilesVisibleChange: (Boolean) -> Unit,
  onProblemsVisibleChange: (Boolean) -> Unit,
  onBreakpointsVisibleChange: (Boolean) -> Unit,
  onDebugPanelVisibleChange: (Boolean) -> Unit,
  onRunPanelVisibleChange: (Boolean) -> Unit,
  onGitPanelVisibleChange: (Boolean) -> Unit,
  onTerminalVisibleChange: (Boolean) -> Unit,
  onNavigateProblem: (Problem) -> Unit,
  onRebuild: () -> Unit,
  onBreakpointNavigate: (filePath: String, line: Int) -> Unit,
  onOpenSearchHit: (SearchHit) -> Unit
) {
  if (findInFilesVisible) {
    HorizontalDivider()
    FindInFilesPanel(
      root = projectRoot,
      visible = true,
      onClose = { onFindInFilesVisibleChange(false) },
      onOpenHit = onOpenSearchHit,
      modifier = Modifier.fillMaxWidth()
    )
  }
  if (problemsVisible) {
    HorizontalDivider()
    ProblemsToolWindow(
      problems = problems,
      building = building,
      visible = true,
      onClose = { onProblemsVisibleChange(false) },
      onNavigate = onNavigateProblem,
      onRebuild = onRebuild,
      modifier = Modifier.fillMaxWidth()
    )
  }
  if (breakpointsVisible) {
    HorizontalDivider()
    BreakpointsToolWindow(
      visible = true,
      onClose = { onBreakpointsVisibleChange(false) },
      onNavigate = onBreakpointNavigate,
      modifier = Modifier.fillMaxWidth()
    )
  }
  if (debugPanelVisible) {
    HorizontalDivider()
    DebugToolWindow(
      sessionManager = debugSessionManager,
      jdiConnector = jdiConnector,
      visible = true,
      onClose = { onDebugPanelVisibleChange(false) },
      modifier = Modifier.fillMaxWidth()
    )
  }
  if (runPanelVisible) {
    HorizontalDivider()
    RunToolWindow(
      runner = processRunner,
      visible = true,
      onClose = { onRunPanelVisibleChange(false) },
      modifier = Modifier.fillMaxWidth()
    )
  }
  if (gitPanelVisible) {
    HorizontalDivider()
    GitToolWindow(
      projectRoot = projectPath,
      visible = true,
      onClose = { onGitPanelVisibleChange(false) },
      modifier = Modifier.fillMaxWidth()
    )
  }
  if (terminalVisible) {
    HorizontalDivider()
    TerminalToolWindow(
      projectRoot = projectPath,
      visible = true,
      onClose = { onTerminalVisibleChange(false) },
      modifier = Modifier.fillMaxWidth()
    )
  }
}
