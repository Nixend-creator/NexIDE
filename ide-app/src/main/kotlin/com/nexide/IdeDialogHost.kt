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
import com.nexide.platform.core.Bookmarks
import com.nexide.platform.core.RecentFiles
import com.nexide.platform.core.history.LocalHistory
import com.nexide.platform.core.navigation.NavigationTarget
import com.nexide.platform.core.notify.NotificationService
import com.nexide.platform.core.refactor.IntentionAction
import com.nexide.platform.core.run.RunConfiguration
import com.nexide.platform.core.run.RunConfigurationManager
import com.nexide.platform.core.vfs.LocalFileSystem
import com.nexide.platform.editor.Editor
import com.nexide.platform.ui.history.LocalHistoryDialog
import com.nexide.platform.ui.navigation.BookmarksDialog
import com.nexide.platform.ui.navigation.CommandPalette
import com.nexide.platform.ui.navigation.GotoTargetsDialog
import com.nexide.platform.ui.navigation.RecentFilesDialog
import com.nexide.platform.ui.navigation.SearchEverywhereDialog
import com.nexide.platform.ui.plugins.MarketplaceDialog
import com.nexide.platform.ui.refactor.IntentionDialog
import com.nexide.platform.ui.refactor.RenameDialog
import com.nexide.platform.ui.run.RunConfigDialog
import com.nexide.platform.ui.settings.SettingsDialog
import com.nexide.platform.util.Logger
import java.io.File

/**
 * Host for modal dialogs — keeps Window content in Main.kt thinner.
 */
@Composable
fun IdeDialogHost(
  projectPath: String,
  activeEditor: Editor?,
  openEditors: List<Editor>,
  recentFilesVisible: Boolean,
  onRecentFilesVisible: (Boolean) -> Unit,
  commandPaletteVisible: Boolean,
  onCommandPaletteVisible: (Boolean) -> Unit,
  paletteActions: List<com.nexide.platform.ui.navigation.PaletteAction>,
  bookmarksVisible: Boolean,
  bookmarksTick: Int,
  onBookmarksVisible: (Boolean) -> Unit,
  onBookmarksTick: () -> Unit,
  searchEverywhereVisible: Boolean,
  onSearchEverywhereVisible: (Boolean) -> Unit,
  marketplaceVisible: Boolean,
  onMarketplaceVisible: (Boolean) -> Unit,
  settingsVisible: Boolean,
  onSettingsVisible: (Boolean) -> Unit,
  onSettingsApplied: () -> Unit,
  runConfigDialogVisible: Boolean,
  onRunConfigDialogVisible: (Boolean) -> Unit,
  runConfigManager: RunConfigurationManager,
  onRunConfig: (RunConfiguration, debug: Boolean) -> Unit,
  gotoTargets: Pair<String, List<NavigationTarget>>?,
  gotoDialogTitle: String,
  onGotoDismiss: () -> Unit,
  onNavigateTarget: (NavigationTarget) -> Unit,
  localHistoryVisible: Boolean,
  onLocalHistoryVisible: (Boolean) -> Unit,
  onSnapshotBeforeEdit: (Editor, String) -> Unit,
  intentionActions: List<IntentionAction>?,
  onIntentionSelect: (Int) -> Unit,
  onIntentionDismiss: () -> Unit,
  renameSymbol: String?,
  onRenameDismiss: () -> Unit,
  onRenameApplied: (files: Int, newName: String) -> Unit,
  onOpenFile: (path: String, line: Int?) -> Unit
) {
  if (recentFilesVisible) {
    RecentFilesDialog(
      paths = RecentFiles.list(),
      onSelect = { path ->
        onRecentFilesVisible(false)
        onOpenFile(path, null)
      },
      onDismiss = { onRecentFilesVisible(false) }
    )
  }
  if (commandPaletteVisible) {
    CommandPalette(
      actions = paletteActions,
      onDismiss = { onCommandPaletteVisible(false) }
    )
  }
  if (bookmarksVisible) {
    @Suppress("UNUSED_EXPRESSION")
    bookmarksTick
    BookmarksDialog(
      bookmarks = Bookmarks.list(),
      onNavigate = { bm ->
        onBookmarksVisible(false)
        onOpenFile(bm.filePath, bm.line)
      },
      onRemove = { bm ->
        Bookmarks.remove(bm.filePath, bm.line)
        onBookmarksTick()
      },
      onClear = {
        Bookmarks.clear()
        onBookmarksTick()
      },
      onDismiss = { onBookmarksVisible(false) }
    )
  }
  if (searchEverywhereVisible) {
    SearchEverywhereDialog(
      projectRoot = File(projectPath),
      onOpenFile = { path, line ->
        onSearchEverywhereVisible(false)
        onOpenFile(path, line)
      },
      onDismiss = { onSearchEverywhereVisible(false) }
    )
  }
  if (marketplaceVisible) {
    MarketplaceDialog(onDismiss = { onMarketplaceVisible(false) })
  }
  if (settingsVisible) {
    SettingsDialog(
      onDismiss = { onSettingsVisible(false) },
      onApplied = onSettingsApplied
    )
  }
  if (runConfigDialogVisible) {
    RunConfigDialog(
      projectPath = projectPath,
      manager = runConfigManager,
      onDismiss = { onRunConfigDialogVisible(false) },
      onRun = { cfg ->
        onRunConfigDialogVisible(false)
        onRunConfig(cfg, false)
      },
      onDebug = { cfg ->
        onRunConfigDialogVisible(false)
        onRunConfig(cfg, true)
      }
    )
  }
  gotoTargets?.let { (symbol, targets) ->
    GotoTargetsDialog(
      symbol = symbol,
      targets = targets,
      title = gotoDialogTitle,
      onSelect = onNavigateTarget,
      onDismiss = onGotoDismiss
    )
  }
  if (localHistoryVisible) {
    val ed = activeEditor
    LocalHistoryDialog(
      fileName = ed?.file?.name ?: "",
      entries = ed?.let { LocalHistory.entries(it.file.path) } ?: emptyList(),
      onRestore = { index ->
        if (ed != null) {
          val content = LocalHistory.contentAt(ed.file.path, index)
          if (content != null) {
            onSnapshotBeforeEdit(ed, "before history restore")
            ed.replaceText(content)
            NotificationService.info("Local History", "Restored snapshot #$index")
          }
        }
        onLocalHistoryVisible(false)
      },
      onDismiss = { onLocalHistoryVisible(false) }
    )
  }
  intentionActions?.let { actions ->
    IntentionDialog(
      titles = actions.map { it.title },
      onSelect = onIntentionSelect,
      onDismiss = onIntentionDismiss
    )
  }
  renameSymbol?.let { symbol ->
    RenameDialog(
      projectRoot = File(projectPath),
      oldName = symbol,
      onBeforeApply = {
        openEditors.forEach { ed ->
          onSnapshotBeforeEdit(ed, "before rename")
        }
      },
      onApplied = { files, newName ->
        Logger.info("Renamed '$symbol' → '$newName' in $files file(s)")
        onRenameApplied(files, newName)
      },
      onDismiss = onRenameDismiss
    )
  }
}
