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

package com.nexide.platform.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexide.platform.core.RecentProjects

/**
 * Welcome / empty-editor screen with recent projects and open action.
 */
@Composable
fun WelcomeScreen(
  currentProjectPath: String?,
  onOpenProject: () -> Unit,
  onOpenRecent: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val recent = remember {
    RecentProjects.list()
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(
      text = "NexIDE",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = "Open a file from the Project View, or open another project.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(24.dp))

    TextButton(onClick = onOpenProject) {
      Text("Open Project…", style = MaterialTheme.typography.titleSmall)
    }

    if (recent.isNotEmpty()) {
      Spacer(modifier = Modifier.height(32.dp))
      Text(
        text = "Recent Projects",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
          .widthIn(max = 420.dp)
          .fillMaxWidth()
      )
      Spacer(modifier = Modifier.height(8.dp))
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 420.dp)
          .fillMaxWidth()
          .height(200.dp)
      ) {
        items(recent, key = { it }) { path ->
          val isCurrent = path == currentProjectPath
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = !isCurrent) { onOpenRecent(path) }
              .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = RecentProjects.displayName(path),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
              color = if (isCurrent) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurface
              },
              modifier = Modifier.weight(1f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = path,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1.5f)
            )
          }
        }
      }
    }
  }
}
