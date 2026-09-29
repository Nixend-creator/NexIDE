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

package com.nexide.platform.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexide.platform.core.debug.Breakpoint
import com.nexide.platform.core.debug.BreakpointManager
import java.io.File

/**
 * Lists all breakpoints; click opens file:line via [onNavigate].
 */
@Composable
fun BreakpointsToolWindow(
  visible: Boolean,
  onClose: () -> Unit,
  onNavigate: (filePath: String, line: Int) -> Unit,
  modifier: Modifier = Modifier
) {
  if (!visible) return

  val manager = remember { BreakpointManager.getInstance() }
  var breakpoints by remember { mutableStateOf(manager.getAll()) }
  var tick by remember { mutableStateOf(0) }

  DisposableEffect(manager) {
    val listener = {
      tick++
      breakpoints = manager.getAll()
    }
    manager.addListener(listener)
    onDispose { manager.removeListener(listener) }
  }

  // Force refresh when tick changes
  @Suppress("UNUSED_EXPRESSION")
  tick
  breakpoints = manager.getAll()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 140.dp, max = 260.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Breakpoints",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = "${breakpoints.size}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      TextButton(
        onClick = { manager.clearAll() },
        enabled = breakpoints.isNotEmpty()
      ) {
        Text("Clear All", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(onClick = onClose) {
        Text("×", style = MaterialTheme.typography.labelLarge)
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      if (breakpoints.isEmpty()) {
        item {
          Text(
            text = "No breakpoints. Click the editor gutter to add one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
          )
        }
      }
      items(breakpoints, key = { it.key }) { bp ->
        BreakpointRow(
          breakpoint = bp,
          onClick = { onNavigate(bp.filePath, bp.line) },
          onRemove = { manager.remove(bp.filePath, bp.line) }
        )
      }
    }
  }
}

@Composable
private fun BreakpointRow(
  breakpoint: Breakpoint,
  onClick: () -> Unit,
  onRemove: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "●",
      color = Color(0xFFE53935),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.width(20.dp)
    )
    Text(
      text = "${File(breakpoint.filePath).name}:${breakpoint.line}",
      style = MaterialTheme.typography.bodySmall,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
    TextButton(onClick = onRemove) {
      Text("×", style = MaterialTheme.typography.labelMedium)
    }
  }
}
