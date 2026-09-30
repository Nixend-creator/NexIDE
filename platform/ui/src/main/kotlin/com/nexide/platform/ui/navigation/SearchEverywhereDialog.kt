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

package com.nexide.platform.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nexide.platform.core.index.SymbolIndex
import com.nexide.platform.core.index.SymbolInfo
import java.io.File

sealed class EverywhereHit {
  data class FileHit(val path: String) : EverywhereHit()
  data class SymbolHit(val symbol: SymbolInfo) : EverywhereHit()
}

@Composable
fun SearchEverywhereDialog(
  projectRoot: File,
  onOpenFile: (path: String, line: Int?) -> Unit,
  onDismiss: () -> Unit
) {
  var query by remember { mutableStateOf("") }
  var selectedIndex by remember { mutableStateOf(0) }
  val hits = remember(query, projectRoot) {
    search(projectRoot, query)
  }
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    try {
      SymbolIndex.ensureIndexed(projectRoot)
    } catch (_: Exception) {
    }
    focusRequester.requestFocus()
  }
  LaunchedEffect(hits) {
    selectedIndex = selectedIndex.coerceIn(0, (hits.size - 1).coerceAtLeast(0))
  }

  fun openSelected() {
    when (val hit = hits.getOrNull(selectedIndex)) {
      is EverywhereHit.FileHit -> onOpenFile(hit.path, null)
      is EverywhereHit.SymbolHit -> onOpenFile(hit.symbol.filePath, hit.symbol.line)
      null -> Unit
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier
        .width(560.dp)
        .heightIn(max = 480.dp)
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .padding(12.dp)
        .onPreviewKeyEvent { event ->
          if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
          when (event.key) {
            Key.DirectionDown -> {
              if (hits.isNotEmpty()) selectedIndex = (selectedIndex + 1) % hits.size
              true
            }
            Key.DirectionUp -> {
              if (hits.isNotEmpty()) selectedIndex = (selectedIndex - 1 + hits.size) % hits.size
              true
            }
            Key.Enter -> {
              openSelected()
              true
            }
            Key.Escape -> {
              onDismiss()
              true
            }
            else -> false
          }
        }
    ) {
      Text("Search Everywhere", style = MaterialTheme.typography.titleSmall)
      BasicTextField(
        value = query,
        onValueChange = {
          query = it
          selectedIndex = 0
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
          .padding(8.dp)
          .focusRequester(focusRequester)
      )
      if (hits.isEmpty()) {
        Text(
          if (query.isBlank()) "Type to search files and symbols" else "No results",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(8.dp)
        )
      } else {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
          itemsIndexed(hits) { index, hit ->
            val bg = if (index == selectedIndex) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surface
            }
            val (title, subtitle, kind) = when (hit) {
              is EverywhereHit.FileHit -> Triple(
                File(hit.path).name,
                hit.path,
                "file"
              )
              is EverywhereHit.SymbolHit -> Triple(
                hit.symbol.name,
                "${hit.symbol.kind} · ${File(hit.symbol.filePath).name}:${hit.symbol.line}",
                hit.symbol.kind
              )
            }
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(bg)
                .clickable {
                  selectedIndex = index
                  when (hit) {
                    is EverywhereHit.FileHit -> onOpenFile(hit.path, null)
                    is EverywhereHit.SymbolHit -> onOpenFile(hit.symbol.filePath, hit.symbol.line)
                  }
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = kind,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 8.dp).width(56.dp)
              )
              Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  subtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun search(projectRoot: File, query: String): List<EverywhereHit> {
  val q = query.trim().lowercase()
  if (q.isEmpty()) return emptyList()
  val result = mutableListOf<EverywhereHit>()
  val skip = setOf(".git", ".gradle", "build", "out", "node_modules", ".idea")

  // Symbols first
  try {
    SymbolIndex.ensureIndexed(projectRoot)
    SymbolIndex.lookup(q, 25).forEach { result.add(EverywhereHit.SymbolHit(it)) }
  } catch (_: Exception) {
  }

  // Files by name
  try {
    projectRoot.walkTopDown()
      .onEnter { it.name !in skip }
      .filter { it.isFile }
      .filter { it.name.lowercase().contains(q) }
      .take(25)
      .forEach { result.add(EverywhereHit.FileHit(it.absolutePath)) }
  } catch (_: Exception) {
  }

  return result.take(40)
}
