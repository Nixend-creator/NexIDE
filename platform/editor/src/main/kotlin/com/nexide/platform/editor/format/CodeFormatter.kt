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

package com.nexide.platform.editor.format

/**
 * Lightweight formatter for Kotlin/Java-like sources.
 *
 * - Normalizes indentation to [indentSize] spaces based on brace depth
 * - Trims trailing whitespace
 * - Collapses runs of more than 2 blank lines
 * - Ensures a single trailing newline
 */
object CodeFormatter {

  fun format(text: String, indentSize: Int = 2): String {
    if (text.isEmpty()) return text
    val indentUnit = " ".repeat(indentSize.coerceIn(1, 8))
    val lines = text.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val out = mutableListOf<String>()
    var depth = 0
    var blankRun = 0

    for (raw in lines) {
      val trimmedEnd = raw.trimEnd()
      val trimmed = trimmedEnd.trimStart()

      if (trimmed.isEmpty()) {
        blankRun++
        if (blankRun <= 2) {
          out.add("")
        }
        continue
      }
      blankRun = 0

      // Decrease depth for lines that start with closing braces/brackets
      val leadingClosers = countLeadingClosers(trimmed)
      val effectiveDepth = (depth - leadingClosers).coerceAtLeast(0)

      // Skip reformatting inside block comments / raw strings — simple heuristic
      val formattedLine = indentUnit.repeat(effectiveDepth) + trimmed
      out.add(formattedLine)

      depth += countDelta(trimmed)
      if (depth < 0) depth = 0
    }

    // Trim trailing blank lines, ensure one final newline
    while (out.isNotEmpty() && out.last().isEmpty()) {
      out.removeAt(out.lastIndex)
    }
    return out.joinToString("\n") + "\n"
  }

  private fun countLeadingClosers(line: String): Int {
    var i = 0
    var n = 0
    while (i < line.length) {
      when (line[i]) {
        '}', ')', ']' -> {
          n++
          i++
        }
        else -> break
      }
    }
    return n
  }

  /**
   * Net change in brace depth for a line (ignores braces in strings/comments approximately).
   */
  private fun countDelta(line: String): Int {
    var delta = 0
    var inSingle = false
    var inDouble = false
    var inLineComment = false
    var i = 0
    while (i < line.length) {
      val c = line[i]
      val next = if (i + 1 < line.length) line[i + 1] else '\u0000'
      if (inLineComment) break
      when {
        !inSingle && !inDouble && c == '/' && next == '/' -> {
          inLineComment = true
          i++
        }
        !inDouble && c == '\'' && (i == 0 || line[i - 1] != '\\') -> inSingle = !inSingle
        !inSingle && c == '"' && (i == 0 || line[i - 1] != '\\') -> inDouble = !inDouble
        !inSingle && !inDouble && (c == '{' || c == '(' || c == '[') -> delta++
        !inSingle && !inDouble && (c == '}' || c == ')' || c == ']') -> delta--
      }
      i++
    }
    return delta
  }
}
