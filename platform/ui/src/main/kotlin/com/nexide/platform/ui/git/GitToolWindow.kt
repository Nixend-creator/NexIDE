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

package com.nexide.platform.ui.git

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexide.platform.core.git.GitChange
import com.nexide.platform.core.git.GitFileStatus
import com.nexide.platform.core.git.GitOperations
import java.io.File
import java.util.concurrent.Executors
import javax.swing.SwingUtilities

/**
 * Git tool window: changes list, stage/unstage, commit, and diff preview.
 */
@Composable
fun GitToolWindow(
  projectRoot: String,
  visible: Boolean,
  onClose: () -> Unit,
  onStatusChanged: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  if (!visible) return

  var changes by remember { mutableStateOf<List<GitChange>>(emptyList()) }
  var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
  var focusedPath by remember { mutableStateOf<String?>(null) }
  var diffText by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }
  var branch by remember { mutableStateOf<String?>(null) }
  var syncSummary by remember { mutableStateOf<String?>(null) }
  var statusText by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  var refreshTick by remember { mutableStateOf(0) }
  val executor = remember { Executors.newSingleThreadExecutor() }
  val projectDir = remember(projectRoot) { File(projectRoot) }

  fun loadDiff(relativePath: String) {
    val change = changes.firstOrNull { it.relativePath == relativePath }
    val staged = change?.staged == true
    executor.execute {
      val result = GitOperations.diff(projectDir, relativePath, staged = staged)
      SwingUtilities.invokeLater {
        diffText = when {
          result.output.isNotBlank() -> result.output
          result.success -> "(no differences)"
          else -> result.output.ifBlank { "Failed to load diff" }
        }
      }
    }
  }

  fun refresh() {
    busy = true
    executor.execute {
      val list = GitOperations.listChanges(projectDir)
      val b = GitOperations.currentBranch(projectDir)
      val sync = GitOperations.remoteSyncSummary(projectDir)
      SwingUtilities.invokeLater {
        changes = list
        branch = b
        syncSummary = sync
        selected = selected.intersect(list.map { it.relativePath }.toSet())
        if (focusedPath != null && list.none { it.relativePath == focusedPath }) {
          focusedPath = null
          diffText = ""
        }
        busy = false
        statusText = if (list.isEmpty()) "No changes" else "${list.size} change(s)"
        onStatusChanged()
        focusedPath?.let { loadDiff(it) }
      }
    }
  }

  LaunchedEffect(projectRoot, refreshTick) {
    refresh()
  }

  fun runOp(label: String, block: () -> com.nexide.platform.core.git.GitCommandResult) {
    if (busy) return
    busy = true
    statusText = "$label…"
    executor.execute {
      val result = block()
      SwingUtilities.invokeLater {
        statusText = if (result.success) {
          "$label OK"
        } else {
          "$label failed: ${result.output.trim().take(120)}"
        }
        busy = false
        refreshTick++
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 220.dp, max = 380.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = buildString {
          append("Git")
          branch?.let { append(" ($it)") }
          syncSummary?.let { append(" $it") }
        },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = statusText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      TextButton(
        onClick = {
          runOp("Pull") { GitOperations.pull(projectDir) }
        },
        enabled = !busy
      ) {
        Text("Pull", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(
        onClick = {
          runOp("Push") { GitOperations.push(projectDir) }
        },
        enabled = !busy
      ) {
        Text("Push", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(onClick = { refreshTick++ }, enabled = !busy) {
        Text("↻", style = MaterialTheme.typography.labelLarge)
      }
      TextButton(onClick = onClose) {
        Text("×", style = MaterialTheme.typography.labelLarge)
      }
    }

    // List + Diff
    Row(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      // Changes list
      LazyColumn(
        modifier = Modifier
          .width(280.dp)
          .fillMaxHeight()
      ) {
        items(changes, key = { it.relativePath }) { change ->
          val isSelected = change.relativePath in selected
          val isFocused = change.relativePath == focusedPath
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                when {
                  isFocused -> MaterialTheme.colorScheme.secondaryContainer
                  isSelected -> MaterialTheme.colorScheme.primaryContainer
                  else -> MaterialTheme.colorScheme.surface
                }
              )
              .clickable {
                focusedPath = change.relativePath
                selected = setOf(change.relativePath)
                loadDiff(change.relativePath)
              }
              .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = statusBadge(change.status),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = statusColor(change.status),
              modifier = Modifier.width(18.dp)
            )
            Text(
              text = if (change.staged) "●" else "○",
              style = MaterialTheme.typography.labelSmall,
              color = if (change.staged) Color(0xFF89D185) else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.width(14.dp)
            )
            Text(
              text = change.relativePath,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      VerticalDivider()

      // Diff pane
      DiffPane(
        path = focusedPath,
        diffText = diffText,
        modifier = Modifier.weight(1f).fillMaxHeight()
      )
    }

    // Actions + commit message
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextButton(
        onClick = {
          val paths = selected.toList().ifEmpty { changes.map { it.relativePath } }
          runOp("Stage") { GitOperations.stage(projectDir, paths) }
        },
        enabled = !busy && changes.isNotEmpty()
      ) {
        Text("Stage", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(
        onClick = {
          val paths = selected.filter { path ->
            changes.any { it.relativePath == path && it.staged }
          }.ifEmpty {
            changes.filter { it.staged }.map { it.relativePath }
          }
          runOp("Unstage") { GitOperations.unstage(projectDir, paths) }
        },
        enabled = !busy && changes.any { it.staged }
      ) {
        Text("Unstage", style = MaterialTheme.typography.labelMedium)
      }
      Spacer(modifier = Modifier.width(8.dp))
      BasicTextField(
        value = message,
        onValueChange = { message = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .weight(1f)
          .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
          .padding(horizontal = 8.dp, vertical = 6.dp),
        decorationBox = { inner ->
          if (message.isEmpty()) {
            Text(
              "Commit message…",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          inner()
        }
      )
      TextButton(
        onClick = {
          val msg = message
          runOp("Commit") {
            val result = GitOperations.commit(projectDir, msg)
            if (result.success) {
              SwingUtilities.invokeLater { message = "" }
            }
            result
          }
        },
        enabled = !busy && message.isNotBlank() && changes.any { it.staged }
      ) {
        Text("Commit", style = MaterialTheme.typography.labelMedium)
      }
    }
  }
}

@Composable
private fun DiffPane(
  path: String?,
  diffText: String,
  modifier: Modifier = Modifier
) {
  val vScroll = rememberScrollState()
  val hScroll = rememberScrollState()

  Column(modifier = modifier.padding(8.dp)) {
    Text(
      text = path?.let { "Diff: $it" } ?: "Select a file to see diff",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.padding(2.dp))
    if (path != null) {
      val annotated = remember(diffText) { colorizeDiff(diffText) }
      Text(
        text = annotated,
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          lineHeight = 15.sp
        ),
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(vScroll)
          .horizontalScroll(hScroll)
      )
    }
  }
}

private fun colorizeDiff(text: String) = buildAnnotatedString {
  if (text.isEmpty()) {
    append("")
    return@buildAnnotatedString
  }
  for (line in text.lineSequence()) {
    val color = when {
      line.startsWith("+++") || line.startsWith("---") -> Color(0xFF73C1DE)
      line.startsWith("@@") -> Color(0xFFC586C0)
      line.startsWith("+") -> Color(0xFF89D185)
      line.startsWith("-") -> Color(0xFFF14C4C)
      line.startsWith("diff ") -> Color(0xFFE2C08D)
      else -> Color(0xFFCCCCCC)
    }
    withStyle(SpanStyle(color = color)) {
      append(line)
      append("\n")
    }
  }
}

private fun statusBadge(status: GitFileStatus): String = when (status) {
  GitFileStatus.MODIFIED -> "M"
  GitFileStatus.ADDED -> "A"
  GitFileStatus.UNTRACKED -> "?"
  GitFileStatus.DELETED -> "D"
  GitFileStatus.RENAMED -> "R"
  GitFileStatus.CLEAN -> " "
}

private fun statusColor(status: GitFileStatus): Color = when (status) {
  GitFileStatus.MODIFIED -> Color(0xFFE2C08D)
  GitFileStatus.ADDED, GitFileStatus.UNTRACKED -> Color(0xFF89D185)
  GitFileStatus.DELETED -> Color(0xFFF14C4C)
  GitFileStatus.RENAMED -> Color(0xFF73C1DE)
  GitFileStatus.CLEAN -> Color.Gray
}
