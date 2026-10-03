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

package com.nexide.platform.ui.structure

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexide.platform.psi.structure.FileStructureExtractor
import com.nexide.platform.psi.structure.StructureItem
import com.nexide.platform.psi.structure.StructureKind

/**
 * Structure tool window for the active editor file.
 */
@Composable
fun StructureToolWindow(
  fileName: String?,
  text: String?,
  language: String?,
  visible: Boolean,
  onClose: () -> Unit,
  onNavigate: (line: Int) -> Unit,
  modules: List<String> = emptyList(),
  onModuleClick: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  if (!visible) return

  val items = remember(text, language) {
    if (text == null || language == null) emptyList()
    else FileStructureExtractor.extract(text, language)
  }

  Column(
    modifier = modifier
      .width(240.dp)
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
        text = "Structure",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.weight(1f)
      )
      TextButton(onClick = onClose) {
        Text("×", style = MaterialTheme.typography.labelLarge)
      }
    }

    if (fileName == null || text == null) {
      Text(
        text = "No file open",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(12.dp)
      )
      return
    }

    if (items.isEmpty()) {
      Text(
        text = "No symbols found",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(12.dp)
      )
      return
    }

    if (modules.isNotEmpty()) {
      Text(
        text = "Modules (${modules.size})",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
      )
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(0.35f)
          .background(MaterialTheme.colorScheme.surface)
      ) {
        items(modules) { mod ->
          Text(
            text = mod,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onModuleClick(mod) }
              .padding(horizontal = 10.dp, vertical = 3.dp)
          )
        }
      }
      Text(
        text = fileName?.let { "File: $it" } ?: "File structure",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
      )
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      items(items, key = { "${it.kind}:${it.name}:${it.line}" }) { item ->
        StructureRow(item = item, onClick = { onNavigate(item.line) })
      }
    }
  }
}

@Composable
private fun StructureRow(
  item: StructureItem,
  onClick: () -> Unit
) {
  val icon = when (item.kind) {
    StructureKind.CLASS -> "C"
    StructureKind.OBJECT -> "O"
    StructureKind.INTERFACE -> "I"
    StructureKind.FUNCTION -> "f"
    StructureKind.PROPERTY -> "p"
    StructureKind.ENUM -> "E"
    StructureKind.OTHER -> "•"
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = icon,
      style = MaterialTheme.typography.labelMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.width(18.dp)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = item.name,
      style = MaterialTheme.typography.bodySmall,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    Text(
      text = ":${item.line}",
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
