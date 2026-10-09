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
  val copyAbsolutePath: () -> Unit,
  val copyRelativePath: () -> Unit,
  val copyFileName: () -> Unit,
  val revealInProject: () -> Unit,
  val toggleBookmark: () -> Unit,
  val nextBookmark: () -> Unit,
  val showBookmarks: () -> Unit,
  val reformat: () -> Unit,
  val optimizeImports: () -> Unit,
  val restoreLocalHistory: () -> Unit,
  val showLocalHistory: () -> Unit,
  val rename: () -> Unit,
  val build: () -> Unit,
  val buildModule: () -> Unit,
  val runTests: () -> Unit,
  val runModuleTests: () -> Unit,
  val reloadGradle: () -> Unit,
  val openSettingsGradle: () -> Unit,
  val openRootBuildGradle: () -> Unit,
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
  val openPlugins: () -> Unit,
  val zoomIn: () -> Unit,
  val zoomOut: () -> Unit,
  val zoomReset: () -> Unit
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
      Separator()
      Item("Reveal in Project View", enabled = hasEditor, onClick = actions.revealInProject)
      Separator()
      Item("Toggle Bookmark", enabled = hasEditor, onClick = actions.toggleBookmark)
      Item("Next Bookmark", onClick = actions.nextBookmark)
      Item("Show Bookmarks", onClick = actions.showBookmarks)
      Separator()
      Item("Copy Absolute Path", enabled = hasEditor, onClick = actions.copyAbsolutePath)
      Item("Copy Relative Path", enabled = hasEditor, onClick = actions.copyRelativePath)
      Item("Copy File Name", enabled = hasEditor, onClick = actions.copyFileName)
    }
    Menu("Code", mnemonic = 'C') {
      Item("Reformat Code", enabled = hasEditor, onClick = actions.reformat)
      Item("Optimize Imports", enabled = hasEditor, onClick = actions.optimizeImports)
      Item("Restore Last Local History", enabled = hasEditor, onClick = actions.restoreLocalHistory)
      Item("Show Local History…", enabled = hasEditor, onClick = actions.showLocalHistory)
    }
    Menu("Refactor", mnemonic = 'T') {
      Item("Rename…", enabled = hasEditor, onClick = actions.rename)
    }
    Menu("Build", mnemonic = 'B') {
      Item("Build Project", onClick = actions.build)
      Item("Build Module", onClick = actions.buildModule)
      Item("Run Tests", onClick = actions.runTests)
      Item("Run Module Tests", onClick = actions.runModuleTests)
      Item("Reload Gradle Project", onClick = actions.reloadGradle)
      Separator()
      Item("Open settings.gradle", onClick = actions.openSettingsGradle)
      Item("Open root build.gradle", onClick = actions.openRootBuildGradle)
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
      Separator()
      Item("Zoom In", onClick = actions.zoomIn)
      Item("Zoom Out", onClick = actions.zoomOut)
      Item("Reset Font Size", onClick = actions.zoomReset)
      Separator()
      Item("Plugins…", onClick = actions.openPlugins)
    }
  }
}
