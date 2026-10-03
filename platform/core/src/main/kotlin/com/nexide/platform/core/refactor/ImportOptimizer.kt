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

/**
 * Removes unused imports and sorts the remaining ones (Kotlin / Java text heuristic).
 */
object ImportOptimizer {

  private val IMPORT_KT = Regex("""^\s*import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$""")
  private val IMPORT_JAVA = Regex("""^\s*import\s+(static\s+)?([\w.]+)\s*;\s*$""")

  data class Result(val text: String, val removed: Int)

  fun optimize(text: String): Result {
    val lines = text.lines()
    if (lines.isEmpty()) return Result(text, 0)

    data class Imp(val lineIndex: Int, val line: String, val simple: String, val sortKey: String)

    val imports = mutableListOf<Imp>()
    lines.forEachIndexed { idx, line ->
      parse(line)?.let { (simple, sortKey) ->
        imports.add(Imp(idx, line.trimEnd(), simple, sortKey))
      }
    }
    if (imports.isEmpty()) return Result(text, 0)

    val used = imports.filter { imp ->
      // wildcard always kept
      if (imp.simple == "*") return@filter true
      lines.indices.any { i ->
        if (i == imp.lineIndex) return@any false
        Regex("""\b${Regex.escape(imp.simple)}\b""").containsMatchIn(lines[i])
      }
    }

    val removed = imports.size - used.size
    if (removed == 0 && used.map { it.line } == imports.map { it.line }) {
      // still may need sort
      val sortedCheck = used.sortedBy { it.sortKey.lowercase() }
      if (sortedCheck.map { it.lineIndex } == used.map { it.lineIndex }) {
        return Result(text, 0)
      }
    }

    val sorted = used.sortedBy { it.sortKey.lowercase() }
    val importIndices = imports.map { it.lineIndex }.toSet()
    val firstImport = imports.minOf { it.lineIndex }
    val lastImport = imports.maxOf { it.lineIndex }

    val out = mutableListOf<String>()
    var i = 0
    while (i < lines.size) {
      when {
        i == firstImport -> {
          sorted.forEach { out.add(it.line) }
          // skip original import block (and blank lines only between imports)
          i = lastImport + 1
          while (i < lines.size && lines[i].isBlank() && i <= lastImport + 1) {
            // skip one trailing blank after imports; re-add single blank if body follows
            i++
          }
          if (sorted.isNotEmpty() && i < lines.size && lines[i].isNotBlank()) {
            out.add("")
          }
        }
        i in importIndices -> i++ // should not hit if we jump
        else -> {
          out.add(lines[i])
          i++
        }
      }
    }
    // Prefer preserving final newline style
    val result = out.joinToString("\n")
    val normalized = if (text.endsWith("\n") && !result.endsWith("\n")) result + "\n" else result
    return Result(normalized, removed)
  }

  private fun parse(line: String): Pair<String, String>? {
    IMPORT_KT.matchEntire(line.trim())?.let { m ->
      val full = m.groupValues[1]
      val alias = m.groupValues[2]
      val simple = if (alias.isNotBlank()) alias else full.substringAfterLast('.')
      return simple to full
    }
    IMPORT_JAVA.matchEntire(line.trim())?.let { m ->
      val full = m.groupValues[2]
      val simple = full.substringAfterLast('.')
      return simple to full
    }
    return null
  }
}
