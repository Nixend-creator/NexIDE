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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexide.platform.core.run.ProcessOutputListener
import com.nexide.platform.core.run.ProcessRunner

/**
 * Bottom tool window that shows process output and Stop control.
 */
@Composable
fun RunToolWindow(
  runner: ProcessRunner,
  visible: Boolean,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!visible) return

  var output by remember { mutableStateOf("") }
  var running by remember { mutableStateOf(runner.isRunning) }
  val scrollState = rememberScrollState()

  DisposableEffect(runner) {
    val listener = object : ProcessOutputListener {
      override fun onStarted(command: String) {
        output = "> $command\n"
        running = true
      }

      override fun onOutput(text: String) {
        output += text
      }

      override fun onFinished(exitCode: Int) {
        output += "\nProcess finished with exit code $exitCode\n"
        running = false
      }
    }
    runner.addListener(listener)
    onDispose { runner.removeListener(listener) }
  }

  // Auto-scroll to bottom when output grows
  LaunchedEffect(output) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 160.dp, max = 280.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    // Toolbar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Run",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
      if (running) {
        TextButton(onClick = { runner.stop() }) {
          Text("Stop", style = MaterialTheme.typography.labelMedium)
        }
      }
      TextButton(onClick = { output = "" }) {
        Text("Clear", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(onClick = onClose) {
        Text("×", style = MaterialTheme.typography.labelLarge)
      }
    }

    // Output
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .padding(8.dp)
    ) {
      Text(
        text = output.ifEmpty { "No output yet. Use Run menu to start a process." },
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          lineHeight = 16.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
      )
    }
  }
}
