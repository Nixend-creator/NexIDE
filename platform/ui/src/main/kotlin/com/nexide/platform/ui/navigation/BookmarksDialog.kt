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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexide.platform.core.Bookmark
import java.io.File

@Composable
fun BookmarksDialog(
  bookmarks: List<Bookmark>,
  onNavigate: (Bookmark) -> Unit,
  onRemove: (Bookmark) -> Unit,
  onClear: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(480.dp)
        .heightIn(max = 420.dp)
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(12.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Bookmarks", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        if (bookmarks.isNotEmpty()) {
          TextButton(onClick = onClear) { Text("Clear") }
        }
        TextButton(onClick = onDismiss) { Text("Close") }
      }
      if (bookmarks.isEmpty()) {
        Text(
          "No bookmarks. Use F11 to toggle on the current line.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(8.dp)
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
          items(bookmarks, key = { it.key }) { bm ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(bm) }
                .padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  "${File(bm.filePath).name}:${bm.line}",
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  bm.filePath,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
              TextButton(onClick = { onRemove(bm) }) { Text("×") }
            }
          }
        }
      }
    }
  }
}
