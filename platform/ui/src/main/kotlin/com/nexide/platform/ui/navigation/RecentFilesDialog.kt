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

package com.nexide.platform.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.io.File

@Composable
fun RecentFilesDialog(
  paths: List<String>,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var filter by remember { mutableStateOf("") }
  var selectedIndex by remember { mutableStateOf(0) }
  val filtered = remember(paths, filter) {
    val q = filter.lowercase()
    if (q.isEmpty()) paths
    else paths.filter {
      it.lowercase().contains(q) || File(it).name.lowercase().contains(q)
    }
  }
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) { focusRequester.requestFocus() }
  LaunchedEffect(filtered) {
    selectedIndex = selectedIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(480.dp)
        .heightIn(max = 420.dp)
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(12.dp)
        .onPreviewKeyEvent { event ->
          if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
          when (event.key) {
            Key.DirectionDown -> {
              if (filtered.isNotEmpty()) {
                selectedIndex = (selectedIndex + 1) % filtered.size
              }
              true
            }
            Key.DirectionUp -> {
              if (filtered.isNotEmpty()) {
                selectedIndex = (selectedIndex - 1 + filtered.size) % filtered.size
              }
              true
            }
            Key.Enter -> {
              filtered.getOrNull(selectedIndex)?.let { onSelect(it) }
              true
            }
            Key.Escape -> {
              onDismiss()
              true
            }
            else -> false
          }
        }
    ) {
      Text("Recent Files", style = MaterialTheme.typography.titleSmall)
      BasicTextField(
        value = filter,
        onValueChange = {
          filter = it
          selectedIndex = 0
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
          .padding(8.dp)
          .focusRequester(focusRequester)
      )
      if (filtered.isEmpty()) {
        Text(
          "No recent files",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(8.dp)
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
          itemsIndexed(filtered, key = { _, p -> p }) { index, path ->
            val bg = if (index == selectedIndex) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surface
            }
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable { onSelect(path) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Text(
                File(path).name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                path,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}
