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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexide.platform.core.debug.BreakpointManager
import com.nexide.platform.core.debug.DebugSessionManager
import com.nexide.platform.core.debug.DebugSessionState
import com.nexide.platform.core.debug.DebugVariable
import com.nexide.platform.core.debug.JdiConnector
import com.nexide.platform.core.debug.JdiListener
import com.nexide.platform.core.debug.SuspendedThreadInfo
import com.nexide.platform.core.run.ProcessOutputListener
import javax.swing.SwingUtilities

/**
 * Debug tool window: session status, JDWP port, JDI attach, breakpoints, output.
 */
@Composable
fun DebugToolWindow(
  sessionManager: DebugSessionManager,
  jdiConnector: JdiConnector,
  visible: Boolean,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!visible) return

  var state by remember { mutableStateOf(sessionManager.state) }
  var output by remember { mutableStateOf("") }
  var bpCount by remember { mutableStateOf(BreakpointManager.getInstance().getAll().size) }
  var attached by remember { mutableStateOf(jdiConnector.isAttached) }
  var suspended by remember { mutableStateOf<SuspendedThreadInfo?>(null) }
  val scrollState = rememberScrollState()

  DisposableEffect(sessionManager, jdiConnector) {
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

    val jdiListener = object : JdiListener {
      override fun onAttached(vmName: String) {
        SwingUtilities.invokeLater {
          attached = true
          output += "[JDI] Attached to $vmName\n"
        }
      }

      override fun onDetached() {
        SwingUtilities.invokeLater {
          attached = false
          suspended = null
          output += "[JDI] Detached\n"
        }
      }

      override fun onBreakpointHit(info: SuspendedThreadInfo) {
        SwingUtilities.invokeLater {
          suspended = info
          output += "[BREAK] ${info.locationDescription} in ${info.threadName}\n"
          info.frames.take(8).forEach { frame ->
            output += "    at $frame\n"
          }
        }
      }

      override fun onMessage(message: String) {
        SwingUtilities.invokeLater {
          output += "[JDI] $message\n"
        }
      }
    }
    jdiConnector.addListener(jdiListener)

    onDispose {
      sessionManager.removeStateListener(stateListener)
      sessionManager.getProcessRunner().removeListener(outputListener)
      BreakpointManager.getInstance().removeListener(bpListener)
      jdiConnector.removeListener(jdiListener)
    }
  }

  LaunchedEffect(output) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  val stateLabel = when {
    suspended != null -> "Suspended"
    attached -> "Attached"
    state == DebugSessionState.STARTING -> "Starting…"
    state == DebugSessionState.RUNNING -> "Running"
    state == DebugSessionState.STOPPED -> "Stopped"
    else -> "Idle"
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 180.dp, max = 340.dp)
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
        modifier = Modifier.padding(end = 4.dp)
      )
      TextButton(
        onClick = {
          Thread {
            jdiConnector.attach(port = sessionManager.debugPort)
          }.start()
        },
        enabled = !attached
      ) {
        Text("Attach", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(
        onClick = {
          val n = jdiConnector.installBreakpoints(BreakpointManager.getInstance().getAll())
          output += "[JDI] install → $n request(s)\n"
        },
        enabled = attached
      ) {
        Text("Install BPs", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(
        onClick = {
          suspended = null
          jdiConnector.stepOver()
        },
        enabled = attached && suspended != null
      ) {
        Text("Step Over", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(
        onClick = {
          suspended = null
          jdiConnector.stepInto()
        },
        enabled = attached && suspended != null
      ) {
        Text("Step Into", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(
        onClick = {
          suspended = null
          jdiConnector.stepOut()
        },
        enabled = attached && suspended != null
      ) {
        Text("Step Out", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(
        onClick = {
          suspended = null
          jdiConnector.resumeAll()
        },
        enabled = attached
      ) {
        Text("Resume", style = MaterialTheme.typography.labelMedium)
      }
      if (sessionManager.isActive) {
        TextButton(onClick = { sessionManager.stop() }) {
          Text("Stop", style = MaterialTheme.typography.labelMedium)
        }
      }
      if (attached) {
        TextButton(onClick = { jdiConnector.detach() }) {
          Text("Detach", style = MaterialTheme.typography.labelMedium)
        }
      }
      TextButton(onClick = { output = "" }) {
        Text("Clear", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(onClick = onClose) {
        Text("×", style = MaterialTheme.typography.labelLarge)
      }
    }

    if (suspended != null) {
      Text(
        text = "● ${suspended!!.locationDescription}  (${suspended!!.threadName})",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
      )
    }
    // Watches editor row
    var watchInput by remember { mutableStateOf("") }
    var watchTick by remember { mutableStateOf(0) }
    val watchResults = remember(suspended, attached, watchTick) {
      jdiConnector.evaluateWatches()
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Watch:", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(end = 6.dp))
      BasicTextField(
        value = watchInput,
        onValueChange = { watchInput = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .weight(1f)
          .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
          .padding(6.dp)
      )
      TextButton(
        onClick = {
          jdiConnector.addWatch(watchInput)
          watchInput = ""
          watchTick++
        },
        enabled = watchInput.isNotBlank()
      ) {
        Text("Add", style = MaterialTheme.typography.labelMedium)
      }
    }

    Row(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      // Console / event log
      Text(
        text = output.ifEmpty {
          "1) Run → Debug Gradle  2) Attach  3) Install BPs  4) hit BP → Variables appear here"
        },
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          lineHeight = 16.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
          .weight(1.2f)
          .fillMaxHeight()
          .padding(8.dp)
          .verticalScroll(scrollState)
      )

      // Variables pane
      VerticalDivider()
      VariablesPane(
        variables = suspended?.variables.orEmpty(),
        watches = watchResults,
        onRemoveWatch = { expr ->
          jdiConnector.removeWatch(expr)
          watchTick++
        },
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      )
    }
  }
}

@Composable
private fun VariablesPane(
  variables: List<DebugVariable>,
  watches: List<Pair<String, String>> = emptyList(),
  onRemoveWatch: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.padding(4.dp)) {
    Text(
      text = "Watches",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
    if (watches.isEmpty()) {
      Text(
        text = "Add expression above (e.g. localName, this.field)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
      )
    } else {
      watches.forEach { (expr, value) ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "$expr = $value",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
          )
          TextButton(onClick = { onRemoveWatch(expr) }) {
            Text("×", style = MaterialTheme.typography.labelSmall)
          }
        }
      }
    }
    Text(
      text = "Variables",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
    if (variables.isEmpty()) {
      Text(
        text = "No variables (not suspended or no debug info)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(8.dp)
      )
    } else {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(variables, key = { "${it.scope}:${it.name}" }) { v ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = when (v.scope) {
                "arg" -> "a"
                "this" -> "f"
                else -> "v"
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.width(14.dp)
            )
            Text(
              text = v.name,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.width(100.dp)
            )
            Text(
              text = v.value,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }
  }
}
