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

package com.nexide.platform.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Result of the "unsaved changes" confirmation dialog.
 */
enum class CloseConfirmResult {
  SAVE,
  DONT_SAVE,
  CANCEL
}

/**
 * Asks what to do with unsaved changes when closing a tab.
 */
@Composable
fun ConfirmCloseDialog(
  fileName: String,
  onResult: (CloseConfirmResult) -> Unit
) {
  Dialog(onDismissRequest = { onResult(CloseConfirmResult.CANCEL) }) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(20.dp)
        .width(360.dp)
    ) {
      Text(
        text = "Unsaved changes",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.padding(6.dp))
      Text(
        text = "\"$fileName\" has unsaved changes. Save before closing?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = { onResult(CloseConfirmResult.CANCEL) }) {
          Text("Cancel")
        }
        TextButton(onClick = { onResult(CloseConfirmResult.DONT_SAVE) }) {
          Text("Don't Save")
        }
        TextButton(onClick = { onResult(CloseConfirmResult.SAVE) }) {
          Text("Save")
        }
      }
    }
  }
}
