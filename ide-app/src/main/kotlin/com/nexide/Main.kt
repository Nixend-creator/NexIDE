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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nexide.platform.core.Application
import com.nexide.platform.core.Project
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.editor.Editor
import com.nexide.platform.editor.ui.EditorView
import com.nexide.platform.plugin.PluginManager
import com.nexide.platform.plugin.example.CoreFileTypesPlugin
import com.nexide.platform.plugin.example.FILE_TYPE_EP
import com.nexide.platform.ui.dialog.CloseConfirmResult
import com.nexide.platform.ui.dialog.ConfirmCloseDialog
import com.nexide.platform.ui.projectview.ProjectView
import com.nexide.platform.ui.tabs.EditorTabs
import com.nexide.platform.util.Logger
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager

fun main() {
  Logger.info("Starting NexIDE...")
  val application = Application.create()
  application.start()

  val pluginManager = PluginManager.getInstance()
  pluginManager.loadPlugin(CoreFileTypesPlugin())
  Logger.info("Registered file types: ${FILE_TYPE_EP.extensionList.map { it.name }}")

  application {
    val windowState = rememberWindowState(width = 1400.dp, height = 900.dp)

    // Default: open current working directory / NexIDE repo if detected
    val defaultPath = run {
      val candidates = listOf(
        File(".").absoluteFile,
        File("..").absoluteFile,
        File(System.getProperty("user.dir") ?: ".")
      )
      candidates.firstOrNull { dir ->
        File(dir, "settings.gradle.kts").exists() || File(dir, "platform").exists()
      }?.absolutePath ?: File(".").absolutePath
    }

    var project by mutableStateOf(application.openProject(defaultPath))
    val openEditors = mutableStateListOf<Editor>()
    var activeEditor by mutableStateOf<Editor?>(null)
    var selectedFile by mutableStateOf<VirtualFile?>(null)
    var pendingCloseEditor by mutableStateOf<Editor?>(null)

    fun doCloseEditor(editor: Editor) {
      val index = openEditors.indexOf(editor)
      openEditors.remove(editor)
      editor.dispose()
      if (activeEditor === editor) {
        activeEditor = when {
          openEditors.isEmpty() -> null
          index < openEditors.size -> openEditors[index]
          else -> openEditors.lastOrNull()
        }
        selectedFile = activeEditor?.file
      }
    }

    fun closeAllEditors() {
      openEditors.toList().forEach { doCloseEditor(it) }
    }

    fun openProjectFromPath(path: String) {
      closeAllEditors()
      project = application.openProject(path)
      selectedFile = null
      activeEditor = null
      Logger.info("Opened project: $path")
    }

    fun chooseAndOpenProject() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      } catch (_: Exception) {
        // ignore
      }
      val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Open Project"
        currentDirectory = File(project.basePath)
      }
      val result = chooser.showOpenDialog(null)
      if (result == JFileChooser.APPROVE_OPTION) {
        openProjectFromPath(chooser.selectedFile.absolutePath)
      }
    }

    fun saveActive() {
      activeEditor?.save()
    }

    Window(
      onCloseRequest = {
        application.exit()
        exitApplication()
      },
      title = "NexIDE — ${project.name}",
      state = windowState,
      onKeyEvent = { event ->
        if (event.type != KeyEventType.KeyDown) return@Window false
        val ctrlOrMeta = event.isCtrlPressed || event.isMetaPressed
        when {
          ctrlOrMeta && event.key == Key.O -> {
            chooseAndOpenProject()
            true
          }
          ctrlOrMeta && event.key == Key.S -> {
            saveActive()
            true
          }
          else -> false
        }
      }
    ) {
      MenuBar {
        Menu("File", mnemonic = 'F') {
          Item("Open Project…", onClick = { chooseAndOpenProject() }, shortcut = null)
          Item(
            "Save",
            enabled = activeEditor != null,
            onClick = { saveActive() }
          )
          Separator()
          Item("Exit", onClick = {
            application.exit()
            exitApplication()
          })
        }
      }

      NexIdeApp(
        project = project,
        openEditors = openEditors,
        activeEditor = activeEditor,
        selectedFile = selectedFile,
        pendingCloseEditor = pendingCloseEditor,
        onOpenFile = { file ->
          selectedFile = file
          val existing = openEditors.firstOrNull { it.file.path == file.path }
          if (existing != null) {
            activeEditor = existing
          } else {
            val editor = Editor.create(file)
            openEditors.add(editor)
            activeEditor = editor
            Logger.info("Opened tab: ${file.path}")
          }
        },
        onSelectEditor = {
          activeEditor = it
          selectedFile = it.file
        },
        onRequestClose = { editor ->
          if (editor.isModified) {
            pendingCloseEditor = editor
          } else {
            doCloseEditor(editor)
          }
        },
        onCloseConfirm = { editor, result ->
          when (result) {
            CloseConfirmResult.SAVE -> {
              editor.save()
              doCloseEditor(editor)
              pendingCloseEditor = null
            }
            CloseConfirmResult.DONT_SAVE -> {
              doCloseEditor(editor)
              pendingCloseEditor = null
            }
            CloseConfirmResult.CANCEL -> {
              pendingCloseEditor = null
            }
          }
        }
      )
    }
  }
}

@Composable
fun NexIdeApp(
  project: Project,
  openEditors: List<Editor>,
  activeEditor: Editor?,
  selectedFile: VirtualFile?,
  pendingCloseEditor: Editor?,
  onOpenFile: (VirtualFile) -> Unit,
  onSelectEditor: (Editor) -> Unit,
  onRequestClose: (Editor) -> Unit,
  onCloseConfirm: (Editor, CloseConfirmResult) -> Unit
) {
  MaterialTheme {
    if (pendingCloseEditor != null) {
      ConfirmCloseDialog(fileName = pendingCloseEditor.file.name) { result ->
        onCloseConfirm(pendingCloseEditor, result)
      }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
      Row(modifier = Modifier.fillMaxSize()) {
        ProjectView(
          root = project.baseDir,
          selectedFile = selectedFile,
          onFileSelected = onOpenFile,
          modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
        )

        VerticalDivider()

        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
          EditorTabs(
            editors = openEditors,
            activeEditor = activeEditor,
            onSelect = onSelectEditor,
            onClose = onRequestClose
          )

          val editor = activeEditor
          if (editor != null) {
            EditorView(
              editor = editor,
              modifier = Modifier.weight(1f).fillMaxSize()
            )
          } else {
            Box(
              modifier = Modifier.weight(1f).fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Open a project (File → Open Project) or select a file",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  }
}
