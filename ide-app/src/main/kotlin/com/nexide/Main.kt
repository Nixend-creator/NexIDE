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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import com.nexide.platform.core.state.IdeStateStore
import com.nexide.platform.core.index.SymbolIndex
import com.nexide.platform.core.notify.NotificationService
import com.nexide.platform.core.history.LocalHistory
import com.nexide.platform.core.Project
import com.nexide.platform.core.RecentProjects
import com.nexide.platform.core.build.Problem
import com.nexide.platform.core.build.ProblemParser
import com.nexide.platform.core.navigation.GotoDeclaration
import com.nexide.platform.core.navigation.NavigationTarget
import com.nexide.platform.core.run.GradleRunSupport
import com.nexide.platform.core.run.ProcessOutputListener
import com.nexide.platform.core.run.ProcessRunner
import com.nexide.platform.core.run.RunConfiguration
import com.nexide.platform.core.run.RunConfigurationManager
import com.nexide.platform.core.vfs.LocalFileSystem
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
import com.nexide.platform.ui.build.ProblemsToolWindow
import com.nexide.platform.ui.navigation.GotoTargetsDialog
import com.nexide.platform.ui.refactor.RenameDialog
import com.nexide.platform.ui.run.RunConfigDialog
import com.nexide.platform.ui.run.RunToolWindow
import com.nexide.platform.ui.search.FindInFilesPanel
import com.nexide.platform.ui.search.SearchHit
import com.nexide.platform.ui.structure.StructureToolWindow
import com.nexide.platform.ui.tabs.EditorTabs
import com.nexide.platform.ui.terminal.TerminalToolWindow
import com.nexide.platform.core.debug.BreakpointManager
import com.nexide.platform.core.debug.DebugSessionManager
import com.nexide.platform.core.debug.JdiConnector
import com.nexide.platform.core.debug.JdiListener
import com.nexide.platform.core.debug.SuspendedThreadInfo
import javax.swing.SwingUtilities
import com.nexide.platform.ui.debug.BreakpointsToolWindow
import com.nexide.platform.ui.debug.DebugToolWindow
import com.nexide.platform.ui.settings.SettingsDialog
import com.nexide.platform.ui.welcome.WelcomeScreen
import com.nexide.platform.ui.notify.NotificationHost
import com.nexide.platform.ui.plugins.MarketplaceDialog
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
    var darkTheme by mutableStateOf(IdeStateStore.darkTheme)
    var splitEditor by mutableStateOf(IdeStateStore.splitEditor)
    var marketplaceVisible by mutableStateOf(false)

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
    var runConfigDialogVisible by mutableStateOf(false)
    var problemsVisible by mutableStateOf(false)
    var problems by mutableStateOf<List<Problem>>(emptyList())
    var building by mutableStateOf(false)
    var gotoTargets by mutableStateOf<Pair<String, List<NavigationTarget>>?>(null)
    var gotoDialogTitle by mutableStateOf("Go to Declaration")
    var renameSymbol by mutableStateOf<String?>(null)
    // When opening a search hit, request go-to-line after editor opens
    var pendingGoToLine by mutableStateOf<Pair<Editor, Int>?>(null)

    val runConfigManager = remember { RunConfigurationManager.getInstance() }
    // Seed defaults for current project
    runConfigManager.ensureDefaults(File(project.basePath))
    // Restore tabs from last session
    if (openEditors.isEmpty()) {
      IdeStateStore.openTabs.forEach { path ->
        try {
          val vf = LocalFileSystem.getInstance().findFileByPath(path)
          if (vf != null && !vf.isDirectory) {
            openEditors.add(Editor.create(vf))
          }
        } catch (_: Exception) {}
      }
      val activePath = IdeStateStore.activeTabPath
      activeEditor = openEditors.firstOrNull { it.file.path == activePath } ?: openEditors.firstOrNull()
      selectedFile = activeEditor?.file
    }


    fun buildProject() {
      val dir = File(project.basePath)
      if (!GradleRunSupport.isGradleProject(dir)) {
        Logger.warn("Not a Gradle project")
        problemsVisible = true
        return
      }
      val gradle = GradleRunSupport.findGradleCommand(dir) ?: return
      problems = emptyList()
      building = true
      problemsVisible = true
      val buffer = StringBuilder()
      val listener = object : ProcessOutputListener {
        override fun onOutput(text: String) {
          buffer.append(text)
        }

        override fun onFinished(exitCode: Int) {
          processRunner.removeListener(this)
          val parsed = ProblemParser.parseOutput(buffer.toString())
          // Resolve relative paths against project root
          problems = parsed.map { p ->
            val path = p.filePath
            if (path != null && !File(path).isAbsolute) {
              p.copy(filePath = File(dir, path).canonicalPath)
            } else {
              p
            }
          }
          building = false
          Logger.info("Build finished ($exitCode): ${problems.size} problem(s)")
          if (problems.isEmpty()) NotificationService.success("Build OK")
          else NotificationService.warn("Build", "${problems.size} problem(s)")
        }
      }
      processRunner.addListener(listener)
      processRunner.start(gradle + "build", dir)
    }


    fun jumpToSource(info: SuspendedThreadInfo) {
      val name = info.sourceName ?: return
      val line = info.lineNumber ?: return
      val root = File(project.basePath)
      val match = root.walkTopDown()
        .onEnter { d -> d.name !in setOf(".git", ".gradle", "build", "out", "node_modules") }
        .firstOrNull { it.isFile && it.name == name }
        ?: return
      val file = LocalFileSystem.getInstance().findFileByPath(match.absolutePath) ?: return
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
      Logger.info("Debug jump: ${match.name}:$line")
    }

    fun autoAttachAndInstallBps() {
      Thread({
        // Wait for JDWP to come up
        var attached = false
        repeat(20) {
          if (jdiConnector.isAttached) {
            attached = true
            return@repeat
          }
          Thread.sleep(500)
          if (jdiConnector.attach(port = debugSessionManager.debugPort)) {
            attached = true
            return@repeat
          }
        }
        if (!attached) {
          Logger.warn("Auto-attach failed — use Attach manually")
          return@Thread
        }
        Thread.sleep(300)
        val n = jdiConnector.installBreakpoints(BreakpointManager.getInstance().getAll())
        Logger.info("Auto-install breakpoints: $n")
      }, "nexide-auto-attach").apply {
        isDaemon = true
        start()
      }
    }

    fun runConfiguration(config: RunConfiguration, debug: Boolean = false) {
      val dir = File(config.workingDir.ifBlank { project.basePath })
      val command = runConfigManager.resolveCommand(
        config.copy(workingDir = dir.absolutePath, debug = debug)
      )
      if (command == null) {
        Logger.warn("Cannot resolve run configuration: ${config.name}")
        return
      }
      if (debug) {
        debugPanelVisible = true
        val ok = debugSessionManager.startDebug(command, dir)
        if (ok) autoAttachAndInstallBps()
      } else {
        runPanelVisible = true
        processRunner.start(command, dir)
      }
      Logger.info("${if (debug) "Debug" else "Run"}: ${config.name} → ${command.joinToString(" ")}")
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

    fun requestCloseEditor(editor: Editor) {
      if (editor.isModified) {
        pendingCloseEditor = editor
      } else {
        doCloseEditor(editor)
      }
    }

    fun closeOtherTabs() {
      val keep = activeEditor ?: return
      openEditors.toList().filter { it !== keep }.forEach { ed ->
        if (ed.isModified) {
          ed.save()
          Logger.info("Saved before close: ${ed.file.name}")
        }
        doCloseEditor(ed)
      }
    }

    fun closeAllEditors() {
      openEditors.toList().forEach { ed ->
        if (ed.isModified) {
          ed.save()
          Logger.info("Saved before close: ${ed.file.name}")
        }
        doCloseEditor(ed)
      }
    }

    fun openProjectFromPath(path: String) {
      closeAllEditors()
      project = application.openProject(path)
      selectedFile = null
      activeEditor = null
      RecentProjects.add(path)
      runConfigManager.clear()
      runConfigManager.ensureDefaults(File(path))
      SymbolIndex.invalidate()
      try { SymbolIndex.ensureIndexed(File(path)) } catch (_: Exception) {}
      IdeStateStore.lastProjectPath = path
      IdeStateStore.save()
      NotificationService.info("Project opened", path)
      Logger.info("Opened project: $path")
    }

    // Record the default project as recent on startup
    RecentProjects.add(project.basePath)
    try { SymbolIndex.ensureIndexed(File(project.basePath)) } catch (_: Exception) {}


    // Jump editor to source when JDI hits a breakpoint / step
    val jumpListener = remember {
      object : JdiListener {
        override fun onBreakpointHit(info: SuspendedThreadInfo) {
          SwingUtilities.invokeLater { jumpToSource(info) }
        }
      }
    }
    androidx.compose.runtime.DisposableEffect(jdiConnector) {
      jdiConnector.addListener(jumpListener)
      onDispose { jdiConnector.removeListener(jumpListener) }
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

    fun persistSession() {
      IdeStateStore.lastProjectPath = project.basePath
      IdeStateStore.openTabs = openEditors.map { it.file.path }
      IdeStateStore.activeTabPath = activeEditor?.file?.path
      IdeStateStore.darkTheme = darkTheme
      IdeStateStore.splitEditor = splitEditor
      IdeStateStore.save()
    }

    fun saveAll() {
      var count = 0
      openEditors.forEach { ed ->
        if (ed.isModified) {
          ed.save()
          count++
        }
      }
      Logger.info("Save All: $count file(s)")
    }

    fun navigateToTarget(target: NavigationTarget) {
      val file = LocalFileSystem.getInstance().findFileByPath(target.filePath) ?: return
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
      pendingGoToLine = editor to target.line
    }

    fun gotoDeclaration() {
      val ed = activeEditor ?: return
      val text = ed.document.getText()
      val symbol = GotoDeclaration.symbolAt(text, ed.caretOffset)
      if (symbol == null) {
        Logger.info("Go to Declaration: no symbol under caret")
        return
      }
      val targets = GotoDeclaration.findDeclarations(File(project.basePath), symbol)
      when {
        targets.isEmpty() -> Logger.info("No declaration found for '$symbol'")
        targets.size == 1 -> navigateToTarget(targets.first())
        else -> {
          gotoDialogTitle = "Go to Declaration"
          gotoTargets = symbol to targets
        }
      }
    }

    fun findUsages() {
      val ed = activeEditor ?: return
      val text = ed.document.getText()
      val symbol = GotoDeclaration.symbolAt(text, ed.caretOffset) ?: return
      val targets = GotoDeclaration.findUsages(File(project.basePath), symbol)
      if (targets.isEmpty()) {
        Logger.info("No usages for '$symbol'")
        return
      }
      gotoDialogTitle = "Find Usages"
      gotoTargets = symbol to targets
    }

    fun startRename() {
      val ed = activeEditor ?: return
      val symbol = GotoDeclaration.symbolAt(ed.document.getText(), ed.caretOffset)
      if (symbol == null) {
        Logger.info("Rename: no symbol under caret")
        return
      }
      renameSymbol = symbol
    }

    fun reloadOpenEditorsFromDisk() {
      openEditors.forEach { ed ->
        try {
          ed.reloadFromDisk()
        } catch (e: Exception) {
          Logger.warn("Reload failed for ${ed.file.path}: ${e.message}")
        }
      }
    }

    Window(
      onCloseRequest = {
        persistSession()
        application.exit()
        exitApplication()
      },
      title = "NexIDE — ${project.name}",
      state = windowState,
      onKeyEvent = { event ->
        if (event.type != KeyEventType.KeyDown) return@Window false
        val ctrlOrMeta = event.isCtrlPressed || event.isMetaPressed
        val shift = event.isShiftPressed
        when {
          // Ctrl+Shift+O — Open Project (also Ctrl+O)
          ctrlOrMeta && event.key == Key.O -> {
            chooseAndOpenProject()
            true
          }
          // Ctrl+Shift+S — Save All
          ctrlOrMeta && shift && event.key == Key.S -> {
            saveAll()
            true
          }
          // Ctrl+S — Save
          ctrlOrMeta && event.key == Key.S -> {
            saveActive()
            true
          }
          // Ctrl+W — Close tab
          ctrlOrMeta && event.key == Key.W && !shift -> {
            activeEditor?.let { requestCloseEditor(it) }
            true
          }
          // Ctrl+Shift+W — Close all tabs
          ctrlOrMeta && shift && event.key == Key.W -> {
            closeAllEditors()
            true
          }
          // Ctrl+Shift+F — Find in Files
          ctrlOrMeta && shift && event.key == Key.F -> {
            findInFilesVisible = true
            true
          }
          // Ctrl+Shift+B / Ctrl+F9 — Build Project
          (ctrlOrMeta && shift && event.key == Key.B) ||
            (ctrlOrMeta && event.key == Key.F9) -> {
            buildProject()
            true
          }
          // Ctrl+Alt+S — Settings
          ctrlOrMeta && event.isAltPressed && event.key == Key.S -> {
            settingsVisible = true
            true
          }
          // Ctrl+Shift+F12 — Problems
          ctrlOrMeta && shift && event.key == Key.F12 -> {
            problemsVisible = true
            true
          }
          // F8 — Toggle breakpoint on line 1 fallback (gutter is primary)
          event.key == Key.F8 && activeEditor != null -> {
            val ed = activeEditor!!
            BreakpointManager.getInstance().toggle(ed.file.path, 1)
            breakpointsVisible = true
            true
          }
          // Shift+F10 — Run selected configuration
          shift && event.key == Key.F10 && runConfigManager.selected != null -> {
            runConfigManager.selected?.let { runConfiguration(it, debug = false) }
            true
          }
          // Shift+F9 — Debug selected configuration
          shift && event.key == Key.F9 && runConfigManager.selected != null -> {
            runConfigManager.selected?.let { runConfiguration(it, debug = true) }
            true
          }
          // Ctrl+B — Go to Declaration
          ctrlOrMeta && event.key == Key.B && !shift -> {
            gotoDeclaration()
            true
          }
          // Alt+F7 — Find Usages
          event.isAltPressed && event.key == Key.F7 -> {
            findUsages()
            true
          }
          // Shift+F6 — Rename
          event.isShiftPressed && event.key == Key.F6 -> {
            startRename()
            true
          }
          else -> false
        }
      }
    ) {
      IdeMenuBar(
        hasEditor = activeEditor != null,
        hasModified = openEditors.any { it.isModified },
        tabCount = openEditors.size,
        selectedRunName = runConfigManager.selected?.name,
        softWrap = com.nexide.platform.core.settings.NexIdeSettings.softWrap,
        darkTheme = darkTheme,
        splitEditor = splitEditor,
        currentProjectPath = project.basePath,
        actions = IdeMenuActions(
          openProject = { chooseAndOpenProject() },
          openRecent = { openProjectFromPath(it) },
          clearRecent = {
            RecentProjects.list().forEach { RecentProjects.remove(it) }
          },
          save = { saveActive() },
          saveAll = { saveAll() },
          closeTab = { activeEditor?.let { requestCloseEditor(it) } },
          closeOtherTabs = { closeOtherTabs() },
          closeAllTabs = { closeAllEditors() },
          openSettings = { settingsVisible = true },
          exit = {
            persistSession()
            application.exit()
            exitApplication()
          },
          gotoDeclaration = { gotoDeclaration() },
          findUsages = { findUsages() },
          reformat = {
            val ed = activeEditor
            if (ed != null) {
              val indent = com.nexide.platform.core.settings.NexIdeSettings.tabSize
              val formatted = com.nexide.platform.editor.format.CodeFormatter.format(
                ed.document.getText(), indent
              )
              if (formatted != ed.document.getText()) {
                ed.replaceText(formatted)
              }
            }
          },
          restoreLocalHistory = {
            val ed = activeEditor
            if (ed != null) {
              val content = com.nexide.platform.core.history.LocalHistory.contentAt(ed.file.path, 0)
              if (content != null) {
                ed.replaceText(content)
                NotificationService.info("Local History", "Restored last snapshot")
              } else {
                NotificationService.warn("Local History", "No snapshots for this file")
              }
            }
          },
          rename = { startRename() },
          build = { buildProject() },
          showProblems = { problemsVisible = true },
          runSelected = {
            runConfigManager.selected?.let { runConfiguration(it, debug = false) }
          },
          debugSelected = {
            runConfigManager.selected?.let { runConfiguration(it, debug = true) }
          },
          editRunConfigs = { runConfigDialogVisible = true },
          stop = {
            processRunner.stop()
            debugSessionManager.stop()
            jdiConnector.detach()
          },
          showBreakpoints = { breakpointsVisible = true },
          showDebug = { debugPanelVisible = true },
          findInFiles = { findInFilesVisible = true },
          showStructure = { structureVisible = true },
          showGit = { gitPanelVisible = true },
          showTerminal = { terminalVisible = true },
          showRun = { runPanelVisible = true },
          toggleSoftWrap = {
            com.nexide.platform.core.settings.NexIdeSettings.softWrap =
              !com.nexide.platform.core.settings.NexIdeSettings.softWrap
            com.nexide.platform.core.settings.NexIdeSettings.save()
            settingsRevision++
          },
          toggleDarkTheme = {
            darkTheme = !darkTheme
            IdeStateStore.darkTheme = darkTheme
            IdeStateStore.save()
          },
          toggleSplit = {
            splitEditor = !splitEditor
            IdeStateStore.splitEditor = splitEditor
            IdeStateStore.save()
          },
          openPlugins = { marketplaceVisible = true }
        )
      )

      if (marketplaceVisible) {
        MarketplaceDialog(onDismiss = { marketplaceVisible = false })
      }
      if (settingsVisible) {
        SettingsDialog(
          onDismiss = { settingsVisible = false },
          onApplied = { settingsRevision++ }
        )
      }
      if (runConfigDialogVisible) {
        RunConfigDialog(
          projectPath = project.basePath,
          manager = runConfigManager,
          onDismiss = { runConfigDialogVisible = false },
          onRun = { cfg ->
            runConfigDialogVisible = false
            runConfiguration(cfg, debug = false)
          },
          onDebug = { cfg ->
            runConfigDialogVisible = false
            runConfiguration(cfg, debug = true)
          }
        )
      }
      gotoTargets?.let { (symbol, targets) ->
        GotoTargetsDialog(
          symbol = symbol,
          targets = targets,
          title = gotoDialogTitle,
          onSelect = { navigateToTarget(it) },
          onDismiss = { gotoTargets = null }
        )
      }
      renameSymbol?.let { symbol ->
        RenameDialog(
          projectRoot = File(project.basePath),
          oldName = symbol,
          onApplied = { files, newName ->
            Logger.info("Renamed '$symbol' → '$newName' in $files file(s)")
            reloadOpenEditorsFromDisk()
          },
          onDismiss = { renameSymbol = null }
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
        problemsVisible = problemsVisible,
        onProblemsVisibleChange = { problemsVisible = it },
        problems = problems,
        building = building,
        onRebuild = { buildProject() },
        onNavigateProblem = { problem ->
          val path = problem.filePath
          if (path != null) {
            val file = LocalFileSystem.getInstance().findFileByPath(path)
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
              pendingGoToLine = editor to (problem.line ?: 1)
            }
          }
        },
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
        darkTheme = darkTheme,
        splitEditor = splitEditor,
        onRequestClose = { editor -> requestCloseEditor(editor) },
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
  problemsVisible: Boolean = false,
  onProblemsVisibleChange: (Boolean) -> Unit = {},
  problems: List<Problem> = emptyList(),
  building: Boolean = false,
  onRebuild: () -> Unit = {},
  onNavigateProblem: (Problem) -> Unit = {},
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
  onCloseConfirm: (Editor, CloseConfirmResult) -> Unit,
  darkTheme: Boolean = true,
  splitEditor: Boolean = false
) {
  val colors = if (darkTheme) darkColorScheme() else lightColorScheme()
  MaterialTheme(colorScheme = colors) {
    Box(Modifier.fillMaxSize()) {
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
            if (editor != null && splitEditor && openEditors.size > 1) {
              val secondary = openEditors.firstOrNull { it !== editor } ?: editor
              Row(Modifier.weight(1f).fillMaxSize()) {
                EditorView(
                  editor = editor,
                  modifier = Modifier.weight(1f).fillMaxSize(),
                  projectRootPath = project.basePath,
                  settingsRevision = settingsRevision,
                  problemLines = problems.filter { it.filePath == editor.file.path }.mapNotNull { it.line }.toSet()
                )
                VerticalDivider()
                EditorView(
                  editor = secondary,
                  modifier = Modifier.weight(1f).fillMaxSize(),
                  projectRootPath = project.basePath,
                  settingsRevision = settingsRevision,
                  problemLines = problems.filter { it.filePath == secondary.file.path }.mapNotNull { it.line }.toSet()
                )
              }
            } else if (editor != null) {
              val gotoLine = pendingGoToLine?.takeIf { it.first === editor }?.second
              EditorView(
                editor = editor,
                modifier = Modifier.weight(1f).fillMaxSize(),
                gotoLineRequest = gotoLine,
                onGotoLineHandled = onPendingGoToLineHandled,
                projectRootPath = project.basePath,
                settingsRevision = settingsRevision,
                problemLines = problems
                  .filter { it.filePath == editor.file.path && it.line != null }
                  .mapNotNull { it.line }
                  .toSet()
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
        if (problemsVisible) {
          HorizontalDivider()
          ProblemsToolWindow(
            problems = problems,
            building = building,
            visible = true,
            onClose = { onProblemsVisibleChange(false) },
            onNavigate = onNavigateProblem,
            onRebuild = onRebuild,
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
    // Toasts
    Box(Modifier.fillMaxSize()) {
      NotificationHost(modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd))
    }
    }
  }
}
