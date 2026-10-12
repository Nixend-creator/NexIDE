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

package com.nexide.platform.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexide.platform.core.settings.NexIdeSettings

/**
 * Basic settings dialog. Applies values to [NexIdeSettings] and calls [onApplied].
 */
@Composable
fun SettingsDialog(
  onDismiss: () -> Unit,
  onApplied: () -> Unit
) {
  var fontSize by remember { mutableStateOf(NexIdeSettings.editorFontSize) }
  var showLineNumbers by remember { mutableStateOf(NexIdeSettings.showLineNumbers) }
  var tabSize by remember { mutableStateOf(NexIdeSettings.tabSize) }
  var softWrap by remember { mutableStateOf(NexIdeSettings.softWrap) }
  var confirmExit by remember { mutableStateOf(NexIdeSettings.confirmExitOnUnsaved) }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(20.dp)
        .width(360.dp)
    ) {
      Text(
        text = "Settings",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.padding(10.dp))

      // Font size
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Editor font size",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f)
        )
        TextButton(
          onClick = { fontSize = (fontSize - 1).coerceAtLeast(10) },
          enabled = fontSize > 10
        ) { Text("−") }
        Text(
          text = "$fontSize",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(horizontal = 8.dp)
        )
        TextButton(
          onClick = { fontSize = (fontSize + 1).coerceAtMost(32) },
          enabled = fontSize < 32
        ) { Text("+") }
      }

      // Tab size
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Tab size",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f)
        )
        TextButton(
          onClick = { tabSize = (tabSize - 1).coerceAtLeast(1) },
          enabled = tabSize > 1
        ) { Text("−") }
        Text(
          text = "$tabSize",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(horizontal = 8.dp)
        )
        TextButton(
          onClick = { tabSize = (tabSize + 1).coerceAtMost(8) },
          enabled = tabSize < 8
        ) { Text("+") }
      }

      // Line numbers
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Show line numbers",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f)
        )
        Checkbox(
          checked = showLineNumbers,
          onCheckedChange = { showLineNumbers = it }
        )
      }

      // Soft wrap
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Soft wrap",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f)
        )
        Checkbox(
          checked = softWrap,
          onCheckedChange = { softWrap = it }
        )
      }

      // Confirm exit
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Confirm exit if unsaved files",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f)
        )
        Checkbox(
          checked = confirmExit,
          onCheckedChange = { confirmExit = it }
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
        TextButton(
          onClick = {
            NexIdeSettings.editorFontSize = fontSize
            NexIdeSettings.showLineNumbers = showLineNumbers
            NexIdeSettings.tabSize = tabSize
            NexIdeSettings.softWrap = softWrap
            NexIdeSettings.confirmExitOnUnsaved = confirmExit
            NexIdeSettings.save()
            onApplied()
            onDismiss()
          }
        ) {
          Text("Apply")
        }
      }
    }
  }
}
