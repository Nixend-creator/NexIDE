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

package com.nexide.platform.core.refactor

import com.nexide.platform.util.Logger
import java.io.File

/**
 * One planned text replacement in a file.
 */
data class RenameOccurrence(
  val filePath: String,
  val line: Int,
  val column: Int,
  val lineText: String
)

data class RenamePreview(
  val oldName: String,
  val newName: String,
  val occurrences: List<RenameOccurrence>
)

/**
 * Heuristic project-wide rename by whole-word identifier match.
 * Not scope-aware (not true PSI refactor).
 */
object RenameRefactoring {

  private val TEXT_EXTENSIONS = setOf(
    "kt", "kts", "java", "groovy", "gradle", "properties", "xml", "md", "txt"
  )

  fun isValidIdentifier(name: String): Boolean {
    if (name.isEmpty()) return false
    if (!name[0].isLetter() && name[0] != '_') return false
    return name.all { it.isLetterOrDigit() || it == '_' }
  }

  /**
   * Collects whole-word matches of [oldName] under [projectRoot].
   */
  fun preview(projectRoot: File, oldName: String, newName: String): RenamePreview {
    val word = Regex("""\b${Regex.escape(oldName)}\b""")
    val list = mutableListOf<RenameOccurrence>()
    walk(projectRoot) { file ->
      val lines = try {
        file.readLines(Charsets.UTF_8)
      } catch (_: Exception) {
        return@walk
      }
      lines.forEachIndexed { index, line ->
        word.findAll(line).forEach { m ->
          val before = line.substring(0, m.range.first)
          // Skip matches that look like line comments
          if (before.contains("//")) return@forEach
          list.add(
            RenameOccurrence(
              filePath = file.absolutePath,
              line = index + 1,
              column = m.range.first + 1,
              lineText = line.trim().take(120)
            )
          )
        }
      }
    }
    return RenamePreview(oldName, newName, list)
  }

  /**
   * Applies rename in all files from [preview]. Returns number of files changed.
   */
  fun apply(preview: RenamePreview): Int {
    if (!isValidIdentifier(preview.newName)) {
      Logger.warn("Invalid identifier: ${preview.newName}")
      return 0
    }
    val word = Regex("""\b${Regex.escape(preview.oldName)}\b""")
    val byFile = preview.occurrences.groupBy { it.filePath }
    var filesChanged = 0
    for ((path, _) in byFile) {
      val file = File(path)
      if (!file.isFile) continue
      val original = try {
        file.readText(Charsets.UTF_8)
      } catch (e: Exception) {
        Logger.warn("Cannot read $path: ${e.message}")
        continue
      }
      val updated = word.replace(original, preview.newName)
      if (updated != original) {
        try {
          file.writeText(updated, Charsets.UTF_8)
          filesChanged++
          Logger.info("Renamed in $path")
        } catch (e: Exception) {
          Logger.warn("Cannot write $path: ${e.message}")
        }
      }
    }
    return filesChanged
  }

  private fun walk(root: File, visitor: (File) -> Unit) {
    val skip = setOf(".git", ".gradle", "build", "out", "node_modules", ".idea")
    root.walkTopDown()
      .onEnter { it.name !in skip }
      .filter { it.isFile && it.extension.lowercase() in TEXT_EXTENSIONS }
      .filter { it.length() < 2_000_000 }
      .forEach(visitor)
  }
}
