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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp

/**
 * Find / Replace bar.
 * When [replaceMode] is true, shows replace field and Replace / Replace All actions.
 */
@Composable
fun FindBar(
  query: String,
  onQueryChange: (String) -> Unit,
  matchCount: Int,
  currentIndex: Int,
  onNext: () -> Unit,
  onPrev: () -> Unit,
  onClose: () -> Unit,
  replaceMode: Boolean = false,
  replaceText: String = "",
  onReplaceTextChange: (String) -> Unit = {},
  onReplace: () -> Unit = {},
  onReplaceAll: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.secondaryContainer)
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
          Key.Escape -> {
            onClose()
            true
          }
          Key.Enter, Key.NumPadEnter -> {
            onNext()
            true
          }
          else -> false
        }
      }
  ) {
    // Find row
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "Find:",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.width(56.dp)
      )

      BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .weight(1f)
          .focusRequester(focusRequester)
          .background(
            MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small
          )
          .padding(horizontal = 8.dp, vertical = 4.dp)
      )

      Spacer(modifier = Modifier.width(8.dp))

      val countLabel = if (query.isEmpty()) {
        ""
      } else if (matchCount == 0) {
        "No results"
      } else {
        "${currentIndex + 1} of $matchCount"
      }
      Text(
        text = countLabel,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.width(90.dp)
      )

      TextButton(onClick = onPrev, enabled = matchCount > 0) {
        Text("↑", style = MaterialTheme.typography.labelLarge)
      }
      TextButton(onClick = onNext, enabled = matchCount > 0) {
        Text("↓", style = MaterialTheme.typography.labelLarge)
      }
      TextButton(onClick = onClose) {
        Text("×", style = MaterialTheme.typography.labelLarge)
      }
    }

    // Replace row
    if (replaceMode) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
      ) {
        Text(
          text = "Replace:",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.width(56.dp)
        )

        BasicTextField(
          value = replaceText,
          onValueChange = onReplaceTextChange,
          singleLine = true,
          textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSecondaryContainer
          ),
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          modifier = Modifier
            .weight(1f)
            .background(
              MaterialTheme.colorScheme.surface,
              shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        TextButton(onClick = onReplace, enabled = matchCount > 0) {
          Text("Replace", style = MaterialTheme.typography.labelMedium)
        }
        TextButton(onClick = onReplaceAll, enabled = matchCount > 0) {
          Text("Replace All", style = MaterialTheme.typography.labelMedium)
        }
      }
    }
  }
}

/**
 * Finds all case-insensitive occurrences of [query] in [text].
 * Returns list of start offsets.
 */
fun findMatches(text: String, query: String): List<Int> {
  if (query.isEmpty()) return emptyList()
  val result = mutableListOf<Int>()
  val haystack = text.lowercase()
  val needle = query.lowercase()
  var idx = 0
  while (idx <= haystack.length - needle.length) {
    val found = haystack.indexOf(needle, idx)
    if (found < 0) break
    result.add(found)
    idx = found + 1
  }
  return result
}

/**
 * Replaces the match at [startOffset] with [replacement].
 * Match length is [queryLength] (original query length in the document).
 */
fun replaceAt(text: String, startOffset: Int, queryLength: Int, replacement: String): String {
  val end = (startOffset + queryLength).coerceAtMost(text.length)
  if (startOffset < 0 || startOffset > text.length) return text
  return text.substring(0, startOffset) + replacement + text.substring(end)
}

/**
 * Replaces all case-insensitive occurrences of [query] with [replacement].
 */
fun replaceAll(text: String, query: String, replacement: String): String {
  if (query.isEmpty()) return text
  // Case-insensitive replace while preserving structure via lowercase scan
  val matches = findMatches(text, query)
  if (matches.isEmpty()) return text
  val sb = StringBuilder()
  var last = 0
  val qLen = query.length
  for (start in matches) {
    sb.append(text.substring(last, start))
    sb.append(replacement)
    last = start + qLen
  }
  sb.append(text.substring(last))
  return sb.toString()
}
