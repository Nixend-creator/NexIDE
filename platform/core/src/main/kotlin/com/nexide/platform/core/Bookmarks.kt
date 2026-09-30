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

package com.nexide.platform.core

data class Bookmark(
  val filePath: String,
  val line: Int, // 1-based
  val label: String = ""
) {
  val key: String get() = "$filePath:$line"
}

/**
 * Session bookmarks (toggle line, list, cycle).
 */
object Bookmarks {
  private val items = linkedMapOf<String, Bookmark>()

  fun list(): List<Bookmark> = items.values.toList()

  fun isBookmarked(filePath: String, line: Int): Boolean =
    items.containsKey("$filePath:$line")

  fun toggle(filePath: String, line: Int, label: String = ""): Boolean {
    val key = "$filePath:$line"
    return if (items.containsKey(key)) {
      items.remove(key)
      false
    } else {
      items[key] = Bookmark(filePath, line, label.ifBlank { "Line $line" })
      true
    }
  }

  fun remove(filePath: String, line: Int) {
    items.remove("$filePath:$line")
  }

  fun clear() {
    items.clear()
  }

  /** Next bookmark after [current], or first if none match. */
  fun next(currentFile: String?, currentLine: Int): Bookmark? {
    val all = list()
    if (all.isEmpty()) return null
    val idx = all.indexOfFirst { it.filePath == currentFile && it.line == currentLine }
    return if (idx < 0) all.first() else all[(idx + 1) % all.size]
  }
}
