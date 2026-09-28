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
 * Simple find bar: query input, match count, next/prev, close.
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
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  Row(
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
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = "Find:",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSecondaryContainer
    )
    Spacer(modifier = Modifier.width(8.dp))

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
