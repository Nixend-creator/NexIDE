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
 * A single changed path from `git status --porcelain`.
 */
data class GitChange(
  /** Path relative to repo root. */
  val relativePath: String,
  val absolutePath: String,
  val status: GitFileStatus,
  /** True if staged in the index (first column of porcelain). */
  val staged: Boolean
)

data class GitCommandResult(
  val success: Boolean,
  val output: String,
  val exitCode: Int
)

/**
 * Higher-level git operations used by the IDE UI.
 */
object GitOperations {

  fun listChanges(projectDir: File): List<GitChange> {
    val root = GitStatusService.findGitRoot(projectDir) ?: return emptyList()
    val result = runGit(root, listOf("status", "--porcelain", "-u"))
    if (!result.success) return emptyList()

    val changes = mutableListOf<GitChange>()
    for (line in result.output.lineSequence()) {
      if (line.length < 4) continue
      val xy = line.substring(0, 2)
      val pathPart = line.substring(3).trim().removeSurrounding("\"")
      val relativePath = if (" -> " in pathPart) {
        pathPart.substringAfter(" -> ")
      } else {
        pathPart
      }
      val abs = File(root, relativePath).canonicalPath
      val status = decodeStatus(xy)
      val staged = xy[0] != ' ' && xy[0] != '?'
      changes.add(GitChange(relativePath, abs, status, staged))
    }
    return changes
  }

  fun stage(projectDir: File, relativePaths: List<String>): GitCommandResult {
    val root = GitStatusService.findGitRoot(projectDir)
      ?: return GitCommandResult(false, "Not a git repository", -1)
    if (relativePaths.isEmpty()) return GitCommandResult(true, "", 0)
    return runGit(root, listOf("add", "--") + relativePaths)
  }

  fun unstage(projectDir: File, relativePaths: List<String>): GitCommandResult {
    val root = GitStatusService.findGitRoot(projectDir)
      ?: return GitCommandResult(false, "Not a git repository", -1)
    if (relativePaths.isEmpty()) return GitCommandResult(true, "", 0)
    // git restore --staged <paths>
    return runGit(root, listOf("restore", "--staged", "--") + relativePaths)
  }

  fun commit(projectDir: File, message: String): GitCommandResult {
    val root = GitStatusService.findGitRoot(projectDir)
      ?: return GitCommandResult(false, "Not a git repository", -1)
    if (message.isBlank()) {
      return GitCommandResult(false, "Commit message is empty", -1)
    }
    return runGit(root, listOf("commit", "-m", message))
  }

  fun currentBranch(projectDir: File): String? {
    val root = GitStatusService.findGitRoot(projectDir) ?: return null
    val result = runGit(root, listOf("rev-parse", "--abbrev-ref", "HEAD"))
    return if (result.success) result.output.trim().ifEmpty { null } else null
  }

  private fun runGit(repoRoot: File, args: List<String>): GitCommandResult {
    return try {
      val process = ProcessBuilder(listOf("git") + args)
        .directory(repoRoot)
        .redirectErrorStream(true)
        .start()
      val output = process.inputStream.bufferedReader().readText()
      val code = process.waitFor()
      if (code != 0) {
        Logger.warn("git ${args.joinToString(" ")} exited $code: $output")
      }
      GitCommandResult(code == 0, output, code)
    } catch (e: Exception) {
      Logger.error("git failed: ${e.message}", e)
      GitCommandResult(false, e.message ?: "unknown error", -1)
    }
  }

  private fun decodeStatus(xy: String): GitFileStatus {
    val x = xy[0]
    val y = xy[1]
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
