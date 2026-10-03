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
 * Next/previous diagnostic relative to caret (file path + line).
 */
object ProblemNavigation {

  fun step(
    problems: List<Problem>,
    currentFile: String?,
    currentLine: Int,
    forward: Boolean
  ): Problem? {
    val sorted = problems
      .filter { it.filePath != null && it.line != null }
      .sortedWith(
        compareBy<Problem> { it.filePath ?: "" }
          .thenBy { it.line ?: 0 }
      )
    if (sorted.isEmpty()) return null
    if (currentFile == null) return if (forward) sorted.first() else sorted.last()

    return if (forward) {
      sorted.firstOrNull { p ->
        val fp = p.filePath!!
        val ln = p.line!!
        fp > currentFile || (fp == currentFile && ln > currentLine)
      } ?: sorted.first()
    } else {
      sorted.lastOrNull { p ->
        val fp = p.filePath!!
        val ln = p.line!!
        fp < currentFile || (fp == currentFile && ln < currentLine)
      } ?: sorted.last()
    }
  }
}
