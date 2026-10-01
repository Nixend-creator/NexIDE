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

package com.nexide.platform.core.build

enum class ProblemSeverity {
  ERROR,
  WARNING,
  INFO
}

/**
 * A compiler/build diagnostic that can be navigated to.
 */
data class Problem(
  val severity: ProblemSeverity,
  val message: String,
  val filePath: String?,
  val line: Int?,
  val column: Int? = null,
  val rawLine: String = ""
)

/**
 * Parses common compiler output formats into [Problem]s.
 *
 * Supports:
 * - Kotlin: path/File.kt:12:34: error: message
 * - Java/javac: path/File.java:12: error: message
 * - Gradle: e: file:///path/File.kt:12:34 message
 * - Generic: path:line: message
 */
object ProblemParser {

  private val patterns = listOf(
    // e: file:///path/File.kt:12:34 Message
    Regex("""^e:\s*file://(.+?):(\d+)(?::(\d+))?\s+(.*)$"""),
    // w: file:///path/File.kt:12:34 Message
    Regex("""^w:\s*file://(.+?):(\d+)(?::(\d+))?\s+(.*)$"""),
    // /path/File.kt:12:34: error: message
    Regex("""^(.+?\.(?:kt|kts|java|groovy)):(\d+)(?::(\d+))?:\s*(error|warning|info):\s*(.*)$""", RegexOption.IGNORE_CASE),
    // /path/File.java:12: error: message
    Regex("""^(.+?\.(?:kt|kts|java)):(\d+):\s*(error|warning):\s*(.*)$""", RegexOption.IGNORE_CASE),
    // path:line:col: message (fallback)
    Regex("""^(.+?):(\d+)(?::(\d+))?:\s*(.*)$""")
  )

  fun parseLine(line: String): Problem? {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return null

    // Kotlin gradle style e: / w:
    patterns[0].matchEntire(trimmed)?.let { m ->
      return Problem(
        severity = ProblemSeverity.ERROR,
        message = m.groupValues[4],
        filePath = m.groupValues[1],
        line = m.groupValues[2].toIntOrNull(),
        column = m.groupValues[3].toIntOrNull()?.takeIf { it > 0 },
        rawLine = line
      )
    }
    patterns[1].matchEntire(trimmed)?.let { m ->
      return Problem(
        severity = ProblemSeverity.WARNING,
        message = m.groupValues[4],
        filePath = m.groupValues[1],
        line = m.groupValues[2].toIntOrNull(),
        column = m.groupValues[3].toIntOrNull()?.takeIf { it > 0 },
        rawLine = line
      )
    }
    patterns[2].matchEntire(trimmed)?.let { m ->
      val sev = when (m.groupValues[4].lowercase()) {
        "error" -> ProblemSeverity.ERROR
        "warning" -> ProblemSeverity.WARNING
        else -> ProblemSeverity.INFO
      }
      return Problem(
        severity = sev,
        message = m.groupValues[5],
        filePath = m.groupValues[1],
        line = m.groupValues[2].toIntOrNull(),
        column = m.groupValues[3].toIntOrNull()?.takeIf { it > 0 },
        rawLine = line
      )
    }
    patterns[3].matchEntire(trimmed)?.let { m ->
      val sev = if (m.groupValues[3].equals("warning", true)) {
        ProblemSeverity.WARNING
      } else {
        ProblemSeverity.ERROR
      }
      return Problem(
        severity = sev,
        message = m.groupValues[4],
        filePath = m.groupValues[1],
        line = m.groupValues[2].toIntOrNull(),
        rawLine = line
      )
    }
    // Generic only if looks like a path with extension
    patterns[4].matchEntire(trimmed)?.let { m ->
      val path = m.groupValues[1]
      if (!path.contains('/') && !path.contains('\\')) return null
      if (!path.contains('.')) return null
      return Problem(
        severity = ProblemSeverity.ERROR,
        message = m.groupValues[4],
        filePath = path,
        line = m.groupValues[2].toIntOrNull(),
        column = m.groupValues[3].toIntOrNull()?.takeIf { it > 0 },
        rawLine = line
      )
    }
    return null
  }

  fun parseOutput(output: String): List<Problem> {
    val fromLines = output.lineSequence().mapNotNull { parseLine(it) }.toList()
    val fromTests = parseTestFailures(output)
    return (fromLines + fromTests).distinctBy {
      "${it.filePath}:${it.line}:${it.message}"
    }
  }

  /**
   * Gradle/JUnit style failures + first stack frame with source file.
   */
  fun parseTestFailures(output: String): List<Problem> {
    val problems = mutableListOf<Problem>()
    val stackFrame = Regex("""^\s*at\s+[\w.$]+\(([\w.]+\.(?:kt|java)):(\d+)\)""")
    val failedHeader = Regex("""(?i)([\w.]+)\s*>\s*([\w`]+)\s+FAILED""")
    var currentTest: String? = null
    for (line in output.lineSequence()) {
      failedHeader.find(line)?.let { m ->
        currentTest = "${m.groupValues[1]}.${m.groupValues[2]}"
      }
      val frame = stackFrame.find(line.trim())
      if (frame != null) {
        problems.add(
          Problem(
            severity = ProblemSeverity.ERROR,
            message = currentTest?.let { "Test failed: $it" } ?: "Test failure",
            filePath = frame.groupValues[1],
            line = frame.groupValues[2].toIntOrNull(),
            rawLine = line
          )
        )
        currentTest = null
      }
    }
    return problems
  }
}
