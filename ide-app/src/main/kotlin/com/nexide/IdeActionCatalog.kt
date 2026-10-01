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

import com.nexide.platform.ui.navigation.PaletteAction

/**
 * Snapshot of UI flags used to enable/disable actions.
 */
data class IdeActionState(
  val hasEditor: Boolean,
  val hasModified: Boolean,
  val hasTabs: Boolean,
  val hasRunConfig: Boolean
)

/**
 * Handlers for all global IDE actions (menu, palette, future keymap).
 */
class IdeActionHandlers(
  val openProject: () -> Unit,
  val save: () -> Unit,
  val saveAll: () -> Unit,
  val closeTab: () -> Unit,
  val closeAllTabs: () -> Unit,
  val recentFiles: () -> Unit,
  val searchEverywhere: () -> Unit,
  val findAction: () -> Unit,
  val findInFiles: () -> Unit,
  val gotoDeclaration: () -> Unit,
  val findUsages: () -> Unit,
  val rename: () -> Unit,
  val reformat: () -> Unit,
  val build: () -> Unit,
  val runTests: () -> Unit,
  val showProblems: () -> Unit,
  val runSelected: () -> Unit,
  val debugSelected: () -> Unit,
  val stop: () -> Unit,
  val showGit: () -> Unit,
  val showTerminal: () -> Unit,
  val showStructure: () -> Unit,
  val showDebug: () -> Unit,
  val showBreakpoints: () -> Unit,
  val openSettings: () -> Unit,
  val openPlugins: () -> Unit,
  val toggleTheme: () -> Unit,
  val toggleSplit: () -> Unit,
  val toggleSoftWrap: () -> Unit,
  val zoomIn: () -> Unit,
  val zoomOut: () -> Unit,
  val zoomReset: () -> Unit,
  val copyAbsolutePath: () -> Unit,
  val copyRelativePath: () -> Unit,
  val copyFileName: () -> Unit,
  val revealInProject: () -> Unit,
  val toggleBookmark: () -> Unit,
  val nextBookmark: () -> Unit,
  val showBookmarks: () -> Unit
)

/**
 * Single catalog of IDE actions — Command Palette (and later keymap) reads from here.
 */
object IdeActionCatalog {

  fun paletteActions(state: IdeActionState, h: IdeActionHandlers): List<PaletteAction> {
    fun action(
      id: String,
      title: String,
      shortcut: String = "",
      enabled: Boolean = true,
      run: () -> Unit
    ) = PaletteAction(id, title, shortcut, enabled, run)

    return listOf(
      action("save", "Save", "Ctrl+S", state.hasEditor, h.save),
      action("save.all", "Save All", "Ctrl+Shift+S", state.hasModified, h.saveAll),
      action("close.tab", "Close Tab", "Ctrl+W", state.hasEditor, h.closeTab),
      action("close.all", "Close All Tabs", "Ctrl+Shift+W", state.hasTabs, h.closeAllTabs),
      action("open.project", "Open Project…", "Ctrl+O", run = h.openProject),
      action("recent.files", "Recent Files", "Ctrl+E", run = h.recentFiles),
      action("search.everywhere", "Search Everywhere", "Ctrl+N", run = h.searchEverywhere),
      action("find.action", "Find Action…", "Ctrl+Shift+A", run = h.findAction),
      action("find.files", "Find in Files", "Ctrl+Shift+F", run = h.findInFiles),
      action("goto.decl", "Go to Declaration", "Ctrl+B", state.hasEditor, h.gotoDeclaration),
      action("find.usages", "Find Usages", "Alt+F7", state.hasEditor, h.findUsages),
      action("rename", "Rename…", "Shift+F6", state.hasEditor, h.rename),
      action("reformat", "Reformat Code", "Ctrl+Alt+L", state.hasEditor, h.reformat),
      action("build", "Build Project", run = h.build),
      action("run.tests", "Run Tests", run = h.runTests),
      action("problems", "Show Problems", run = h.showProblems),
      action("run", "Run Selected Configuration", enabled = state.hasRunConfig, run = h.runSelected),
      action("debug", "Debug Selected Configuration", enabled = state.hasRunConfig, run = h.debugSelected),
      action("stop", "Stop", run = h.stop),
      action("git", "Git Tool Window", run = h.showGit),
      action("terminal", "Terminal", run = h.showTerminal),
      action("structure", "Structure", run = h.showStructure),
      action("debug.panel", "Debug Tool Window", run = h.showDebug),
      action("breakpoints", "View Breakpoints", run = h.showBreakpoints),
      action("settings", "Settings…", "Ctrl+Alt+S", run = h.openSettings),
      action("plugins", "Plugins…", run = h.openPlugins),
      action("theme", "Toggle Dark Theme", run = h.toggleTheme),
      action("split", "Toggle Split Editor", run = h.toggleSplit),
      action("softwrap", "Toggle Soft Wrap", run = h.toggleSoftWrap),
      action("zoom.in", "Zoom In", "Ctrl+=", run = h.zoomIn),
      action("zoom.out", "Zoom Out", "Ctrl+-", run = h.zoomOut),
      action("zoom.reset", "Reset Font Size", "Ctrl+0", run = h.zoomReset),
      action("copy.path", "Copy Absolute Path", enabled = state.hasEditor, run = h.copyAbsolutePath),
      action("copy.relative", "Copy Relative Path", enabled = state.hasEditor, run = h.copyRelativePath),
      action("copy.name", "Copy File Name", enabled = state.hasEditor, run = h.copyFileName),
      action("reveal.project", "Reveal in Project View", enabled = state.hasEditor, run = h.revealInProject),
      action("bookmark.toggle", "Toggle Bookmark", "F11", state.hasEditor, h.toggleBookmark),
      action("bookmark.next", "Next Bookmark", "Shift+F11", run = h.nextBookmark),
      action("bookmark.list", "Show Bookmarks", "Ctrl+F11", run = h.showBookmarks)
    )
  }
}
