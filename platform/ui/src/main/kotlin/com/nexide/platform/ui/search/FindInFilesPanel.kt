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

package com.nexide.platform.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexide.platform.core.vfs.VirtualFile
import java.util.concurrent.Executors
import javax.swing.SwingUtilities

data class SearchHit(
  val file: VirtualFile,
  val lineNumber: Int,
  val lineText: String,
  val matchStartInLine: Int
)

private val SKIP_DIRS = setOf(
  "build", "out", ".gradle", ".git", "node_modules", ".idea", "bin", "obj"
)

private val TEXT_EXTENSIONS = setOf(
  "kt", "kts", "java", "xml", "gradle", "properties", "md", "txt",
  "json", "yml", "yaml", "toml", "html", "css", "js", "ts", "py",
  "c", "cpp", "h", "hpp", "rs", "go", "sh", "bat", "csv"
)

/**
 * Recursively searches text files under [root] for [query] (case-insensitive).
 */
fun searchInFiles(
  root: VirtualFile,
  query: String,
  maxResults: Int = 200
): List<SearchHit> {
  if (query.isEmpty()) return emptyList()
  val needle = query.lowercase()
  val results = mutableListOf<SearchHit>()

  fun visit(file: VirtualFile) {
    if (results.size >= maxResults) return
    if (!file.isValid) return

    if (file.isDirectory) {
      if (file.name.startsWith(".") || file.name in SKIP_DIRS) return
      for (child in file.getChildren()) {
        visit(child)
        if (results.size >= maxResults) return
      }
      return
    }

    val ext = file.name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    if (ext.isNotEmpty() && ext !in TEXT_EXTENSIONS) return

    val text = try {
      file.contentsToString()
    } catch (_: Exception) {
      return
    }
    // Skip likely-binary
    if (text.indexOf('\u0000') >= 0) return

    val lines = text.split('\n')
    for ((index, line) in lines.withIndex()) {
      if (results.size >= maxResults) return
      val lower = line.lowercase()
      var from = 0
      while (true) {
        val at = lower.indexOf(needle, from)
        if (at < 0) break
        results.add(
          SearchHit(
            file = file,
            lineNumber = index + 1,
            lineText = line.trimEnd(),
            matchStartInLine = at
          )
        )
        if (results.size >= maxResults) return
        from = at + 1
      }
    }
  }

  visit(root)
  return results
}

@Composable
fun FindInFilesPanel(
  root: VirtualFile?,
  visible: Boolean,
  onClose: () -> Unit,
  onOpenHit: (SearchHit) -> Unit,
  modifier: Modifier = Modifier
) {
  if (!visible) return

  var query by remember { mutableStateOf("") }
  var results by remember { mutableStateOf<List<SearchHit>>(emptyList()) }
  var searching by remember { mutableStateOf(false) }
  var status by remember { mutableStateOf("") }
  val focusRequester = remember { FocusRequester() }
  val executor = remember { Executors.newSingleThreadExecutor() }

  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  fun doSearch() {
    val r = root
    if (r == null || query.isBlank()) {
      results = emptyList()
      status = ""
      return
    }
    searching = true
    status = "Searching…"
    val q = query
    executor.execute {
      val hits = try {
        searchInFiles(r, q)
      } catch (e: Exception) {
        emptyList()
      }
      SwingUtilities.invokeLater {
        results = hits
        searching = false
        status = when {
          hits.isEmpty() -> "No results"
          hits.size >= 200 -> "200+ results (showing first 200)"
          else -> "${hits.size} result(s)"
        }
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 180.dp, max = 320.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    // Header + query
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Find in Files",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Spacer(modifier = Modifier.width(12.dp))
      BasicTextField(
        value = query,
        onValueChange = { query = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .weight(1f)
          .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .focusRequester(focusRequester)
          .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown &&
              (event.key == Key.Enter || event.key == Key.NumPadEnter)
            ) {
              doSearch()
              true
            } else {
              false
            }
          }
      )
      TextButton(onClick = { doSearch() }, enabled = !searching && query.isNotBlank()) {
        Text("Search", style = MaterialTheme.typography.labelMedium)
      }
      Text(
        text = status,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp)
      )
      TextButton(onClick = onClose) {
        Text("×", style = MaterialTheme.typography.labelLarge)
      }
    }

    // Results
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      items(results, key = { "${it.file.path}:${it.lineNumber}:${it.matchStartInLine}" }) { hit ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenHit(hit) }
            .padding(horizontal = 12.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "${hit.file.name}:${hit.lineNumber}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(160.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = hit.lineText.trim().take(120),
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}
