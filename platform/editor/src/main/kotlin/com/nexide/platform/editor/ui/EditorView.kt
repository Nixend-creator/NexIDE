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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexide.platform.core.debug.BreakpointManager
import com.nexide.platform.core.git.GitDiffGutter
import com.nexide.platform.core.git.LineDiffType
import com.nexide.platform.core.settings.NexIdeSettings
import com.nexide.platform.editor.Editor
import java.io.File
import com.nexide.platform.editor.completion.CompletionContributor
import com.nexide.platform.editor.completion.LookupElement
import com.nexide.platform.editor.format.CodeFormatter
import com.nexide.platform.editor.syntax.SyntaxColors
import com.nexide.platform.editor.syntax.SyntaxHighlighterRegistry
import com.nexide.platform.plugin.filetype.FileTypeRegistry
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.DisposableEffect

/**
 * Basic text editor view backed by an [Editor] instance.
 * Supports save, undo/redo, line numbers and basic syntax highlighting.
 */
@Composable
fun EditorView(
  editor: Editor,
  modifier: Modifier = Modifier,
  /** When non-null, jumps caret to this 1-based line (e.g. from Find in Files). */
  gotoLineRequest: Int? = null,
  onGotoLineHandled: () -> Unit = {},
  projectRootPath: String? = null,
  /** Bump to re-read settings (font size, line numbers). */
  settingsRevision: Int = 0
) {
  var textFieldValue by remember(editor) {
    mutableStateOf(TextFieldValue(editor.document.getText()))
  }
  val text get() = textFieldValue.text
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var modifiedTick by remember { mutableStateOf(0) }

  // Reload buffer when Editor.reloadFromDisk() bumps contentRevision
  LaunchedEffect(editor, editor.contentRevision) {
    textFieldValue = TextFieldValue(editor.document.getText())
    modifiedTick++
  }

  var findVisible by remember { mutableStateOf(false) }
  var replaceMode by remember { mutableStateOf(false) }
  var findQuery by remember { mutableStateOf("") }
  var replaceText by remember { mutableStateOf("") }
  var findIndex by remember { mutableStateOf(0) }
  var goToLineVisible by remember { mutableStateOf(false) }
  var completionVisible by remember { mutableStateOf(false) }
  var completionItems by remember { mutableStateOf<List<LookupElement>>(emptyList()) }
  var completionSelected by remember { mutableStateOf(0) }
  var completionPrefix by remember { mutableStateOf("") }
  val findMatches = remember(text, findQuery) { findMatches(text, findQuery) }
  if (findIndex >= findMatches.size && findMatches.isNotEmpty()) {
    findIndex = 0
  }

  LaunchedEffect(gotoLineRequest, editor) {
    val line = gotoLineRequest
    if (line != null && line > 0) {
      val offset = offsetOfLine(textFieldValue.text, line).coerceIn(0, textFieldValue.text.length)
      textFieldValue = textFieldValue.copy(selection = TextRange(offset))
      statusMessage = "Line $line"
      modifiedTick++
      onGotoLineHandled()
    }
  }

  fun applyTextChange(newText: String, caret: Int = newText.length) {
    editor.document.setText(newText)
    editor.undoManager.recordChange()
    textFieldValue = TextFieldValue(newText, TextRange(caret.coerceIn(0, newText.length)))
    modifiedTick++
  }

  fun performReplace() {
    if (findMatches.isEmpty() || findQuery.isEmpty()) return
    val idx = findIndex.coerceIn(0, findMatches.lastIndex)
    val start = findMatches[idx]
    val newText = replaceAt(text, start, findQuery.length, replaceText)
    applyTextChange(newText, start + replaceText.length)
    statusMessage = "Replaced"
  }

  fun performReplaceAll() {
    if (findMatches.isEmpty() || findQuery.isEmpty()) return
    val count = findMatches.size
    val newText = replaceAll(text, findQuery, replaceText)
    applyTextChange(newText)
    findIndex = 0
    statusMessage = "Replaced $count occurrence(s)"
  }

  fun performSave() {
    val ok = editor.save()
    statusMessage = if (ok) "Saved" else "Save failed"
    modifiedTick++
  }

  fun performUndo() {
    if (editor.undoManager.undo()) {
      textFieldValue = TextFieldValue(editor.document.getText())
      statusMessage = "Undo"
      modifiedTick++
    }
  }

  fun performRedo() {
    if (editor.undoManager.redo()) {
      textFieldValue = TextFieldValue(editor.document.getText())
      statusMessage = "Redo"
      modifiedTick++
    }
  }

  fun goToLine(line: Int) {
    val offset = offsetOfLine(text, line).coerceIn(0, text.length)
    textFieldValue = textFieldValue.copy(selection = TextRange(offset))
    statusMessage = "Line $line"
    goToLineVisible = false
    modifiedTick++
  }

  fun insertIndent() {
    val tabSize = NexIdeSettings.tabSize
    val spaces = " ".repeat(tabSize)
    val sel = textFieldValue.selection
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)
    val newText = text.substring(0, start) + spaces + text.substring(end)
    val newCaret = start + spaces.length
    editor.document.setText(newText)
    editor.undoManager.recordChange()
    textFieldValue = TextFieldValue(newText, TextRange(newCaret))
    modifiedTick++
  }

  fun removeIndent() {
    val tabSize = NexIdeSettings.tabSize
    val sel = textFieldValue.selection
    val caret = minOf(sel.start, sel.end)
    // Find start of current line
    val lineStart = text.lastIndexOf('\n', (caret - 1).coerceAtLeast(0)).let {
      if (it < 0) 0 else it + 1
    }
    val leading = text.substring(lineStart, caret)
    val spacesToRemove = leading.takeWhile { it == ' ' }.length.coerceAtMost(tabSize)
    if (spacesToRemove == 0) return
    val removeFrom = caret - spacesToRemove
    val newText = text.substring(0, removeFrom) + text.substring(caret)
    editor.document.setText(newText)
    editor.undoManager.recordChange()
    textFieldValue = TextFieldValue(newText, TextRange(removeFrom))
    modifiedTick++
  }

  fun showCompletion() {
    val offset = textFieldValue.selection.start
    val lang = FileTypeRegistry.getLanguageByFileName(editor.file.name)
    completionPrefix = CompletionContributor.prefixAt(text, offset)
    completionItems = CompletionContributor.complete(text, offset, lang)
    completionSelected = 0
    completionVisible = completionItems.isNotEmpty()
    if (completionVisible) {
      statusMessage = "Completion (${completionItems.size})"
    }
  }

  fun applyCompletion(item: LookupElement) {
    val offset = textFieldValue.selection.start
    val prefix = CompletionContributor.prefixAt(text, offset)
    val start = offset - prefix.length
    val newText = text.substring(0, start) + item.lookupString + text.substring(offset)
    val newCaret = start + item.lookupString.length
    editor.document.setText(newText)
    editor.undoManager.recordChange()
    textFieldValue = TextFieldValue(newText, TextRange(newCaret))
    completionVisible = false
    statusMessage = null
    modifiedTick++
  }

  fun formatDocument() {
    val indent = NexIdeSettings.tabSize
    val formatted = CodeFormatter.format(text, indent)
    if (formatted == text) {
      statusMessage = "Already formatted"
      return
    }
    val caret = textFieldValue.selection.start.coerceIn(0, formatted.length)
    editor.replaceText(formatted)
    editor.undoManager.recordChange()
    textFieldValue = TextFieldValue(formatted, TextRange(caret.coerceAtMost(formatted.length)))
    statusMessage = "Formatted"
    modifiedTick++
  }

  /**
   * Toggle // comments on the current line or all lines in the selection.
   */
  fun toggleComment() {
    val sel = textFieldValue.selection
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)
    val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let {
      if (it < 0) 0 else it + 1
    }
    val lineEnd = text.indexOf('\n', if (end > start) end - 1 else end).let {
      if (it < 0) text.length else it
    }
    val block = text.substring(lineStart, lineEnd)
    val blockLines = block.split('\n')
    val allCommented = blockLines.all { line ->
      val t = line.trimStart()
      t.isEmpty() || t.startsWith("//")
    }
    val newBlock = blockLines.joinToString("\n") { line ->
      if (line.isEmpty()) return@joinToString line
      val leading = line.takeWhile { it == ' ' || it == '\t' }
      val rest = line.substring(leading.length)
      if (allCommented) {
        val uncommented = when {
          rest.startsWith("// ") -> rest.removePrefix("// ")
          rest.startsWith("//") -> rest.removePrefix("//")
          else -> rest
        }
        leading + uncommented
      } else {
        if (rest.startsWith("//")) line else "$leading// $rest"
      }
    }
    val newText = text.substring(0, lineStart) + newBlock + text.substring(lineEnd)
    editor.replaceText(newText)
    editor.undoManager.recordChange()
    val newEnd = lineStart + newBlock.length
    textFieldValue = TextFieldValue(newText, TextRange(lineStart, newEnd.coerceAtMost(newText.length)))
    statusMessage = if (allCommented) "Uncommented" else "Commented"
    modifiedTick++
  }

  /**
   * Duplicates the current line, or the selected lines, placing a copy below.
   */
  fun duplicateLines() {
    val sel = textFieldValue.selection
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)
    val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let {
      if (it < 0) 0 else it + 1
    }
    val lineEndExclusive = when {
      end > start && end <= text.length && end > 0 && text[end - 1] == '\n' -> end
      else -> {
        val nl = text.indexOf('\n', if (end > start) (end - 1).coerceAtLeast(0) else end)
        if (nl < 0) text.length else nl
      }
    }
    val block = text.substring(lineStart, lineEndExclusive)
    val hasTrailingNl = lineEndExclusive < text.length && text[lineEndExclusive] == '\n'
    val after = if (hasTrailingNl) lineEndExclusive + 1 else lineEndExclusive
    val chunk = if (block.endsWith("\n")) block else "$block\n"
    val newText = text.substring(0, after) + chunk + text.substring(after)
    val caret = (after + chunk.length - 1).coerceIn(0, newText.length)
    editor.replaceText(newText)
    editor.undoManager.recordChange()
    textFieldValue = TextFieldValue(newText, TextRange(caret))
    statusMessage = "Duplicated"
    modifiedTick++
  }

  /**
   * Moves the current line (or selected lines) up or down by one line.
   */
  fun moveLines(up: Boolean) {
    val sel = textFieldValue.selection
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)
    val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let {
      if (it < 0) 0 else it + 1
    }
    val lineEnd = text.indexOf('\n', if (end > start) (end - 1).coerceAtLeast(0) else end).let {
      if (it < 0) text.length else it
    }
    val block = text.substring(lineStart, lineEnd)

    if (up) {
      if (lineStart == 0) return
      val prevStart = text.lastIndexOf('\n', lineStart - 2).let { if (it < 0) 0 else it + 1 }
      val prevLine = text.substring(prevStart, lineStart - 1)
      val after = if (lineEnd < text.length) text.substring(lineEnd + 1) else ""
      val finalText = buildString {
        append(text.substring(0, prevStart))
        append(block)
        append('\n')
        append(prevLine)
        if (after.isNotEmpty() || lineEnd < text.length) {
          append('\n')
          append(after)
        }
      }
      editor.replaceText(finalText)
      editor.undoManager.recordChange()
      textFieldValue = TextFieldValue(finalText, TextRange(prevStart, prevStart + block.length))
    } else {
      if (lineEnd >= text.length) return
      val nextStart = lineEnd + 1
      val nextEnd = text.indexOf('\n', nextStart).let { if (it < 0) text.length else it }
      val nextLine = text.substring(nextStart, nextEnd)
      val after = if (nextEnd < text.length) text.substring(nextEnd + 1) else ""
      val finalText = buildString {
        append(text.substring(0, lineStart))
        append(nextLine)
        append('\n')
        append(block)
        if (after.isNotEmpty() || nextEnd < text.length) {
          append('\n')
          append(after)
        }
      }
      val newStart = lineStart + nextLine.length + 1
      editor.replaceText(finalText)
      editor.undoManager.recordChange()
      textFieldValue = TextFieldValue(finalText, TextRange(newStart, newStart + block.length))
    }
    statusMessage = if (up) "Moved up" else "Moved down"
    modifiedTick++
  }

  val onTextChange: (TextFieldValue) -> Unit = { newValue ->
    if (newValue.text != textFieldValue.text) {
      editor.document.setText(newValue.text)
      editor.undoManager.recordChange()
      statusMessage = null
    }
    textFieldValue = newValue
    editor.caretOffset = newValue.selection.start
    modifiedTick++
  }

  // Sync caret when only selection moves (arrow keys etc.)
  SideEffect {
    editor.caretOffset = textFieldValue.selection.start
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
  val highlightedText = remember(text, language, isDark, findQuery, findIndex, findMatches) {
    val base = SyntaxHighlighterRegistry.highlightAsAnnotatedString(text, language, syntaxColors)
    if (findQuery.isEmpty() || findMatches.isEmpty()) {
      base
    } else {
      // Overlay find match backgrounds on top of syntax colors
      buildAnnotatedString {
        append(base)
        val matchLen = findQuery.length
        findMatches.forEachIndexed { i, start ->
          val end = (start + matchLen).coerceAtMost(text.length)
          val bg = if (i == findIndex) {
            Color(0x88FF9800) // current match — stronger orange
          } else {
            Color(0x55FFEB3B) // other matches — yellow
          }
          addStyle(SpanStyle(background = bg), start, end)
        }
      }
    }
  }

  @Suppress("UNUSED_EXPRESSION")
  settingsRevision // recompose when settings change
  val fontSizeSp = NexIdeSettings.editorFontSize.sp
  val lineHeightSp = (NexIdeSettings.editorFontSize * 1.4f).sp
  val showGutter = NexIdeSettings.showLineNumbers

  val textStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = fontSizeSp,
    color = Color.Transparent, // real colors come from highlighted overlay
    lineHeight = lineHeightSp
  )
  val highlightStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = fontSizeSp,
    color = syntaxColors.default,
    lineHeight = lineHeightSp
  )
  val gutterStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = fontSizeSp,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
    lineHeight = lineHeightSp,
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
          ctrlOrMeta && event.key == Key.F -> {
            findVisible = true
            replaceMode = false
            true
          }
          ctrlOrMeta && event.key == Key.R -> {
            findVisible = true
            replaceMode = true
            true
          }
          ctrlOrMeta && event.key == Key.G -> {
            goToLineVisible = true
            true
          }
          ctrlOrMeta && event.key == Key.Spacebar -> {
            showCompletion()
            true
          }
          // Ctrl+Alt+L — Format
          ctrlOrMeta && event.isAltPressed && event.key == Key.L -> {
            formatDocument()
            true
          }
          // Ctrl+/ — Toggle line comment
          ctrlOrMeta && (event.key == Key.Slash || event.key == Key.Divide) -> {
            toggleComment()
            true
          }
          // Ctrl+D — Duplicate line / selection
          ctrlOrMeta && event.key == Key.D && !event.isShiftPressed -> {
            duplicateLines()
            true
          }
          // Alt+Shift+Up — Move line up
          event.isAltPressed && event.isShiftPressed && event.key == Key.DirectionUp -> {
            moveLines(up = true)
            true
          }
          // Alt+Shift+Down — Move line down
          event.isAltPressed && event.isShiftPressed && event.key == Key.DirectionDown -> {
            moveLines(up = false)
            true
          }
          event.key == Key.Escape && completionVisible -> {
            completionVisible = false
            true
          }
          event.key == Key.Escape && findVisible -> {
            findVisible = false
            replaceMode = false
            true
          }
          completionVisible && event.key == Key.DirectionDown -> {
            if (completionItems.isNotEmpty()) {
              completionSelected = (completionSelected + 1) % completionItems.size
            }
            true
          }
          completionVisible && event.key == Key.DirectionUp -> {
            if (completionItems.isNotEmpty()) {
              completionSelected =
                if (completionSelected <= 0) completionItems.lastIndex else completionSelected - 1
            }
            true
          }
          completionVisible && (event.key == Key.Enter || event.key == Key.NumPadEnter ||
            (event.key == Key.Tab && !event.isShiftPressed)) -> {
            completionItems.getOrNull(completionSelected)?.let { applyCompletion(it) }
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
          event.key == Key.Tab && !event.isShiftPressed && !ctrlOrMeta && !completionVisible -> {
            insertIndent()
            true
          }
          event.key == Key.Tab && event.isShiftPressed && !ctrlOrMeta -> {
            removeIndent()
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

      val caretOffset = textFieldValue.selection.start
      val (caretLine, caretCol) = lineColAtOffset(text, caretOffset)
      val status = buildString {
        append(editor.file.name)
        if (editor.isModified) append(" *")
        append("  |  ")
        append(language)
        append("  |  ")
        append("Ln $caretLine, Col $caretCol")
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

    val caretLine = lineColAtOffset(text, textFieldValue.selection.start).first
    BreadcrumbsBar(
      projectRootPath = projectRootPath,
      file = editor.file,
      documentText = text,
      language = language,
      caretLine = caretLine,
      onNavigateToLine = { line -> goToLine(line) }
    )

    if (findVisible) {
      FindBar(
        query = findQuery,
        onQueryChange = {
          findQuery = it
          findIndex = 0
        },
        matchCount = findMatches.size,
        currentIndex = findIndex.coerceAtMost((findMatches.size - 1).coerceAtLeast(0)),
        onNext = {
          if (findMatches.isNotEmpty()) {
            findIndex = (findIndex + 1) % findMatches.size
          }
        },
        onPrev = {
          if (findMatches.isNotEmpty()) {
            findIndex = if (findIndex <= 0) findMatches.lastIndex else findIndex - 1
          }
        },
        onClose = {
          findVisible = false
          replaceMode = false
        },
        replaceMode = replaceMode,
        replaceText = replaceText,
        onReplaceTextChange = { replaceText = it },
        onReplace = { performReplace() },
        onReplaceAll = { performReplaceAll() }
      )
    }

    if (goToLineVisible) {
      GoToLineDialog(
        maxLine = lineCount,
        onGoTo = { goToLine(it) },
        onDismiss = { goToLineVisible = false }
      )
    }

    // Editor area: gutter + text
    Row(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      if (showGutter) {
        val bpManager = remember { BreakpointManager.getInstance() }
        var bpTick by remember { mutableStateOf(0) }
        DisposableEffect(bpManager, editor.file.path) {
          val listener = { bpTick++ }
          bpManager.addListener(listener)
          onDispose { bpManager.removeListener(listener) }
        }
        @Suppress("UNUSED_EXPRESSION")
        bpTick
        val bpLines = remember(bpTick, editor.file.path) {
          bpManager.getForFile(editor.file.path).map { it.line }.toSet()
        }
        val diffLines = remember(editor.file.path, editor.contentRevision, projectRootPath) {
          val root = projectRootPath?.let { File(it) }
          if (root != null && root.isDirectory) {
            GitDiffGutter.invalidate(editor.file.path)
            GitDiffGutter.lineChanges(root, editor.file.path)
          } else {
            emptyMap()
          }
        }

        Column(
          modifier = Modifier
            .widthIn(min = lineNumberWidth + 16.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .verticalScroll(scrollState)
            .padding(start = 2.dp, end = 4.dp, top = 12.dp, bottom = 12.dp)
        ) {
          for (line in 1..lineCount) {
            val hasBp = line in bpLines
            val diff = diffLines[line]
            val diffColor = when (diff) {
              LineDiffType.ADDED -> Color(0xFF4CAF50)
              LineDiffType.MODIFIED -> Color(0xFF2196F3)
              null -> Color.Transparent
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  bpManager.toggle(editor.file.path, line)
                  bpTick++
                },
              verticalAlignment = Alignment.CenterVertically
            ) {
              // VCS change stripe
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .heightIn(min = 14.dp)
                  .background(diffColor)
              )
              Text(
                text = if (hasBp) "●" else " ",
                style = gutterStyle.copy(
                  color = if (hasBp) Color(0xFFE53935) else Color.Transparent,
                  textAlign = TextAlign.Center
                ),
                modifier = Modifier.width(12.dp)
              )
              Text(
                text = line.toString(),
                style = gutterStyle
              )
            }
          }
        }
      }

      // Text field with syntax highlight overlay + completion popup
      Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
        BasicTextField(
          value = textFieldValue,
          onValueChange = { newValue ->
            if (completionVisible) completionVisible = false
            onTextChange(newValue)
          },
          modifier = Modifier
            .fillMaxSize()
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
        if (completionVisible && completionItems.isNotEmpty()) {
          LazyColumn(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(start = 24.dp, top = 40.dp)
              .widthIn(min = 180.dp, max = 320.dp)
              .heightIn(max = 220.dp)
              .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
              .padding(4.dp)
          ) {
            itemsIndexed(completionItems) { index, item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .background(
                    if (index == completionSelected) MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                  )
                  .clickable { applyCompletion(item) }
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = item.lookupString,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = item.typeText,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }
  }
}
