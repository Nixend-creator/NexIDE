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

package com.nexide.platform.ui.projectview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexide.platform.core.git.GitFileStatus
import com.nexide.platform.core.git.GitStatusService
import com.nexide.platform.core.git.GitStatusSnapshot
import com.nexide.platform.core.vfs.VirtualFile
import java.io.File

/**
 * Project view (file tree) with expand/collapse and Git status badges.
 */
@Composable
fun ProjectView(
  root: VirtualFile?,
  selectedFile: VirtualFile?,
  onFileSelected: (VirtualFile) -> Unit,
  /** Ancestor paths to expand (e.g. Reveal in Project View). */
  expandPaths: Set<String> = emptySet(),
  modifier: Modifier = Modifier
) {
  var expandedPaths by remember { mutableStateOf(setOf<String>()) }
  var gitSnapshot by remember { mutableStateOf(GitStatusSnapshot.EMPTY) }
  var refreshTick by remember { mutableStateOf(0) }

  LaunchedEffect(expandPaths) {
    if (expandPaths.isNotEmpty()) {
      expandedPaths = expandedPaths + expandPaths
    }
  }

  // Load git status when root changes or user refreshes
  LaunchedEffect(root?.path, refreshTick) {
    val path = root?.path
    gitSnapshot = if (path != null) {
      GitStatusService.loadStatus(File(path))
    } else {
      GitStatusSnapshot.EMPTY
    }
  }

  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Project",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f).padding(start = 4.dp)
      )
      TextButton(onClick = { refreshTick++ }) {
        Text("↻", style = MaterialTheme.typography.labelLarge)
      }
    }

    if (root == null || !root.isValid) {
      Text(
        text = "No project open",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(12.dp)
      )
      return
    }

    if (expandedPaths.isEmpty()) {
      expandedPaths = setOf(root.path)
    }

    val entries = remember(root, expandedPaths) {
      buildVisibleEntries(root, expandedPaths)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(entries, key = { it.file.path }) { entry ->
        val isExpanded = entry.file.path in expandedPaths
        val status = gitSnapshot.statusFor(entry.file.path)
        FileTreeItem(
          entry = entry,
          isSelected = selectedFile?.path == entry.file.path,
          isExpanded = isExpanded,
          gitStatus = status,
          onClick = {
            if (entry.file.isDirectory) {
              expandedPaths = if (isExpanded) {
                expandedPaths - entry.file.path
              } else {
                expandedPaths + entry.file.path
              }
            } else {
              onFileSelected(entry.file)
            }
          }
        )
      }
    }
  }
}

private data class TreeEntry(
  val file: VirtualFile,
  val depth: Int,
  val hasChildren: Boolean
)

private fun buildVisibleEntries(
  root: VirtualFile,
  expandedPaths: Set<String>
): List<TreeEntry> {
  val result = mutableListOf<TreeEntry>()

  fun visit(dir: VirtualFile, depth: Int) {
    if (!dir.isDirectory) return
    val children = dir.getChildren()
      .filter { child ->
        !child.name.startsWith(".") && child.name !in SKIP_DIRS
      }
      .sortedWith(compareBy<VirtualFile> { !it.isDirectory }.thenBy { it.name.lowercase() })

    for (child in children) {
      val childHasChildren = child.isDirectory && child.getChildren().any { c ->
        !c.name.startsWith(".") && c.name !in SKIP_DIRS
      }
      result.add(TreeEntry(child, depth, childHasChildren))
      if (child.isDirectory && child.path in expandedPaths) {
        visit(child, depth + 1)
      }
    }
  }

  val rootHasChildren = root.getChildren().any { c ->
    !c.name.startsWith(".") && c.name !in SKIP_DIRS
  }
  result.add(TreeEntry(root, 0, rootHasChildren))
  if (root.path in expandedPaths) {
    visit(root, 1)
  }

  return result
}

private val SKIP_DIRS = setOf("build", "out", ".gradle", ".git", "node_modules")

@Composable
private fun FileTreeItem(
  entry: TreeEntry,
  isSelected: Boolean,
  isExpanded: Boolean,
  gitStatus: GitFileStatus,
  onClick: () -> Unit
) {
  val background = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer
  } else {
    MaterialTheme.colorScheme.surfaceVariant
  }

  val expandIcon = when {
    !entry.file.isDirectory -> "  "
    isExpanded -> "▼"
    else -> "▶"
  }

  val icon = when {
    entry.file.isDirectory && isExpanded -> "📂"
    entry.file.isDirectory -> "📁"
    entry.file.name.endsWith(".kt") || entry.file.name.endsWith(".kts") -> "🅺"
    entry.file.name.endsWith(".java") -> "🅹"
    else -> "📄"
  }

  val (badge, badgeColor) = when (gitStatus) {
    GitFileStatus.MODIFIED -> "M" to Color(0xFFE2C08D)
    GitFileStatus.ADDED -> "A" to Color(0xFF89D185)
    GitFileStatus.UNTRACKED -> "?" to Color(0xFF89D185)
    GitFileStatus.DELETED -> "D" to Color(0xFFF14C4C)
    GitFileStatus.RENAMED -> "R" to Color(0xFF73C1DE)
    GitFileStatus.CLEAN -> "" to Color.Transparent
  }

  val nameColor = when {
    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
    gitStatus == GitFileStatus.MODIFIED -> Color(0xFFE2C08D)
    gitStatus == GitFileStatus.ADDED || gitStatus == GitFileStatus.UNTRACKED -> Color(0xFF89D185)
    gitStatus == GitFileStatus.DELETED -> Color(0xFFF14C4C)
    else -> MaterialTheme.colorScheme.onSurface
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(background)
      .clickable(onClick = onClick)
      .padding(start = (8 + entry.depth * 16).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = expandIcon,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.width(14.dp)
    )
    Text(text = icon, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = entry.file.name,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = nameColor,
      modifier = Modifier.weight(1f)
    )
    if (badge.isNotEmpty()) {
      Text(
        text = badge,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = badgeColor
      )
    }
  }
}
