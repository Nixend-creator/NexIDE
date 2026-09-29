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

/**
 * Persists recently opened project paths under the user home directory.
 */
object RecentProjects {
  private const val MAX_ENTRIES = 10
  private val storeFile: File by lazy {
    File(System.getProperty("user.home"), ".nexide/recent-projects.txt").also { f ->
      f.parentFile?.mkdirs()
    }
  }

  fun list(): List<String> {
    return try {
      if (!storeFile.exists()) return emptyList()
      storeFile.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && File(it).isDirectory }
        .distinct()
        .take(MAX_ENTRIES)
    } catch (e: Exception) {
      Logger.warn("Failed to read recent projects: ${e.message}")
      emptyList()
    }
  }

  /**
   * Moves [path] to the top of the recent list and persists.
   */
  fun add(path: String) {
    val canonical = try {
      File(path).canonicalPath
    } catch (_: Exception) {
      path
    }
    val updated = (listOf(canonical) + list().filter { it != canonical })
      .take(MAX_ENTRIES)
    try {
      storeFile.parentFile?.mkdirs()
      storeFile.writeText(updated.joinToString("\n") + "\n")
    } catch (e: Exception) {
      Logger.warn("Failed to save recent projects: ${e.message}")
    }
  }

  fun remove(path: String) {
    val canonical = try {
      File(path).canonicalPath
    } catch (_: Exception) {
      path
    }
    val updated = list().filter { it != canonical }
    try {
      storeFile.writeText(updated.joinToString("\n").let { if (it.isEmpty()) "" else it + "\n" })
    } catch (e: Exception) {
      Logger.warn("Failed to update recent projects: ${e.message}")
    }
  }

  fun displayName(path: String): String = File(path).name.ifEmpty { path }
}
