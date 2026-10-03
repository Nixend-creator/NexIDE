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

package com.nexide.platform.ui.build

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexide.platform.core.build.Problem
import com.nexide.platform.core.build.ProblemSeverity
import java.io.File

/**
 * Problems tool window — list of build diagnostics with navigation.
 */
@Composable
fun ProblemsToolWindow(
  problems: List<Problem>,
  building: Boolean,
  visible: Boolean,
  onClose: () -> Unit,
  onNavigate: (Problem) -> Unit,
  onRebuild: () -> Unit,
  modifier: Modifier = Modifier
) {
  if (!visible) return

  val errors = problems.count { it.severity == ProblemSeverity.ERROR }
  val warnings = problems.count { it.severity == ProblemSeverity.WARNING }
  var filter by remember { mutableStateOf(ProblemFilter.ALL) }
  val filtered = when (filter) {
    ProblemFilter.ALL -> problems
    ProblemFilter.ERRORS -> problems.filter { it.severity == ProblemSeverity.ERROR }
    ProblemFilter.WARNINGS -> problems.filter {
      it.severity == ProblemSeverity.WARNING || it.severity == ProblemSeverity.INFO
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 140.dp, max = 280.dp)
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = when {
          building -> "Problems — Building…"
          problems.isEmpty() -> "Problems — No issues"
          else -> "Problems — $errors error(s), $warnings warning(s)"
        },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(1f)
      )
      TextButton(onClick = onRebuild, enabled = !building) {
        Text("Build", style = MaterialTheme.typography.labelMedium)
      }
      TextButton(onClick = onClose) {
        Text("×", style = MaterialTheme.typography.labelLarge)
      }
    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 0.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      FilterChip("All (${problems.size})", filter == ProblemFilter.ALL) {
        filter = ProblemFilter.ALL
      }
      FilterChip("Errors ($errors)", filter == ProblemFilter.ERRORS) {
        filter = ProblemFilter.ERRORS
      }
      FilterChip("Warnings ($warnings)", filter == ProblemFilter.WARNINGS) {
        filter = ProblemFilter.WARNINGS
      }
    }

    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.surface)
    ) {
      if (!building && filtered.isEmpty()) {
        item {
          Text(
            text = if (problems.isEmpty()) {
              "No problems. Click Build to compile the project."
            } else {
              "No problems in this filter."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
          )
        }
      }
      itemsIndexed(filtered, key = { index, p -> "$index:${p.filePath}:${p.line}:${p.message}" }) { _, problem ->
        ProblemRow(problem = problem, onClick = { onNavigate(problem) })
      }
    }
  }
}

private enum class ProblemFilter { ALL, ERRORS, WARNINGS }

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
  Text(
    text = label,
    style = MaterialTheme.typography.labelSmall,
    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  )
}

@Composable
private fun ProblemRow(
  problem: Problem,
  onClick: () -> Unit
) {
  val severityColor = when (problem.severity) {
    ProblemSeverity.ERROR -> Color(0xFFF14C4C)
    ProblemSeverity.WARNING -> Color(0xFFE2C08D)
    ProblemSeverity.INFO -> Color(0xFF73C1DE)
  }
  val severityLabel = when (problem.severity) {
    ProblemSeverity.ERROR -> "E"
    ProblemSeverity.WARNING -> "W"
    ProblemSeverity.INFO -> "I"
  }
  val location = buildString {
    problem.filePath?.let { append(File(it).name) }
    problem.line?.let { append(":$it") }
    problem.column?.let { append(":$it") }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(enabled = problem.filePath != null, onClick = onClick)
      .padding(horizontal = 10.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = severityLabel,
      style = MaterialTheme.typography.labelMedium,
      color = severityColor,
      modifier = Modifier.width(18.dp)
    )
    Text(
      text = location.ifEmpty { "—" },
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.width(140.dp)
    )
    Text(
      text = problem.message,
      style = MaterialTheme.typography.bodySmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )
  }
}
