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

package com.nexide.platform.ui.run

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexide.platform.core.run.RunConfiguration
import com.nexide.platform.core.run.RunConfigurationManager
import com.nexide.platform.core.run.RunConfigurationType

/**
 * Dialog to list / add / select run configurations.
 */
@Composable
fun RunConfigDialog(
  projectPath: String,
  manager: RunConfigurationManager,
  onDismiss: () -> Unit,
  onRun: (RunConfiguration) -> Unit,
  onDebug: (RunConfiguration) -> Unit
) {
  var configs by remember { mutableStateOf(manager.configurations) }
  var selectedName by remember { mutableStateOf(manager.selected?.name) }

  var name by remember { mutableStateOf("") }
  var taskOrMain by remember { mutableStateOf("") }
  var isApplication by remember { mutableStateOf(false) }

  fun refresh() {
    configs = manager.configurations
    selectedName = manager.selected?.name
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(16.dp)
        .width(440.dp)
        .heightIn(max = 520.dp)
    ) {
      Text(
        text = "Run Configurations",
        style = MaterialTheme.typography.titleMedium
      )
      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(180.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant)
      ) {
        items(configs, key = { it.name }) { cfg ->
          val selected = cfg.name == selectedName
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
              )
              .clickable {
                selectedName = cfg.name
                manager.selected = cfg
              }
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = if (cfg.type == RunConfigurationType.GRADLE) "G" else "A",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.width(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(cfg.name, style = MaterialTheme.typography.bodyMedium)
              Text(
                text = if (cfg.type == RunConfigurationType.GRADLE) {
                  "gradle ${cfg.gradleTask}"
                } else {
                  cfg.mainClass
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      Text("New configuration", style = MaterialTheme.typography.labelMedium)
      Spacer(modifier = Modifier.height(4.dp))

      FieldRow("Name") {
        BasicTextField(
          value = name,
          onValueChange = { name = it },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(6.dp)
        )
      }
      FieldRow(if (isApplication) "Main class" else "Gradle task") {
        BasicTextField(
          value = taskOrMain,
          onValueChange = { taskOrMain = it },
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
            .padding(6.dp)
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { isApplication = !isApplication }) {
          Text(if (isApplication) "Type: Application" else "Type: Gradle")
        }
        TextButton(
          onClick = {
            if (name.isBlank() || taskOrMain.isBlank()) return@TextButton
            val cfg = if (isApplication) {
              RunConfiguration(
                name = name.trim(),
                type = RunConfigurationType.APPLICATION,
                mainClass = taskOrMain.trim(),
                workingDir = projectPath
              )
            } else {
              RunConfiguration(
                name = name.trim(),
                type = RunConfigurationType.GRADLE,
                gradleTask = taskOrMain.trim(),
                workingDir = projectPath
              )
            }
            manager.add(cfg)
            manager.selected = cfg
            name = ""
            taskOrMain = ""
            refresh()
          }
        ) {
          Text("Add")
        }
        TextButton(
          onClick = {
            selectedName?.let {
              manager.remove(it)
              refresh()
            }
          },
          enabled = selectedName != null
        ) {
          Text("Remove")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = onDismiss) { Text("Close") }
        TextButton(
          onClick = {
            manager.selected?.let { onDebug(it) }
          },
          enabled = manager.selected != null
        ) { Text("Debug") }
        TextButton(
          onClick = {
            manager.selected?.let { onRun(it) }
          },
          enabled = manager.selected != null
        ) { Text("Run") }
      }
    }
  }
}

@Composable
private fun FieldRow(label: String, content: @Composable () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(90.dp))
    content()
  }
}
