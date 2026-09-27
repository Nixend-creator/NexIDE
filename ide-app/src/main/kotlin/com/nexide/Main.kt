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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nexide.platform.core.Application
import com.nexide.platform.core.vfs.LocalFileSystem
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.editor.Editor
import com.nexide.platform.editor.ui.EditorView
import com.nexide.platform.plugin.PluginManager
import com.nexide.platform.plugin.example.CoreFileTypesPlugin
import com.nexide.platform.plugin.example.FILE_TYPE_EP
import com.nexide.platform.ui.projectview.ProjectView
import com.nexide.platform.util.Logger
import java.io.File

fun main() {
  Logger.info("Starting NexIDE...")
  val application = Application.create()
  application.start()

  // Load built-in plugins
  val pluginManager = PluginManager.getInstance()
  pluginManager.loadPlugin(CoreFileTypesPlugin())

  Logger.info("Registered file types: ${FILE_TYPE_EP.extensionList.map { it.name }}")

  application {
    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)

    Window(
      onCloseRequest = {
        application.exit()
        exitApplication()
      },
      title = "NexIDE",
      state = windowState
    ) {
      NexIdeApp(application)
    }
  }
}

@Composable
fun NexIdeApp(application: Application) {
  // Open the NexIDE project itself as a demo project
  val projectRootPath = remember {
    // Try to find the project root (works when run from the repo)
    val candidates = listOf(
      File(".").absoluteFile,
      File("..").absoluteFile,
      File(System.getProperty("user.dir") ?: ".")
    )
    candidates.firstOrNull { dir ->
      File(dir, "settings.gradle.kts").exists() || File(dir, "platform").exists()
    }?.absolutePath ?: File(".").absolutePath
  }

  val project = remember {
    application.openProject(projectRootPath)
  }

  val rootVirtualFile = project.baseDir

  var selectedFile by remember { mutableStateOf<VirtualFile?>(null) }
  var currentEditor by remember { mutableStateOf<Editor?>(null) }

  fun openFile(file: VirtualFile) {
    selectedFile = file
    currentEditor = Editor.create(file)
    Logger.info("Opened in editor: ${file.path}")
  }

  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      Row(modifier = Modifier.fillMaxSize()) {
        // Left: Project View
        ProjectView(
          root = rootVirtualFile,
          selectedFile = selectedFile,
          onFileSelected = { openFile(it) },
          modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
        )

        VerticalDivider()

        // Right: Editor
        val editor = currentEditor
        if (editor != null) {
          EditorView(
            editor = editor,
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
          )
        } else {
          // Empty state
          androidx.compose.foundation.layout.Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
            contentAlignment = androidx.compose.ui.Alignment.Center
          ) {
            androidx.compose.material3.Text(
              text = "Select a file in the Project View",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}
