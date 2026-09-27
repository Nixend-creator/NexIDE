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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nexide.platform.core.Application
import com.nexide.platform.core.vfs.LocalFileSystem
import com.nexide.platform.editor.Editor
import com.nexide.platform.editor.ui.EditorView
import com.nexide.platform.util.Logger
import java.io.File

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
  // Demo: create a temporary file and open it in the editor
  val editor = remember {
    val tempFile = File.createTempFile("nexide-demo", ".kt")
    tempFile.writeText(
      """
      // Welcome to NexIDE
      // This is a basic editor backed by VirtualFile + Document
      
      fun main() {
          println("Hello from NexIDE!")
      }
      """.trimIndent()
    )
    val virtualFile = LocalFileSystem.getInstance().findFileByPath(tempFile.absolutePath)!!
    Editor.create(virtualFile)
  }

  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      EditorView(editor = editor, modifier = Modifier.fillMaxSize())
    }
  }
}
