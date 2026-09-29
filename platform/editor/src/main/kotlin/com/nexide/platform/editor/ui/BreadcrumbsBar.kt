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

package com.nexide.platform.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.psi.structure.FileStructureExtractor
import com.nexide.platform.psi.structure.StructureItem
import com.nexide.platform.psi.structure.StructureKind

/**
 * Breadcrumb trail: project-relative path segments + nearest structure symbol.
 */
@Composable
fun BreadcrumbsBar(
  projectRootPath: String?,
  file: VirtualFile?,
  documentText: String?,
  language: String?,
  caretLine: Int,
  onNavigateToLine: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  if (file == null) return

  val clipboard = LocalClipboardManager.current
  val segments = remember(file.path, projectRootPath) {
    pathSegments(projectRootPath, file.path)
  }
  val structureTrail = remember(documentText, language, caretLine) {
    structureBreadcrumb(documentText, language, caretLine)
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    segments.forEachIndexed { index, segment ->
      if (index > 0) {
        Text(
          text = " / ",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      val pathUpTo = segments.take(index + 1).joinToString("/")
      Text(
        text = segment,
        style = MaterialTheme.typography.labelSmall,
        color = if (index == segments.lastIndex) {
          MaterialTheme.colorScheme.onSurface
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.clickable {
          clipboard.setText(AnnotatedString(pathUpTo))
        }
      )
    }

    structureTrail.forEach { item ->
      Text(
        text = " › ",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "${kindPrefix(item.kind)}${item.name}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        modifier = Modifier.clickable { onNavigateToLine(item.line) }
      )
    }
  }
}

private fun pathSegments(projectRoot: String?, absolutePath: String): List<String> {
  val normalized = absolutePath.replace('\\', '/')
  val root = projectRoot?.replace('\\', '/')?.trimEnd('/')
  val relative = if (root != null && normalized.startsWith(root)) {
    normalized.removePrefix(root).trimStart('/')
  } else {
    normalized.substringAfterLast('/')
  }
  return if (relative.isEmpty()) {
    listOf(absolutePath.substringAfterLast('/'))
  } else {
    relative.split('/').filter { it.isNotEmpty() }
  }
}

/**
 * Structure symbols that contain [caretLine] (outermost type + nearest member).
 */
private fun structureBreadcrumb(
  text: String?,
  language: String?,
  caretLine: Int
): List<StructureItem> {
  if (text == null || language == null || caretLine < 1) return emptyList()
  val items = FileStructureExtractor.extract(text, language)
  if (items.isEmpty()) return emptyList()

  val types = items.filter {
    it.kind == StructureKind.CLASS ||
      it.kind == StructureKind.OBJECT ||
      it.kind == StructureKind.INTERFACE ||
      it.kind == StructureKind.ENUM
  }
  val members = items.filter {
    it.kind == StructureKind.FUNCTION || it.kind == StructureKind.PROPERTY
  }

  val result = mutableListOf<StructureItem>()
  // Nearest type whose line <= caret
  types.filter { it.line <= caretLine }.maxByOrNull { it.line }?.let { result.add(it) }
  // Nearest member whose line <= caret
  members.filter { it.line <= caretLine }.maxByOrNull { it.line }?.let { result.add(it) }
  return result
}

private fun kindPrefix(kind: StructureKind): String = when (kind) {
  StructureKind.CLASS -> "C:"
  StructureKind.OBJECT -> "O:"
  StructureKind.INTERFACE -> "I:"
  StructureKind.FUNCTION -> "f:"
  StructureKind.PROPERTY -> "p:"
  StructureKind.ENUM -> "E:"
  StructureKind.OTHER -> ""
}
