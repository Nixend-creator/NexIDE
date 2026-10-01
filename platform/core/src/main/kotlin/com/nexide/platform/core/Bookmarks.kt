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

import com.nexide.platform.util.Logger
import java.io.File

data class Bookmark(
  val filePath: String,
  val line: Int, // 1-based
  val label: String = ""
) {
  val key: String get() = "$filePath:$line"
}

/**
 * Bookmarks persisted to ~/.nexide/bookmarks.txt
 */
object Bookmarks {
  private val items = linkedMapOf<String, Bookmark>()
  private var loaded = false

  private fun storeFile(): File =
    File(System.getProperty("user.home"), ".nexide/bookmarks.txt").also {
      it.parentFile?.mkdirs()
    }

  private fun ensureLoaded() {
    if (loaded) return
    loaded = true
    try {
      val f = storeFile()
      if (!f.exists()) return
      f.readLines(Charsets.UTF_8).forEach { line ->
        val parts = line.split('\t')
        if (parts.size >= 2) {
          val path = parts[0]
          val ln = parts[1].toIntOrNull() ?: return@forEach
          val label = parts.getOrNull(2) ?: "Line $ln"
          items["$path:$ln"] = Bookmark(path, ln, label)
        }
      }
    } catch (e: Exception) {
      Logger.warn("Bookmarks load: ${e.message}")
    }
  }

  private fun persist() {
    try {
      val f = storeFile()
      f.writeText(
        items.values.joinToString("\n") { "${it.filePath}\t${it.line}\t${it.label}" },
        Charsets.UTF_8
      )
    } catch (e: Exception) {
      Logger.warn("Bookmarks save: ${e.message}")
    }
  }

  fun list(): List<Bookmark> {
    ensureLoaded()
    return items.values.toList()
  }

  fun isBookmarked(filePath: String, line: Int): Boolean {
    ensureLoaded()
    return items.containsKey("$filePath:$line")
  }

  fun toggle(filePath: String, line: Int, label: String = ""): Boolean {
    ensureLoaded()
    val key = "$filePath:$line"
    return if (items.containsKey(key)) {
      items.remove(key)
      persist()
      false
    } else {
      items[key] = Bookmark(filePath, line, label.ifBlank { "Line $line" })
      persist()
      true
    }
  }

  fun remove(filePath: String, line: Int) {
    ensureLoaded()
    items.remove("$filePath:$line")
    persist()
  }

  fun clear() {
    ensureLoaded()
    items.clear()
    persist()
  }

  /** Next bookmark after [current], or first if none match. */
  fun next(currentFile: String?, currentLine: Int): Bookmark? {
    val all = list()
    if (all.isEmpty()) return null
    val idx = all.indexOfFirst { it.filePath == currentFile && it.line == currentLine }
    return if (idx < 0) all.first() else all[(idx + 1) % all.size]
  }
}
