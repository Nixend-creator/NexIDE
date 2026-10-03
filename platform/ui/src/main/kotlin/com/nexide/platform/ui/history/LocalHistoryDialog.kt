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

package com.nexide.platform.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nexide.platform.core.history.HistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Timeline of local history snapshots for a file; restore selected entry.
 */
@Composable
fun LocalHistoryDialog(
  fileName: String,
  entries: List<HistoryEntry>,
  onRestore: (indexFromNewest: Int) -> Unit,
  onDismiss: () -> Unit
) {
  var selected by remember { mutableStateOf(0) }
  val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(16.dp)
        .width(520.dp)
        .heightIn(max = 480.dp)
    ) {
      Text(
        "Local History — $fileName",
        style = MaterialTheme.typography.titleMedium
      )
      Spacer(modifier = Modifier.height(8.dp))
      if (entries.isEmpty()) {
        Text(
          "No history snapshots yet. Save the file to create one.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      } else {
        Row(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
          LazyColumn(modifier = Modifier.width(200.dp).padding(end = 8.dp)) {
            itemsIndexed(entries) { index, entry ->
              val label = "${fmt.format(Date(entry.timestampMs))} · ${entry.label}"
              Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (index == selected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { selected = index }
                  .background(
                    if (index == selected) {
                      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                      MaterialTheme.colorScheme.surface
                    }
                  )
                  .padding(6.dp)
              )
            }
          }
          val preview = entries.getOrNull(selected)?.content.orEmpty()
          Text(
            text = preview.take(4000).ifEmpty { "(empty)" },
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              lineHeight = 14.sp
            ),
            modifier = Modifier
              .weight(1f)
              .heightIn(max = 360.dp)
              .verticalScroll(rememberScrollState())
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
              .padding(8.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("Close") }
        TextButton(
          onClick = { onRestore(selected) },
          enabled = entries.isNotEmpty()
        ) {
          Text("Restore")
        }
      }
    }
  }
}
