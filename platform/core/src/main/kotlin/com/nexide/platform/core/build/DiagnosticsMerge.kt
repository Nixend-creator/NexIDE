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

/**
 * Merges build/test diagnostics with lightweight live checks into one list.
 */
object DiagnosticsMerge {

  fun merge(build: List<Problem>, live: List<Problem>): List<Problem> {
    return (build + live).distinctBy { key(it) }
      .sortedWith(
        compareBy<Problem> { it.severity.ordinal }
          .thenBy { it.filePath ?: "" }
          .thenBy { it.line ?: 0 }
      )
  }

  fun key(p: Problem): String =
    "${p.filePath}:${p.line}:${p.column}:${p.message}"

  /**
   * Live checks for multiple source files (path → text).
   */
  fun liveForFiles(files: List<Pair<String, String>>): List<Problem> {
    return files.flatMap { (path, text) ->
      try {
        LiveSyntaxChecker.check(path, text) + SimpleInspections.checkFile(path, text)
      } catch (_: Exception) {
        emptyList()
      }
    }
  }
}
