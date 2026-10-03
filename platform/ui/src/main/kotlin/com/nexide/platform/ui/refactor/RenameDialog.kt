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

package com.nexide.platform.ui.refactor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexide.platform.core.refactor.RenameOccurrence
import com.nexide.platform.core.refactor.RenamePreview
import com.nexide.platform.core.refactor.RenameRefactoring
import java.io.File

/**
 * Rename symbol dialog with preview of occurrences.
 */
@Composable
fun RenameDialog(
  projectRoot: File,
  oldName: String,
  onBeforeApply: () -> Unit = {},
  onApplied: (filesChanged: Int, newName: String) -> Unit,
  onDismiss: () -> Unit
) {
  var newName by remember { mutableStateOf(oldName) }
  var preview by remember {
    mutableStateOf(
      RenameRefactoring.preview(projectRoot, oldName, oldName)
    )
  }

  fun refreshPreview(name: String) {
    newName = name
    if (RenameRefactoring.isValidIdentifier(name) && name != oldName) {
      preview = RenameRefactoring.preview(projectRoot, oldName, name)
    } else if (name == oldName) {
      preview = RenamePreview(oldName, name, emptyList())
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(16.dp)
        .width(440.dp)
        .heightIn(max = 480.dp)
    ) {
      Text("Rename", style = MaterialTheme.typography.titleMedium)
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Rename '$oldName' in project (text-based)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.height(8.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("New name:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(80.dp))
        BasicTextField(
          value = newName,
          onValueChange = { refreshPreview(it) },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
          ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(8.dp)
        )
      }

      val valid = RenameRefactoring.isValidIdentifier(newName) && newName != oldName
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = if (!valid && newName != oldName) {
          "Invalid identifier"
        } else {
          "${preview.occurrences.size} occurrence(s) in ${preview.occurrences.map { it.filePath }.toSet().size} file(s)"
        },
        style = MaterialTheme.typography.labelMedium,
        color = if (!valid && newName != oldName) {
          MaterialTheme.colorScheme.error
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }
      )

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 220.dp)
          .padding(vertical = 6.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        items(
          preview.occurrences.take(80),
          key = { "${it.filePath}:${it.line}:${it.column}" }
        ) { occ: RenameOccurrence ->
          Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
              text = "${File(occ.filePath).name}:${occ.line}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary
            )
            Text(
              text = occ.lineText,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text("Cancel") }
        TextButton(
          onClick = {
            onBeforeApply()
            val p = RenameRefactoring.preview(projectRoot, oldName, newName)
            val n = RenameRefactoring.apply(p)
            onApplied(n, newName)
            onDismiss()
          },
          enabled = valid && preview.occurrences.isNotEmpty()
        ) {
          Text("Refactor")
        }
      }
    }
  }
}
