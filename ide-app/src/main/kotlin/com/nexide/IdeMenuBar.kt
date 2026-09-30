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

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.FrameWindowScope
import com.nexide.platform.core.RecentProjects

/**
 * Callbacks for the main window menu bar — keeps Main.kt free of menu layout.
 */
data class IdeMenuActions(
  val openProject: () -> Unit,
  val openRecent: (String) -> Unit,
  val clearRecent: () -> Unit,
  val save: () -> Unit,
  val saveAll: () -> Unit,
  val closeTab: () -> Unit,
  val closeOtherTabs: () -> Unit,
  val closeAllTabs: () -> Unit,
  val openSettings: () -> Unit,
  val exit: () -> Unit,
  val gotoDeclaration: () -> Unit,
  val findUsages: () -> Unit,
  val recentFiles: () -> Unit,
  val findAction: () -> Unit,
  val searchEverywhere: () -> Unit,
  val reformat: () -> Unit,
  val restoreLocalHistory: () -> Unit,
  val rename: () -> Unit,
  val build: () -> Unit,
  val showProblems: () -> Unit,
  val runSelected: () -> Unit,
  val debugSelected: () -> Unit,
  val editRunConfigs: () -> Unit,
  val stop: () -> Unit,
  val showBreakpoints: () -> Unit,
  val showDebug: () -> Unit,
  val findInFiles: () -> Unit,
  val showStructure: () -> Unit,
  val showGit: () -> Unit,
  val showTerminal: () -> Unit,
  val showRun: () -> Unit,
  val toggleSoftWrap: () -> Unit,
  val toggleDarkTheme: () -> Unit,
  val toggleSplit: () -> Unit,
  val openPlugins: () -> Unit
)

@Composable
fun FrameWindowScope.IdeMenuBar(
  hasEditor: Boolean,
  hasModified: Boolean,
  tabCount: Int,
  selectedRunName: String?,
  softWrap: Boolean,
  darkTheme: Boolean,
  splitEditor: Boolean,
  currentProjectPath: String,
  actions: IdeMenuActions
) {
  MenuBar {
    Menu("File", mnemonic = 'F') {
      Item("Open Project…", onClick = actions.openProject)
      val recent = RecentProjects.list().filter { it != currentProjectPath }
      if (recent.isNotEmpty()) {
        Menu("Open Recent", mnemonic = 'R') {
          recent.forEach { path ->
            Item(RecentProjects.displayName(path), onClick = { actions.openRecent(path) })
          }
          Separator()
          Item("Clear List", onClick = actions.clearRecent)
        }
      }
      Item("Save", enabled = hasEditor, onClick = actions.save)
      Item("Save All", enabled = hasModified, onClick = actions.saveAll)
      Separator()
      Item("Close Tab", enabled = hasEditor, onClick = actions.closeTab)
      Item("Close Other Tabs", enabled = tabCount > 1 && hasEditor, onClick = actions.closeOtherTabs)
      Item("Close All Tabs", enabled = tabCount > 0, onClick = actions.closeAllTabs)
      Separator()
      Item("Settings…", onClick = actions.openSettings)
      Separator()
      Item("Exit", onClick = actions.exit)
    }
    Menu("Navigate", mnemonic = 'N') {
      Item("Search Everywhere", onClick = actions.searchEverywhere)
      Item("Find Action…", onClick = actions.findAction)
      Item("Recent Files", onClick = actions.recentFiles)
      Item("Go to Declaration", enabled = hasEditor, onClick = actions.gotoDeclaration)
      Item("Find Usages", enabled = hasEditor, onClick = actions.findUsages)
    }
    Menu("Code", mnemonic = 'C') {
      Item("Reformat Code", enabled = hasEditor, onClick = actions.reformat)
      Item("Restore Last Local History", enabled = hasEditor, onClick = actions.restoreLocalHistory)
    }
    Menu("Refactor", mnemonic = 'T') {
      Item("Rename…", enabled = hasEditor, onClick = actions.rename)
    }
    Menu("Build", mnemonic = 'B') {
      Item("Build Project", onClick = actions.build)
      Item("Show Problems", onClick = actions.showProblems)
    }
    Menu("Run", mnemonic = 'R') {
      Item(
        "Run '${selectedRunName ?: "…"}'",
        enabled = selectedRunName != null,
        onClick = actions.runSelected
      )
      Item(
        "Debug '${selectedRunName ?: "…"}'",
        enabled = selectedRunName != null,
        onClick = actions.debugSelected
      )
      Item("Edit Configurations…", onClick = actions.editRunConfigs)
      Separator()
      Item("View Breakpoints", onClick = actions.showBreakpoints)
      Item("Show Debug Tool Window", onClick = actions.showDebug)
      Separator()
      Item("Stop", onClick = actions.stop)
    }
    Menu("View", mnemonic = 'V') {
      Item("Find in Files", onClick = actions.findInFiles)
      Item("Structure", onClick = actions.showStructure)
      Item("Git", onClick = actions.showGit)
      Item("Terminal", onClick = actions.showTerminal)
      Item("Run Tool Window", onClick = actions.showRun)
      Item("Problems", onClick = actions.showProblems)
      Separator()
      Item(if (softWrap) "Soft Wrap ✓" else "Soft Wrap", onClick = actions.toggleSoftWrap)
      Item(if (darkTheme) "Dark Theme ✓" else "Dark Theme", onClick = actions.toggleDarkTheme)
      Item(if (splitEditor) "Split Editor ✓" else "Split Editor", onClick = actions.toggleSplit)
      Item("Plugins…", onClick = actions.openPlugins)
    }
  }
}
