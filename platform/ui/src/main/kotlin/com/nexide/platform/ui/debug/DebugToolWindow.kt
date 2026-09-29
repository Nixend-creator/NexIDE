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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import com.nexide.platform.core.debug.BreakpointManager
import com.nexide.platform.core.debug.DebugSessionManager
import com.nexide.platform.core.debug.DebugSessionState
import com.nexide.platform.core.run.ProcessOutputListener
import javax.swing.SwingUtilities

/**
 * Debug tool window: session status, JDWP port, breakpoints count, process output.
 */
@Composable
fun DebugToolWindow(
  sessionManager: DebugSessionManager,
  visible: Boolean,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!visible) return

  var state by remember { mutableStateOf(sessionManager.state) }
  var output by remember { mutableStateOf("") }
  var bpCount by remember { mutableStateOf(BreakpointManager.getInstance().getAll().size) }
  val scrollState = rememberScrollState()

  DisposableEffect(sessionManager) {
    val stateListener: (DebugSessionState) -> Unit = { s ->
      SwingUtilities.invokeLater { state = s }
    }
    sessionManager.addStateListener(stateListener)

    val outputListener = object : ProcessOutputListener {
      override fun onStarted(command: String) {
        SwingUtilities.invokeLater {
          output = "> $command\n[JDWP listening on *:${sessionManager.debugPort}]\n"
        }
      }

      override fun onOutput(text: String) {
        SwingUtilities.invokeLater { output += text }
      }

      override fun onFinished(exitCode: Int) {
        SwingUtilities.invokeLater {
          output += "\nProcess finished with exit code $exitCode\n"
        }
      }
    }
    sessionManager.getProcessRunner().addListener(outputListener)

    val bpListener = {
      SwingUtilities.invokeLater {
        bpCount = BreakpointManager.getInstance().getAll().size
      }
    }
    BreakpointManager.getInstance().addListener(bpListener)

    onDispose {
      sessionManager.removeStateListener(stateListener)
      sessionManager.getProcessRunner().removeListener(outputListener)
      BreakpointManager.getInstance().removeListener(bpListener)
    }
  }

  LaunchedEffect(output) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  val stateLabel = when (state) {
    DebugSessionState.IDLE -> "Idle"
    DebugSessionState.STARTING -> "Starting…"
    DebugSessionState.RUNNING -> "Running"
    DebugSessionState.STOPPED -> "Stopped"
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 180.dp, max = 320.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Debug — $stateLabel",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = "port ${sessionManager.debugPort} · $bpCount bp",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 8.dp)
      )
      if (sessionManager.isActive) {
        TextButton(onClick = { sessionManager.stop() }) {
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

    Text(
      text = output.ifEmpty {
        "No debug session. Use Run → Debug Gradle to start with JDWP on port ${DebugSessionManager.DEFAULT_PORT}."
      },
      style = MaterialTheme.typography.bodySmall.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 16.sp
      ),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .padding(8.dp)
        .verticalScroll(scrollState)
    )
  }
}
