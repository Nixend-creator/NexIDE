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

package com.nexide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nexide.platform.core.Application
import com.nexide.platform.util.Logger

fun main() {
  Logger.info("Starting NexIDE...")
  val application = Application.create()
  application.start()

  application {
    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

    Window(
      onCloseRequest = {
        application.exit()
        exitApplication()
      },
      title = "NexIDE",
      state = windowState
    ) {
      NexIdeApp()
    }
  }
}

@Composable
fun NexIdeApp() {
  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "Welcome to NexIDE\n\nModern IDE inspired by IntelliJ IDEA",
          style = MaterialTheme.typography.headlineMedium
        )
      }
    }
  }
}
