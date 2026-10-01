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
import java.util.LinkedList

/**
 * MRU recent files, persisted to ~/.nexide/recent-files.txt
 */
object RecentFiles {
  private const val MAX = 30
  private val paths = LinkedList<String>()
  private var loaded = false

  private fun storeFile(): File =
    File(System.getProperty("user.home"), ".nexide/recent-files.txt").also {
      it.parentFile?.mkdirs()
    }

  private fun ensureLoaded() {
    if (loaded) return
    loaded = true
    try {
      val f = storeFile()
      if (!f.exists()) return
      f.readLines(Charsets.UTF_8)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { paths.add(it) }
    } catch (e: Exception) {
      Logger.warn("RecentFiles load: ${e.message}")
    }
  }

  private fun persist() {
    try {
      storeFile().writeText(paths.joinToString("\n"), Charsets.UTF_8)
    } catch (e: Exception) {
      Logger.warn("RecentFiles save: ${e.message}")
    }
  }

  fun add(path: String) {
    if (path.isBlank()) return
    ensureLoaded()
    paths.remove(path)
    paths.addFirst(path)
    while (paths.size > MAX) paths.removeLast()
    persist()
  }

  fun list(): List<String> {
    ensureLoaded()
    return paths.toList()
  }

  fun clear() {
    ensureLoaded()
    paths.clear()
    persist()
  }
}
