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

/**
 * Git working-tree status for a single path.
 */
enum class GitFileStatus {
  /** Not changed / tracked and clean (or not a git repo). */
  CLEAN,

  /** Modified in working tree or index. */
  MODIFIED,

  /** Added / staged new file. */
  ADDED,

  /** Untracked. */
  UNTRACKED,

  /** Deleted. */
  DELETED,

  /** Renamed or copied. */
  RENAMED
}

/**
 * Snapshot of `git status --porcelain` for a repository root.
 */
class GitStatusSnapshot(
  val repoRoot: File,
  /** Absolute path → status */
  val statuses: Map<String, GitFileStatus>
) {
  fun statusFor(absolutePath: String): GitFileStatus {
    val normalized = File(absolutePath).canonicalPath
    return statuses[normalized] ?: GitFileStatus.CLEAN
  }

  fun statusFor(file: File): GitFileStatus = statusFor(file.canonicalPath)

  companion object {
    val EMPTY = GitStatusSnapshot(File("."), emptyMap())
  }
}

/**
 * Loads git status via the system `git` CLI.
 */
object GitStatusService {

  fun isGitRepo(projectDir: File): Boolean {
    return File(projectDir, ".git").exists() ||
      findGitRoot(projectDir) != null
  }

  fun findGitRoot(start: File): File? {
    var dir: File? = start.canonicalFile
    while (dir != null) {
      if (File(dir, ".git").exists()) return dir
      dir = dir.parentFile
    }
    return null
  }

  /**
   * Runs `git status --porcelain` and returns a path → status map.
   * Paths in the map are absolute canonical paths.
   */
  fun loadStatus(projectDir: File): GitStatusSnapshot {
    val root = findGitRoot(projectDir) ?: return GitStatusSnapshot.EMPTY

    return try {
      val process = ProcessBuilder("git", "status", "--porcelain", "-u")
        .directory(root)
        .redirectErrorStream(true)
        .start()

      val output = process.inputStream.bufferedReader().readText()
      val code = process.waitFor()
      if (code != 0) {
        Logger.warn("git status exited with $code")
        return GitStatusSnapshot(root, emptyMap())
      }

      val map = mutableMapOf<String, GitFileStatus>()
      for (line in output.lineSequence()) {
        if (line.length < 4) continue
        val xy = line.substring(0, 2)
        // Path may be quoted; handle "renamed" as "old -> new"
        val pathPart = line.substring(3).trim().removeSurrounding("\"")
        val relativePath = if (" -> " in pathPart) {
          pathPart.substringAfter(" -> ")
        } else {
          pathPart
        }
        val abs = File(root, relativePath).canonicalPath
        map[abs] = decodeStatus(xy)
      }
      GitStatusSnapshot(root, map)
    } catch (e: Exception) {
      Logger.warn("Failed to run git status: ${e.message}")
      GitStatusSnapshot(root, emptyMap())
    }
  }

  private fun decodeStatus(xy: String): GitFileStatus {
    val x = xy[0]
    val y = xy[1]
    // Prefer working-tree letter if set, else index
    val letter = when {
      y != ' ' && y != '?' -> y
      x != ' ' && x != '?' -> x
      x == '?' || y == '?' -> '?'
      else -> x
    }
    return when (letter) {
      'M' -> GitFileStatus.MODIFIED
      'A' -> GitFileStatus.ADDED
      'D' -> GitFileStatus.DELETED
      'R', 'C' -> GitFileStatus.RENAMED
      '?' -> GitFileStatus.UNTRACKED
      else -> GitFileStatus.MODIFIED
    }
  }
}
