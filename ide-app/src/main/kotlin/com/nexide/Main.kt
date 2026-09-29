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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nexide.platform.core.Application
import com.nexide.platform.core.Project
import com.nexide.platform.core.RecentProjects
import com.nexide.platform.core.run.GradleRunSupport
import com.nexide.platform.core.run.ProcessRunner
import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.editor.Editor
import com.nexide.platform.editor.ui.EditorView
import com.nexide.platform.plugin.PluginManager
import com.nexide.platform.plugin.example.CoreFileTypesPlugin
import com.nexide.platform.plugin.example.FILE_TYPE_EP
import com.nexide.platform.ui.dialog.CloseConfirmResult
import com.nexide.platform.ui.dialog.ConfirmCloseDialog
import com.nexide.platform.plugin.filetype.FileTypeRegistry
import com.nexide.platform.ui.git.GitToolWindow
import com.nexide.platform.ui.projectview.ProjectView
import com.nexide.platform.ui.run.RunToolWindow
import com.nexide.platform.ui.search.FindInFilesPanel
import com.nexide.platform.ui.search.SearchHit
import com.nexide.platform.ui.structure.StructureToolWindow
import com.nexide.platform.ui.tabs.EditorTabs
import com.nexide.platform.ui.terminal.TerminalToolWindow
import com.nexide.platform.core.debug.BreakpointManager
import com.nexide.platform.core.debug.DebugSessionManager
import com.nexide.platform.core.debug.JdiConnector
import com.nexide.platform.ui.debug.BreakpointsToolWindow
import com.nexide.platform.ui.debug.DebugToolWindow
import com.nexide.platform.ui.settings.SettingsDialog
import com.nexide.platform.ui.welcome.WelcomeScreen
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
    val processRunner = remember { ProcessRunner() }
    val debugSessionManager = remember { DebugSessionManager(processRunner) }
    val jdiConnector = remember { JdiConnector() }
    var runPanelVisible by mutableStateOf(false)
    var debugPanelVisible by mutableStateOf(false)
    var terminalVisible by mutableStateOf(false)
    var gitPanelVisible by mutableStateOf(false)
    var structureVisible by mutableStateOf(false)
    var findInFilesVisible by mutableStateOf(false)
    var settingsVisible by mutableStateOf(false)
    var settingsRevision by mutableStateOf(0)
    var breakpointsVisible by mutableStateOf(false)
    // When opening a search hit, request go-to-line after editor opens
    var pendingGoToLine by mutableStateOf<Pair<Editor, Int>?>(null)

    fun runGradle(task: String) {
      val dir = File(project.basePath)
      if (!GradleRunSupport.isGradleProject(dir)) {
        Logger.warn("Not a Gradle project: ${dir.absolutePath}")
        runPanelVisible = true
        return
      }
      val gradle = GradleRunSupport.findGradleCommand(dir) ?: return
      runPanelVisible = true
      processRunner.start(gradle + task, dir)
    }

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
      RecentProjects.add(path)
      Logger.info("Opened project: $path")
    }

    // Record the default project as recent on startup
    RecentProjects.add(project.basePath)

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
          ctrlOrMeta && event.isShiftPressed && event.key == Key.F -> {
            findInFilesVisible = true
            true
          }
          else -> false
        }
      }
    ) {
      MenuBar {
        Menu("File", mnemonic = 'F') {
          Item("Open Project…", onClick = { chooseAndOpenProject() }, shortcut = null)
          val recent = RecentProjects.list()
            .filter { it != project.basePath }
          if (recent.isNotEmpty()) {
            Menu("Open Recent", mnemonic = 'R') {
              recent.forEach { path ->
                Item(
                  RecentProjects.displayName(path),
                  onClick = { openProjectFromPath(path) }
                )
              }
              Separator()
              Item("Clear List", onClick = {
                RecentProjects.list().forEach { RecentProjects.remove(it) }
              })
            }
          }
          Item(
            "Save",
            enabled = activeEditor != null,
            onClick = { saveActive() }
          )
          Separator()
          Item("Settings…", onClick = { settingsVisible = true })
          Separator()
          Item("Exit", onClick = {
            application.exit()
            exitApplication()
          })
        }
        Menu("Run", mnemonic = 'R') {
          Item("Gradle: build", onClick = { runGradle("build") })
          Item("Gradle: test", onClick = { runGradle("test") })
          Item("Gradle: tasks", onClick = { runGradle("tasks") })
          Separator()
          Item(
            "Debug Gradle: build",
            onClick = {
              debugPanelVisible = true
              debugSessionManager.startGradleDebug(File(project.basePath), "build")
            }
          )
          Item(
            "Debug Gradle: test",
            onClick = {
              debugPanelVisible = true
              debugSessionManager.startGradleDebug(File(project.basePath), "test")
            }
          )
          Separator()
          Item(
            "Toggle Breakpoint",
            enabled = activeEditor != null,
            onClick = {
              val ed = activeEditor ?: return@Item
              BreakpointManager.getInstance().toggle(ed.file.path, 1)
              breakpointsVisible = true
            }
          )
          Item(
            "View Breakpoints",
            onClick = { breakpointsVisible = true }
          )
          Item(
            "Show Debug Tool Window",
            onClick = { debugPanelVisible = true }
          )
          Separator()
          Item(
            "Stop",
            enabled = processRunner.isRunning || debugSessionManager.isActive,
            onClick = {
              if (debugSessionManager.isActive) debugSessionManager.stop()
              else processRunner.stop()
            }
          )
          Item(
            "Show Run Tool Window",
            onClick = { runPanelVisible = true }
          )
        }
        Menu("View", mnemonic = 'V') {
          Item("Find in Files", onClick = { findInFilesVisible = true })
          Item("Structure", onClick = { structureVisible = true })
          Item("Breakpoints", onClick = { breakpointsVisible = true })
          Item("Git", onClick = { gitPanelVisible = true })
          Item("Terminal", onClick = { terminalVisible = true })
          Item("Run Tool Window", onClick = { runPanelVisible = true })
        }
      }

      if (settingsVisible) {
        SettingsDialog(
          onDismiss = { settingsVisible = false },
          onApplied = { settingsRevision++ }
        )
      }

      NexIdeApp(
        project = project,
        openEditors = openEditors,
        activeEditor = activeEditor,
        selectedFile = selectedFile,
        pendingCloseEditor = pendingCloseEditor,
        processRunner = processRunner,
        debugSessionManager = debugSessionManager,
        jdiConnector = jdiConnector,
        runPanelVisible = runPanelVisible,
        onRunPanelVisibleChange = { runPanelVisible = it },
        debugPanelVisible = debugPanelVisible,
        onDebugPanelVisibleChange = { debugPanelVisible = it },
        terminalVisible = terminalVisible,
        onTerminalVisibleChange = { terminalVisible = it },
        gitPanelVisible = gitPanelVisible,
        onGitPanelVisibleChange = { gitPanelVisible = it },
        breakpointsVisible = breakpointsVisible,
        onBreakpointsVisibleChange = { breakpointsVisible = it },
        onBreakpointNavigate = { filePath, line ->
          val file = com.nexide.platform.core.vfs.LocalFileSystem.getInstance()
            .findFileByPath(filePath)
          if (file != null) {
            selectedFile = file
            val existing = openEditors.firstOrNull { it.file.path == file.path }
            val editor = if (existing != null) {
              activeEditor = existing
              existing
            } else {
              val created = Editor.create(file)
              openEditors.add(created)
              activeEditor = created
              created
            }
            pendingGoToLine = editor to line
          }
        },
        structureVisible = structureVisible,
        onStructureVisibleChange = { structureVisible = it },
        findInFilesVisible = findInFilesVisible,
        onFindInFilesVisibleChange = { findInFilesVisible = it },
        settingsRevision = settingsRevision,
        pendingGoToLine = pendingGoToLine,
        onPendingGoToLineHandled = { pendingGoToLine = null },
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
        onOpenSearchHit = { hit: SearchHit ->
          selectedFile = hit.file
          val existing = openEditors.firstOrNull { it.file.path == hit.file.path }
          val editor = if (existing != null) {
            activeEditor = existing
            existing
          } else {
            val created = Editor.create(hit.file)
            openEditors.add(created)
            activeEditor = created
            created
          }
          pendingGoToLine = editor to hit.lineNumber
        },
        onNavigateToLine = { editor, line ->
          activeEditor = editor
          pendingGoToLine = editor to line
        },
        onOpenProject = { chooseAndOpenProject() },
        onOpenRecent = { path -> openProjectFromPath(path) },
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
  processRunner: ProcessRunner,
  debugSessionManager: DebugSessionManager,
  jdiConnector: JdiConnector,
  runPanelVisible: Boolean,
  onRunPanelVisibleChange: (Boolean) -> Unit,
  debugPanelVisible: Boolean = false,
  onDebugPanelVisibleChange: (Boolean) -> Unit = {},
  terminalVisible: Boolean,
  onTerminalVisibleChange: (Boolean) -> Unit,
  gitPanelVisible: Boolean,
  onGitPanelVisibleChange: (Boolean) -> Unit,
  breakpointsVisible: Boolean = false,
  onBreakpointsVisibleChange: (Boolean) -> Unit = {},
  onBreakpointNavigate: (filePath: String, line: Int) -> Unit = { _, _ -> },
  structureVisible: Boolean,
  onStructureVisibleChange: (Boolean) -> Unit,
  findInFilesVisible: Boolean,
  onFindInFilesVisibleChange: (Boolean) -> Unit,
  settingsRevision: Int = 0,
  pendingGoToLine: Pair<Editor, Int>?,
  onPendingGoToLineHandled: () -> Unit,
  onOpenFile: (VirtualFile) -> Unit,
  onOpenSearchHit: (SearchHit) -> Unit,
  onNavigateToLine: (Editor, Int) -> Unit,
  onOpenProject: () -> Unit,
  onOpenRecent: (String) -> Unit,
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
      Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f).fillMaxSize()) {
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
              val gotoLine = pendingGoToLine?.takeIf { it.first === editor }?.second
              EditorView(
                editor = editor,
                modifier = Modifier.weight(1f).fillMaxSize(),
                gotoLineRequest = gotoLine,
                onGotoLineHandled = onPendingGoToLineHandled,
                projectRootPath = project.basePath,
                settingsRevision = settingsRevision
              )
            } else {
              WelcomeScreen(
                currentProjectPath = project.basePath,
                onOpenProject = onOpenProject,
                onOpenRecent = onOpenRecent,
                modifier = Modifier.weight(1f).fillMaxSize()
              )
            }
          }

          if (structureVisible) {
            VerticalDivider()
            val editor = activeEditor
            StructureToolWindow(
              fileName = editor?.file?.name,
              text = editor?.document?.getText(),
              language = editor?.file?.name?.let { FileTypeRegistry.getLanguageByFileName(it) },
              visible = true,
              onClose = { onStructureVisibleChange(false) },
              onNavigate = { line ->
                if (editor != null) {
                  onNavigateToLine(editor, line)
                }
              }
            )
          }
        }

        if (findInFilesVisible) {
          HorizontalDivider()
          FindInFilesPanel(
            root = project.baseDir,
            visible = true,
            onClose = { onFindInFilesVisibleChange(false) },
            onOpenHit = onOpenSearchHit,
            modifier = Modifier.fillMaxWidth()
          )
        }
        if (breakpointsVisible) {
          HorizontalDivider()
          BreakpointsToolWindow(
            visible = true,
            onClose = { onBreakpointsVisibleChange(false) },
            onNavigate = onBreakpointNavigate,
            modifier = Modifier.fillMaxWidth()
          )
        }
        if (debugPanelVisible) {
          HorizontalDivider()
          DebugToolWindow(
            sessionManager = debugSessionManager,
            jdiConnector = jdiConnector,
            visible = true,
            onClose = { onDebugPanelVisibleChange(false) },
            modifier = Modifier.fillMaxWidth()
          )
        }
        if (runPanelVisible) {
          HorizontalDivider()
          RunToolWindow(
            runner = processRunner,
            visible = true,
            onClose = { onRunPanelVisibleChange(false) },
            modifier = Modifier.fillMaxWidth()
          )
        }
        if (gitPanelVisible) {
          HorizontalDivider()
          GitToolWindow(
            projectRoot = project.basePath,
            visible = true,
            onClose = { onGitPanelVisibleChange(false) },
            modifier = Modifier.fillMaxWidth()
          )
        }
        if (terminalVisible) {
          HorizontalDivider()
          TerminalToolWindow(
            projectRoot = project.basePath,
            visible = true,
            onClose = { onTerminalVisibleChange(false) },
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }
  }
}
