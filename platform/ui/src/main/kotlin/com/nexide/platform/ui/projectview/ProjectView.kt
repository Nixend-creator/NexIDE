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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexide.platform.core.vfs.VirtualFile

/**
 * Simple project view (file tree) backed by VFS.
 */
@Composable
fun ProjectView(
  root: VirtualFile?,
  selectedFile: VirtualFile?,
  onFileSelected: (VirtualFile) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    // Header
    Text(
      text = "Project",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 10.dp)
    )

    if (root == null || !root.isValid) {
      Text(
        text = "No project open",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(12.dp)
      )
      return
    }

    val entries = remember(root) { buildTreeEntries(root, depth = 0) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(entries, key = { it.file.path }) { entry ->
        FileTreeItem(
          entry = entry,
          isSelected = selectedFile?.path == entry.file.path,
          onClick = {
            if (!entry.file.isDirectory) {
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
  val depth: Int
)

private fun buildTreeEntries(dir: VirtualFile, depth: Int, maxDepth: Int = 4): List<TreeEntry> {
  if (!dir.isDirectory || depth > maxDepth) return emptyList()

  val result = mutableListOf<TreeEntry>()
  val children = dir.getChildren()
    .sortedWith(compareBy<VirtualFile> { !it.isDirectory }.thenBy { it.name.lowercase() })

  for (child in children) {
    // Skip hidden / build dirs for cleaner demo
    if (child.name.startsWith(".") || child.name in SKIP_DIRS) continue

    result.add(TreeEntry(child, depth))
    if (child.isDirectory) {
      result.addAll(buildTreeEntries(child, depth + 1, maxDepth))
    }
  }
  return result
}

private val SKIP_DIRS = setOf("build", "out", ".gradle", ".git", "node_modules")

@Composable
private fun FileTreeItem(
  entry: TreeEntry,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val background = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer
  } else {
    MaterialTheme.colorScheme.surfaceVariant
  }

  val icon = when {
    entry.file.isDirectory -> "📁"
    entry.file.name.endsWith(".kt") -> "🅺"
    entry.file.name.endsWith(".java") -> "🅹"
    else -> "📄"
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(background)
      .clickable(onClick = onClick)
      .padding(start = (12 + entry.depth * 16).dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = icon, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = entry.file.name,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      color = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
      } else {
        MaterialTheme.colorScheme.onSurface
      }
    )
  }
}
