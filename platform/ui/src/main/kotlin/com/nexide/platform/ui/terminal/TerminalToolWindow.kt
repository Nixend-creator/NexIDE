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

package com.nexide.platform.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.Executors
import javax.swing.SwingUtilities

/**
 * Simple integrated terminal: one command at a time in a working directory.
 * Supports built-in `cd` and external shell commands.
 */
@Composable
fun TerminalToolWindow(
  projectRoot: String,
  visible: Boolean,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!visible) return

  var cwd by remember(projectRoot) { mutableStateOf(File(projectRoot).absoluteFile) }
  var output by remember { mutableStateOf("Terminal ready. Working directory: ${cwd.path}\n") }
  var input by remember { mutableStateOf("") }
  var busy by remember { mutableStateOf(false) }
  val history = remember { mutableListOf<String>() }
  var historyIndex by remember { mutableStateOf(-1) }
  val scrollState = rememberScrollState()
  val executor = remember { Executors.newSingleThreadExecutor() }

  LaunchedEffect(output) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  fun append(text: String) {
    if (SwingUtilities.isEventDispatchThread()) {
      output += text
    } else {
      SwingUtilities.invokeLater { output += text }
    }
  }

  fun setBusy(value: Boolean) {
    if (SwingUtilities.isEventDispatchThread()) {
      busy = value
    } else {
      SwingUtilities.invokeLater { busy = value }
    }
  }

  fun runCommand(line: String) {
    val trimmed = line.trim()
    if (trimmed.isEmpty() || busy) return

    history.add(trimmed)
    historyIndex = history.size
    append("$ ${trimmed}\n")
    input = ""

    // Built-in: cd
    if (trimmed == "cd" || trimmed.startsWith("cd ")) {
      val target = trimmed.removePrefix("cd").trim().ifEmpty { projectRoot }
      val next = when {
        target == "~" -> File(System.getProperty("user.home"))
        target.startsWith("/") || (target.length >= 2 && target[1] == ':') -> File(target)
        else -> File(cwd, target)
      }.canonicalFile
      if (next.isDirectory) {
        cwd = next
        append("cd: ${cwd.path}\n")
      } else {
        append("cd: no such directory: $target\n")
      }
      return
    }

    // Built-in: clear
    if (trimmed == "clear" || trimmed == "cls") {
      output = ""
      return
    }

    // Built-in: pwd
    if (trimmed == "pwd") {
      append("${cwd.path}\n")
      return
    }

    setBusy(true)
    executor.execute {
      try {
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val command = if (isWindows) {
          listOf("cmd", "/c", trimmed)
        } else {
          listOf("sh", "-c", trimmed)
        }
        val process = ProcessBuilder(command)
          .directory(cwd)
          .redirectErrorStream(true)
          .start()

        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
          var lineOut: String?
          while (reader.readLine().also { lineOut = it } != null) {
            append(lineOut + "\n")
          }
        }
        val code = process.waitFor()
        if (code != 0) {
          append("[exit code $code]\n")
        }
      } catch (e: Exception) {
        append("[error] ${e.message}\n")
      } finally {
        setBusy(false)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 180.dp, max = 300.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    // Toolbar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Terminal",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
      Text(
        text = cwd.name,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
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
        .weight(1f)
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(8.dp)
    ) {
      Text(
        text = output,
        style = TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(scrollState)
      )
    }

    // Input
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "$ ",
        style = TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.primary
        )
      )
      BasicTextField(
        value = input,
        onValueChange = { input = it },
        singleLine = true,
        enabled = !busy,
        textStyle = TextStyle(
          fontFamily = FontFamily.Monospace,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { runCommand(input) }),
        modifier = Modifier
          .weight(1f)
          .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
              Key.Enter, Key.NumPadEnter -> {
                runCommand(input)
                true
              }
              Key.DirectionUp -> {
                if (history.isNotEmpty()) {
                  historyIndex = (historyIndex - 1).coerceAtLeast(0)
                  input = history[historyIndex]
                }
                true
              }
              Key.DirectionDown -> {
                if (history.isNotEmpty()) {
                  historyIndex = (historyIndex + 1).coerceAtMost(history.size)
                  input = if (historyIndex >= history.size) "" else history[historyIndex]
                }
                true
              }
              else -> false
            }
          }
      )
    }
  }
}
