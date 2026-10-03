/*
 * Copyright 2026 NexIDE Contributors
 * Licensed under the Apache License, Version 2.0
 */
package com.nexide

/**
 * Documented global shortcuts (window-level). Editor-local shortcuts live in EditorView.
 */
object IdeKeyShortcuts {
  const val OPEN_PROJECT = "Ctrl+O"
  const val SAVE = "Ctrl+S"
  const val SAVE_ALL = "Ctrl+Shift+S"
  const val CLOSE_TAB = "Ctrl+W"
  const val CLOSE_ALL = "Ctrl+Shift+W"
  const val RECENT_FILES = "Ctrl+E"
  const val FIND_ACTION = "Ctrl+Shift+A"
  const val SEARCH_EVERYWHERE = "Ctrl+N"
  const val FIND_IN_FILES = "Ctrl+Shift+F"
  const val GOTO_DECLARATION = "Ctrl+B"
  const val FIND_USAGES = "Alt+F7"
  const val RENAME = "Shift+F6"
  const val BUILD = "Ctrl+Shift+B / Ctrl+F9"
  const val RUN = "Shift+F10"
  const val DEBUG = "Shift+F9"
  const val TOGGLE_BOOKMARK = "F11"
  const val NEXT_BOOKMARK = "Shift+F11"
  const val BOOKMARKS = "Ctrl+F11"
  const val ZOOM_IN = "Ctrl+="
  const val ZOOM_OUT = "Ctrl+-"
  const val ZOOM_RESET = "Ctrl+0"
  const val SETTINGS = "Ctrl+Alt+S"
  const val OPTIMIZE_IMPORTS = "Ctrl+Alt+O"
  const val INTENTION = "Alt+Enter"
}
