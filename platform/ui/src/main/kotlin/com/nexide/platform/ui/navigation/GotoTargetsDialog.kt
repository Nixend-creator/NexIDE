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
import com.nexide.platform.core.navigation.NavigationTarget
import java.io.File

/**
 * Chooser when Go to Declaration finds multiple targets.
 */
@Composable
fun GotoTargetsDialog(
  symbol: String,
  targets: List<NavigationTarget>,
  title: String = "Go to Declaration",
  onSelect: (NavigationTarget) -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(16.dp)
        .width(480.dp)
        .heightIn(max = 400.dp)
    ) {
      Text(
        text = "$title — $symbol (${targets.size})",
        style = MaterialTheme.typography.titleSmall
      )
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 300.dp)
          .padding(vertical = 8.dp)
      ) {
        items(targets, key = { "${it.filePath}:${it.line}:${it.kind}:${it.matchText}" }) { target ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                onSelect(target)
                onDismiss()
              }
              .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (target.kind == "declaration") "D" else "U",
              style = MaterialTheme.typography.labelMedium,
              color = if (target.kind == "declaration") {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.width(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "${File(target.filePath).name}:${target.line}",
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                text = target.matchText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
      TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
        Text("Cancel")
      }
    }
  }
}
