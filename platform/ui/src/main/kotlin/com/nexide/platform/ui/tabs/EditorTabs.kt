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

package com.nexide.platform.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexide.platform.editor.Editor

/**
 * Simple horizontal tab bar for open editors.
 */
@Composable
fun EditorTabs(
  editors: List<Editor>,
  activeEditor: Editor?,
  onSelect: (Editor) -> Unit,
  onClose: (Editor) -> Unit,
  pinnedPaths: Set<String> = emptySet(),
  onTogglePin: (Editor) -> Unit = {},
  modifier: Modifier = Modifier
) {
  if (editors.isEmpty()) return

  val scrollState = rememberScrollState()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .horizontalScroll(scrollState)
      .padding(horizontal = 4.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    val ordered = editors.sortedBy { if (it.file.path in pinnedPaths) 0 else 1 }
    ordered.forEach { editor ->
      val isActive = editor === activeEditor
      val pinned = editor.file.path in pinnedPaths
      TabItem(
        title = (if (pinned) "📌 " else "") + editor.file.name + if (editor.isModified) " *" else "",
        isActive = isActive,
        onClick = { onSelect(editor) },
        onClose = { if (!pinned) onClose(editor) },
        onTogglePin = { onTogglePin(editor) }
      )
    }
  }
}

@Composable
private fun TabItem(
  title: String,
  isActive: Boolean,
  onClick: () -> Unit,
  onClose: () -> Unit,
  onTogglePin: () -> Unit = {}
) {
  val background = if (isActive) {
    MaterialTheme.colorScheme.surface
  } else {
    MaterialTheme.colorScheme.surfaceVariant
  }
  val textColor = if (isActive) {
    MaterialTheme.colorScheme.onSurface
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }

  Row(
    modifier = Modifier
      .padding(horizontal = 2.dp)
      .background(background)
      .clickable(onClick = onClick)
      .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
      color = textColor,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = "📌",
      style = MaterialTheme.typography.labelSmall,
      color = textColor,
      modifier = Modifier
        .clickable(onClick = onTogglePin)
        .padding(horizontal = 2.dp)
    )
    Text(
      text = "×",
      style = MaterialTheme.typography.labelLarge,
      color = textColor,
      modifier = Modifier
        .clickable(onClick = onClose)
        .padding(horizontal = 4.dp)
    )
  }
}
