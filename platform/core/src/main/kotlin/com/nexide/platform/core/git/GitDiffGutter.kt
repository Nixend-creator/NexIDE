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

package com.nexide.platform.core.git

import com.nexide.platform.util.Logger
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class LineDiffType {
  ADDED,
  MODIFIED
}

/**
 * Computes per-line VCS change markers for a file relative to HEAD.
 */
object GitDiffGutter {

  private val cache = ConcurrentHashMap<String, Pair<Long, Map<Int, LineDiffType>>>()

  /**
   * Returns map of 1-based line number → change type for [filePath] in a git repo at [projectRoot].
   * Empty if not a git repo or file is clean / untracked without content.
   */
  fun lineChanges(projectRoot: File, filePath: String): Map<Int, LineDiffType> {
    val file = File(filePath)
    if (!file.isFile) return emptyMap()
    val key = file.canonicalPath
    val stamp = file.lastModified()
    cache[key]?.let { (cachedStamp, map) ->
      if (cachedStamp == stamp) return map
    }

    val result = compute(projectRoot, file)
    cache[key] = stamp to result
    return result
  }

  fun invalidate(filePath: String? = null) {
    if (filePath == null) cache.clear()
    else cache.remove(File(filePath).canonicalPath)
  }

  private fun compute(projectRoot: File, file: File): Map<Int, LineDiffType> {
    if (!File(projectRoot, ".git").exists()) return emptyMap()
    val relative = try {
      projectRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/')
    } catch (_: Exception) {
      return emptyMap()
    }

    return try {
      // Unified diff against HEAD for this file only
      val pb = ProcessBuilder("git", "diff", "-U0", "HEAD", "--", relative)
        .directory(projectRoot)
        .redirectErrorStream(true)
      val process = pb.start()
      val output = process.inputStream.bufferedReader().readText()
      process.waitFor()
      parseUnifiedDiff(output)
    } catch (e: Exception) {
      Logger.warn("GitDiffGutter: ${e.message}")
      emptyMap()
    }
  }

  /**
   * Parses `git diff -U0` hunks:
   * @@ -oldStart,oldCount +newStart,newCount @@
   */
  internal fun parseUnifiedDiff(diff: String): Map<Int, LineDiffType> {
    val map = mutableMapOf<Int, LineDiffType>()
    val hunkHeader = Regex("""^@@\s+-(\d+)(?:,(\d+))?\s+\+(\d+)(?:,(\d+))?\s+@@""")
    var newLine = 0
    var inHunk = false

    for (line in diff.lineSequence()) {
      val header = hunkHeader.find(line)
      if (header != null) {
        newLine = header.groupValues[3].toInt()
        inHunk = true
        val oldCount = header.groupValues[2].ifEmpty { "1" }.toInt()
        val newCount = header.groupValues[4].ifEmpty { "1" }.toInt()
        // Pure deletion: no new lines in hunk
        if (newCount == 0) {
          inHunk = false
        }
        // For pure additions oldCount==0 — all + lines are ADDED
        continue
      }
      if (!inHunk) continue
      when {
        line.startsWith("+") && !line.startsWith("+++") -> {
          val prev = map[newLine]
          map[newLine] = when (prev) {
            LineDiffType.MODIFIED -> LineDiffType.MODIFIED
            else -> LineDiffType.ADDED
          }
          // If this line also had a removal in same hunk, mark modified — approximate:
          // We only see + lines at new positions; deletions don't map to new lines.
          newLine++
        }
        line.startsWith("-") && !line.startsWith("---") -> {
          // Deletion: mark current newLine as modified if next + lands here
          // Standard approach with -U0: - line doesn't advance newLine;
          // following + at same conceptual place → MODIFIED
          // We set a sticky "pending delete" on newLine
          map[newLine] = LineDiffType.MODIFIED
        }
        line.startsWith(" ") -> {
          newLine++
        }
      }
    }

    // Refine: lines that had both delete sticky and add stay MODIFIED; pure + stay ADDED
    // Current logic over-marks MODIFIED when only deletions precede — acceptable for MVP.
    return map
  }
}
