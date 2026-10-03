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

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Visibility / dismiss hooks for Escape stacking.
 */
class IdeEscapeStack(
  val dismissCommandPalette: () -> Boolean,
  val dismissSearchEverywhere: () -> Boolean,
  val dismissRecentFiles: () -> Boolean,
  val dismissBookmarks: () -> Boolean,
  val dismissMarketplace: () -> Boolean,
  val dismissSettings: () -> Boolean,
  val dismissRunConfig: () -> Boolean,
  val dismissGoto: () -> Boolean,
  val dismissRename: () -> Boolean,
  val dismissIntention: () -> Boolean,
  val dismissFindInFiles: () -> Boolean,
  val dismissProblems: () -> Boolean,
  val dismissBreakpoints: () -> Boolean,
  val dismissDebug: () -> Boolean,
  val dismissRun: () -> Boolean,
  val dismissGit: () -> Boolean,
  val dismissTerminal: () -> Boolean,
  val dismissStructure: () -> Boolean
) {
  fun tryDismiss(): Boolean =
    dismissCommandPalette() ||
      dismissSearchEverywhere() ||
      dismissRecentFiles() ||
      dismissBookmarks() ||
      dismissMarketplace() ||
      dismissSettings() ||
      dismissRunConfig() ||
      dismissGoto() ||
      dismissRename() ||
      dismissIntention() ||
      dismissFindInFiles() ||
      dismissProblems() ||
      dismissBreakpoints() ||
      dismissDebug() ||
      dismissRun() ||
      dismissGit() ||
      dismissTerminal() ||
      dismissStructure()
}

/**
 * Window-level key bindings (not editor-local).
 */
object IdeKeyMap {

  fun handle(
    event: KeyEvent,
    escape: IdeEscapeStack,
    hasEditor: Boolean,
    hasRunConfig: Boolean,
    openProject: () -> Unit,
    recentFiles: () -> Unit,
    findAction: () -> Unit,
    searchEverywhere: () -> Unit,
    zoomIn: () -> Unit,
    zoomOut: () -> Unit,
    zoomReset: () -> Unit,
    toggleBookmark: () -> Unit,
    nextBookmark: () -> Unit,
    showBookmarks: () -> Unit,
    save: () -> Unit,
    saveAll: () -> Unit,
    closeTab: () -> Unit,
    closeAll: () -> Unit,
    findInFiles: () -> Unit,
    build: () -> Unit,
    settings: () -> Unit,
    problems: () -> Unit,
    toggleBreakpoint: () -> Unit,
    run: () -> Unit,
    debug: () -> Unit,
    gotoDeclaration: () -> Unit,
    findUsages: () -> Unit,
    rename: () -> Unit,
    optimizeImports: () -> Unit,
    showIntention: () -> Unit,
    nextProblem: () -> Unit,
    prevProblem: () -> Unit
  ): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val ctrlOrMeta = event.isCtrlPressed || event.isMetaPressed
    val shift = event.isShiftPressed

    return when {
      event.key == Key.Escape -> escape.tryDismiss()

      ctrlOrMeta && event.key == Key.O -> {
        openProject(); true
      }
      ctrlOrMeta && event.key == Key.E && !shift -> {
        recentFiles(); true
      }
      ctrlOrMeta && shift && event.key == Key.A -> {
        findAction(); true
      }
      ctrlOrMeta && event.key == Key.N && !shift -> {
        searchEverywhere(); true
      }
      ctrlOrMeta && (event.key == Key.Equals || event.key == Key.Plus) -> {
        zoomIn(); true
      }
      ctrlOrMeta && event.key == Key.Minus -> {
        zoomOut(); true
      }
      ctrlOrMeta && event.key == Key.Zero && !shift -> {
        zoomReset(); true
      }
      event.key == Key.F11 && !shift && !ctrlOrMeta -> {
        toggleBookmark(); true
      }
      event.key == Key.F11 && shift && !ctrlOrMeta -> {
        nextBookmark(); true
      }
      ctrlOrMeta && event.key == Key.F11 -> {
        showBookmarks(); true
      }
      ctrlOrMeta && shift && event.key == Key.S -> {
        saveAll(); true
      }
      ctrlOrMeta && event.key == Key.S && !event.isAltPressed -> {
        save(); true
      }
      ctrlOrMeta && event.key == Key.W && !shift -> {
        closeTab(); true
      }
      ctrlOrMeta && shift && event.key == Key.W -> {
        closeAll(); true
      }
      ctrlOrMeta && shift && event.key == Key.F -> {
        findInFiles(); true
      }
      (ctrlOrMeta && shift && event.key == Key.B) ||
        (ctrlOrMeta && event.key == Key.F9) -> {
        build(); true
      }
      ctrlOrMeta && event.isAltPressed && event.key == Key.S -> {
        settings(); true
      }
      ctrlOrMeta && shift && event.key == Key.F12 -> {
        problems(); true
      }
      event.key == Key.F8 && hasEditor -> {
        toggleBreakpoint(); true
      }
      shift && event.key == Key.F10 && hasRunConfig -> {
        run(); true
      }
      shift && event.key == Key.F9 && hasRunConfig -> {
        debug(); true
      }
      ctrlOrMeta && event.key == Key.B && !shift -> {
        gotoDeclaration(); true
      }
      event.isAltPressed && event.key == Key.F7 -> {
        findUsages(); true
      }
      event.isShiftPressed && event.key == Key.F6 -> {
        rename(); true
      }
      // Ctrl+Alt+O — Optimize Imports
      ctrlOrMeta && event.isAltPressed && event.key == Key.O -> {
        optimizeImports(); true
      }
      // Alt+Enter — Intention / quick-fix
      event.isAltPressed && (event.key == Key.Enter || event.key == Key.NumPadEnter) &&
        !ctrlOrMeta -> {
        showIntention(); true
      }
      // F2 / Shift+F2 — next / previous problem
      event.key == Key.F2 && !shift && !ctrlOrMeta && !event.isAltPressed -> {
        nextProblem(); true
      }
      event.key == Key.F2 && shift && !ctrlOrMeta -> {
        prevProblem(); true
      }
      else -> false
    }
  }
}
