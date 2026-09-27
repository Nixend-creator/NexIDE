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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexide.platform.editor.Editor

/**
 * Basic text editor view backed by an [Editor] instance.
 * Supports Ctrl+S / Cmd+S to save.
 */
@Composable
fun EditorView(
  editor: Editor,
  modifier: Modifier = Modifier
) {
  var text by remember(editor) { mutableStateOf(editor.document.getText()) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  // Force recomposition when modified flag changes
  var modifiedTick by remember { mutableStateOf(0) }

  fun performSave() {
    val ok = editor.save()
    statusMessage = if (ok) "Saved" else "Save failed"
    modifiedTick++
  }

  val onTextChange: (String) -> Unit = { newText ->
    text = newText
    editor.document.setText(newText)
    statusMessage = null
    modifiedTick++
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown &&
          event.key == Key.S &&
          (event.isCtrlPressed || event.isMetaPressed)
        ) {
          performSave()
          true
        } else {
          false
        }
      }
  ) {
    // Status bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .padding(horizontal = 12.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Force read of modifiedTick so Compose tracks it
      @Suppress("UNUSED_EXPRESSION")
      modifiedTick

      val status = buildString {
        append(editor.file.name)
        if (editor.isModified) append(" *")
        append("  |  ")
        append("${editor.document.textLength} chars")
        statusMessage?.let {
          append("  |  ")
          append(it)
        }
      }
      Text(
        text = status,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )

      if (editor.isModified) {
        TextButton(onClick = { performSave() }) {
          Text("Save", style = MaterialTheme.typography.labelMedium)
        }
      } else {
        Spacer(modifier = Modifier.width(8.dp))
      }
    }

    // Editor area
    BasicTextField(
      value = text,
      onValueChange = onTextChange,
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
        .padding(12.dp),
      textStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 20.sp
      ),
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      decorationBox = { innerTextField ->
        Box(modifier = Modifier.fillMaxSize()) {
          if (text.isEmpty()) {
            Text(
              text = "// Start typing...",
              style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = Color.Gray
              )
            )
          }
          innerTextField()
        }
      }
    )
  }
}
