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

import com.nexide.platform.core.build.Problem

data class IntentionAction(
  val title: String,
  val apply: (text: String) -> String
)

/**
 * Alt+Enter style quick-fixes from diagnostics on a line.
 */
object IntentionSupport {

  fun actionsForLine(
    text: String,
    line1Based: Int,
    problems: List<Problem>
  ): List<IntentionAction> {
    val lineProblems = problems.filter { it.line == line1Based }
    val actions = mutableListOf<IntentionAction>()
    if (lineProblems.any { it.message.startsWith("Unused import") }) {
      actions.add(
        IntentionAction("Remove unused import") { src ->
          removeLine(src, line1Based)
        }
      )
      actions.add(
        IntentionAction("Optimize imports") { src ->
          ImportOptimizer.optimize(src).text
        }
      )
    }
    if (lineProblems.any { it.message.contains("Unclosed") || it.message.contains("Unmatched") }) {
      // informational only — no auto-fix for braces yet
    }
    return actions
  }

  /**
   * Applies the first available intention on [line1Based], or null if none.
   */
  fun applyFirst(
    text: String,
    line1Based: Int,
    problems: List<Problem>
  ): Pair<String, String>? {
    val action = actionsForLine(text, line1Based, problems).firstOrNull() ?: return null
    val next = action.apply(text)
    if (next == text) return null
    return next to action.title
  }

  internal fun removeLine(text: String, line1Based: Int): String {
    val lines = text.lines().toMutableList()
    val idx = line1Based - 1
    if (idx !in lines.indices) return text
    lines.removeAt(idx)
    // drop extra blank after removing import in import block
    if (idx < lines.size && lines[idx].isBlank()) {
      // keep single blank max — leave as is
    }
    val joined = lines.joinToString("\n")
    return if (text.endsWith("\n") && !joined.endsWith("\n")) joined + "\n" else joined
  }
}
