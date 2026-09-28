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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexide.platform.editor.Editor
import com.nexide.platform.editor.syntax.SyntaxColors
import com.nexide.platform.editor.syntax.SyntaxHighlighterRegistry
import com.nexide.platform.plugin.filetype.FileTypeRegistry

private val EDITOR_FONT_SIZE = 14.sp
private val EDITOR_LINE_HEIGHT = 20.sp

/**
 * Basic text editor view backed by an [Editor] instance.
 * Supports save, undo/redo, line numbers and basic syntax highlighting.
 */
@Composable
fun EditorView(
  editor: Editor,
  modifier: Modifier = Modifier
) {
  var text by remember(editor) { mutableStateOf(editor.document.getText()) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var modifiedTick by remember { mutableStateOf(0) }

  fun performSave() {
    val ok = editor.save()
    statusMessage = if (ok) "Saved" else "Save failed"
    modifiedTick++
  }

  fun performUndo() {
    if (editor.undoManager.undo()) {
      text = editor.document.getText()
      statusMessage = "Undo"
      modifiedTick++
    }
  }

  fun performRedo() {
    if (editor.undoManager.redo()) {
      text = editor.document.getText()
      statusMessage = "Redo"
      modifiedTick++
    }
  }

  val onTextChange: (String) -> Unit = { newText ->
    text = newText
    editor.document.setText(newText)
    editor.undoManager.recordChange()
    statusMessage = null
    modifiedTick++
  }

  val lineCount = remember(text) {
    if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
  }
  val lineNumberWidth = remember(lineCount) {
    val digits = lineCount.toString().length.coerceAtLeast(2)
    (digits * 10 + 16).dp
  }

  val scrollState = rememberScrollState()
  val language = FileTypeRegistry.getLanguageByFileName(editor.file.name)
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val syntaxColors = if (isDark) SyntaxColors.Darcula else SyntaxColors.Light
  val highlightedText = remember(text, language, isDark) {
    SyntaxHighlighterRegistry.highlightAsAnnotatedString(text, language, syntaxColors)
  }

  val textStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = EDITOR_FONT_SIZE,
    color = Color.Transparent, // real colors come from highlighted overlay
    lineHeight = EDITOR_LINE_HEIGHT
  )
  val highlightStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = EDITOR_FONT_SIZE,
    color = syntaxColors.default,
    lineHeight = EDITOR_LINE_HEIGHT
  )
  val gutterStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = EDITOR_FONT_SIZE,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    lineHeight = EDITOR_LINE_HEIGHT,
    textAlign = TextAlign.End
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val ctrlOrMeta = event.isCtrlPressed || event.isMetaPressed
        when {
          ctrlOrMeta && event.key == Key.S -> {
            performSave()
            true
          }
          ctrlOrMeta && event.key == Key.Z && !event.isShiftPressed -> {
            performUndo()
            true
          }
          ctrlOrMeta && (event.key == Key.Y || (event.key == Key.Z && event.isShiftPressed)) -> {
            performRedo()
            true
          }
          else -> false
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
      @Suppress("UNUSED_EXPRESSION")
      modifiedTick

      val status = buildString {
        append(editor.file.name)
        if (editor.isModified) append(" *")
        append("  |  ")
        append(language)
        append("  |  ")
        append("${editor.document.textLength} chars")
        append("  |  ")
        append("$lineCount lines")
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

    // Editor area: gutter + text
    Row(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      // Line numbers gutter
      Box(
        modifier = Modifier
          .widthIn(min = lineNumberWidth)
          .fillMaxHeight()
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
          .verticalScroll(scrollState)
          .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
      ) {
        val numbers = (1..lineCount).joinToString("\n")
        Text(
          text = numbers,
          style = gutterStyle
        )
      }

      // Text field with syntax highlight overlay
      BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
          .verticalScroll(scrollState)
          .padding(start = 8.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
          Box {
            if (text.isEmpty()) {
              Text(
                text = "// Start typing...",
                style = highlightStyle.copy(color = Color.Gray)
              )
            } else {
              Text(
                text = highlightedText,
                style = highlightStyle
              )
            }
            innerTextField()
          }
        }
      )
    }
  }
}
