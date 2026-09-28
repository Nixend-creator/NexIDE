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

package com.nexide.platform.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog to jump to a specific line number.
 */
@Composable
fun GoToLineDialog(
  maxLine: Int,
  initialValue: String = "",
  onGoTo: (line: Int) -> Unit,
  onDismiss: () -> Unit
) {
  var text by remember { mutableStateOf(initialValue) }
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  fun tryGoTo() {
    val line = text.toIntOrNull()
    if (line != null && line in 1..maxLine) {
      onGoTo(line)
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(20.dp)
        .width(320.dp)
        .onPreviewKeyEvent { event ->
          if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
          when (event.key) {
            Key.Escape -> {
              onDismiss()
              true
            }
            Key.Enter, Key.NumPadEnter -> {
              tryGoTo()
              true
            }
            else -> false
          }
        }
    ) {
      Text(
        text = "Go to Line",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.padding(6.dp))
      Text(
        text = "Enter line number (1 – $maxLine):",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.padding(4.dp))

      BasicTextField(
        value = text,
        onValueChange = { new ->
          if (new.isEmpty() || new.all { it.isDigit() }) {
            text = new
          }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester)
          .background(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.shapes.small
          )
          .padding(horizontal = 12.dp, vertical = 8.dp)
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
        TextButton(
          onClick = { tryGoTo() },
          enabled = text.toIntOrNull()?.let { it in 1..maxLine } == true
        ) {
          Text("Go")
        }
      }
    }
  }
}

/**
 * Returns the character offset of the start of [line] (1-based) in [text].
 */
fun offsetOfLine(text: String, line: Int): Int {
  if (line <= 1) return 0
  var currentLine = 1
  for (i in text.indices) {
    if (text[i] == '\n') {
      currentLine++
      if (currentLine == line) {
        return i + 1
      }
    }
  }
  return text.length
}

/**
 * Converts a character [offset] into 1-based (line, column).
 */
fun lineColAtOffset(text: String, offset: Int): Pair<Int, Int> {
  val safe = offset.coerceIn(0, text.length)
  var line = 1
  var col = 1
  for (i in 0 until safe) {
    if (text[i] == '\n') {
      line++
      col = 1
    } else {
      col++
    }
  }
  return line to col
}
