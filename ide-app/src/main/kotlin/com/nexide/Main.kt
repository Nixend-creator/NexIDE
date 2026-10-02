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
import com.nexide.platform.core.Bookmarks
import com.nexide.platform.core.RecentFiles
import com.nexide.platform.core.RecentProjects
import com.nexide.platform.core.build.DiagnosticsMerge
import com.nexide.platform.core.build.LiveSyntaxChecker
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
import com.nexide.platform.plugin.PluginManager
import com.nexide.platform.plugin.example.CoreFileTypesPlugin
import com.nexide.platform.plugin.example.FILE_TYPE_EP
import com.nexide.platform.ui.dialog.CloseConfirmResult
import com.nexide.platform.ui.dialog.ConfirmCloseDialog
import com.nexide.platform.plugin.filetype.FileTypeRegistry
import com.nexide.platform.ui.projectview.ProjectView
import com.nexide.platform.ui.navigation.GotoTargetsDialog
import com.nexide.platform.ui.refactor.RenameDialog
import com.nexide.platform.ui.run.RunConfigDialog
import com.nexide.platform.ui.search.SearchHit
import com.nexide.platform.ui.structure.StructureToolWindow
import com.nexide.platform.core.debug.BreakpointManager
import com.nexide.platform.core.debug.DebugSessionManager
import com.nexide.platform.core.debug.JdiConnector
import com.nexide.platform.core.debug.JdiListener
import com.nexide.platform.core.debug.SuspendedThreadInfo
import javax.swing.SwingUtilities
import com.nexide.platform.ui.settings.SettingsDialog
import com.nexide.platform.ui.notify.NotificationHost
import com.nexide.platform.ui.navigation.BookmarksDialog
import com.nexide.platform.ui.navigation.CommandPalette
import com.nexide.platform.ui.navigation.RecentFilesDialog
import com.nexide.platform.ui.navigation.SearchEverywhereDialog
import com.nexide.platform.ui.plugins.MarketplaceDialog
import com.nexide.platform.util.Logger
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
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
    var recentFilesVisible by mutableStateOf(false)
    var commandPaletteVisible by mutableStateOf(false)
    var searchEverywhereVisible by mutableStateOf(false)
    var projectExpandPaths by mutableStateOf<Set<String>>(emptySet())
    var bookmarksVisible by mutableStateOf(false)
    var pinnedPaths by mutableStateOf<Set<String>>(emptySet())
    var bookmarksTick by mutableStateOf(0)

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
    var buildProblems by mutableStateOf<List<Problem>>(emptyList())
    var liveProblems by mutableStateOf<List<Problem>>(emptyList())
    var diagnosticsTick by mutableStateOf(0)
    var building by mutableStateOf(false)
    var gotoTargets by mutableStateOf<Pair<String, List<NavigationTarget>>?>(null)
    var gotoDialogTitle by mutableStateOf("Go to Declaration")
    var renameSymbol by mutableStateOf<String?>(null)
    // When opening a search hit, request go-to-line after editor opens
    var pendingGoToLine by mutableStateOf<Pair<Editor, Int>?>(null)

    val runConfigManager = remember { RunConfigurationManager.getInstance() }
    val session = IdeSessionServices(processRunner, debugSessionManager, jdiConnector, runConfigManager)

    fun mergedProblems(): List<Problem> =
      DiagnosticsMerge.merge(buildProblems, liveProblems)
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





    fun jumpToSource(info: SuspendedThreadInfo) {
      val name = info.sourceName ?: return
      val line = info.lineNumber ?: return
      val root = File(project.basePath)
      val match = root.walkTopDown()
        .onEnter { d -> d.name !in setOf(".git", ".gradle", "build", "out", "node_modules") }
        .firstOrNull { it.isFile && it.name == name }
        ?: return
      val file = LocalFileSystem.getInstance().findFileByPath(match.absolutePath) ?: return
      openFile(file, line)
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


    /**
     * Opens [file] in a tab (or focuses existing) and optionally jumps to [line] (1-based).
     */

        fun reindexProject() {
      session.reindexProjectAsync(project.basePath)
    }

    fun reindexSavedFiles(files: List<File>) {
      session.reindexFilesAsync(files)
    }

    fun buildProject() {
      session.buildProject(
        projectPath = project.basePath,
        onBuilding = { building = it },
        onProblems = { buildProblems = it },
        onShowProblems = { problemsVisible = true }
      )
    }

    fun runTests() {
      session.runTests(
        projectPath = project.basePath,
        onBuilding = { building = it },
        onProblems = { buildProblems = it },
        onShowProblems = { problemsVisible = true },
        onShowRun = { runPanelVisible = true }
      )
    }

    fun runConfiguration(config: RunConfiguration, debug: Boolean = false) {
      session.runConfiguration(
        config = config,
        projectPath = project.basePath,
        debug = debug,
        onShowRun = { runPanelVisible = true },
        onShowDebug = { debugPanelVisible = true },
        autoAttach = { autoAttachAndInstallBps() }
      )
    }

    fun openFile(file: VirtualFile, line: Int? = null): Editor {
      RecentFiles.add(file.path)
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
      if (line != null && line > 0) {
        pendingGoToLine = editor to line
      }
      return editor
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
        if (ed.file.path in pinnedPaths) return@forEach
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
      val ed = activeEditor
      val ok = ed?.save() == true
      if (ok && ed != null) {
        reindexSavedFiles(listOf(File(ed.file.path)))
        diagnosticsTick++
      }
    }

    fun lineNumberAt(text: String, offset: Int): Int {
      val o = offset.coerceIn(0, text.length)
      return text.substring(0, o).count { it == '\n' } + 1
    }

    fun toggleBookmark() {
      val ed = activeEditor ?: return
      val line = lineNumberAt(ed.document.getText(), ed.caretOffset)
      val added = Bookmarks.toggle(ed.file.path, line)
      bookmarksTick++
      NotificationService.info(
        "Bookmark",
        if (added) "Added ${ed.file.name}:$line" else "Removed ${ed.file.name}:$line"
      )
    }

    fun goToNextBookmark() {
      val ed = activeEditor
      val line = if (ed != null) lineNumberAt(ed.document.getText(), ed.caretOffset) else 0
      val next = Bookmarks.next(ed?.file?.path, line) ?: run {
        NotificationService.warn("Bookmarks", "None")
        return
      }
      val vf = LocalFileSystem.getInstance().findFileByPath(next.filePath)
      if (vf != null) openFile(vf, next.line)
    }

    fun copyToClipboard(text: String, label: String) {
      try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        NotificationService.info("Copied", label)
      } catch (e: Exception) {
        NotificationService.warn("Copy failed", e.message ?: "")
      }
    }

    fun copyActivePath(kind: String) {
      val file = activeEditor?.file ?: selectedFile ?: return
      val abs = file.path
      val text = when (kind) {
        "name" -> file.name
        "relative" -> {
          val root = project.basePath
          if (abs.startsWith(root)) abs.removePrefix(root).trimStart('/', '\\') else abs
        }
        else -> abs
      }
      copyToClipboard(text, text)
    }

    fun revealInProjectView() {
      val file = activeEditor?.file ?: return
      selectedFile = file
      val root = project.basePath
      val ancestors = mutableSetOf<String>()
      var f: java.io.File? = java.io.File(file.path).parentFile
      val rootFile = java.io.File(root)
      while (f != null) {
        ancestors.add(f.absolutePath)
        if (f.absolutePath == rootFile.absolutePath) break
        f = f.parentFile
      }
      ancestors.add(root)
      projectExpandPaths = ancestors
      NotificationService.info("Project View", file.name)
    }

    fun zoomEditor(delta: Int) {
      val next = (com.nexide.platform.core.settings.NexIdeSettings.editorFontSize + delta).coerceIn(10, 32)
      if (next != com.nexide.platform.core.settings.NexIdeSettings.editorFontSize) {
        com.nexide.platform.core.settings.NexIdeSettings.editorFontSize = next
        com.nexide.platform.core.settings.NexIdeSettings.save()
        settingsRevision++
        NotificationService.info("Font size", "${next}sp")
      }
    }

    fun resetEditorZoom() {
      com.nexide.platform.core.settings.NexIdeSettings.editorFontSize = 14
      com.nexide.platform.core.settings.NexIdeSettings.save()
      settingsRevision++
      NotificationService.info("Font size", "14sp")
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
      val saved = mutableListOf<File>()
      openEditors.forEach { ed ->
        if (ed.isModified) {
          if (ed.save()) saved.add(File(ed.file.path))
        }
      }
      Logger.info("Save All: ${saved.size} file(s)")
      reindexSavedFiles(saved)
    }

    fun navigateToTarget(target: NavigationTarget) {
      val file = LocalFileSystem.getInstance().findFileByPath(target.filePath) ?: return
      openFile(file, target.line)
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


    // Debounced live diagnostics (300ms after edits settle)
    val editorRevs = openEditors.sumOf { it.contentRevision } + openEditors.size
    LaunchedEffect(editorRevs, diagnosticsTick) {
      delay(300)
      liveProblems = DiagnosticsMerge.liveForFiles(
        openEditors.map { it.file.path to it.document.getText() }
      )
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
        IdeKeyMap.handle(
          event = event,
          escape = IdeEscapeStack(
            dismissCommandPalette = {
              if (commandPaletteVisible) { commandPaletteVisible = false; true } else false
            },
            dismissSearchEverywhere = {
              if (searchEverywhereVisible) { searchEverywhereVisible = false; true } else false
            },
            dismissRecentFiles = {
              if (recentFilesVisible) { recentFilesVisible = false; true } else false
            },
            dismissBookmarks = {
              if (bookmarksVisible) { bookmarksVisible = false; true } else false
            },
            dismissMarketplace = {
              if (marketplaceVisible) { marketplaceVisible = false; true } else false
            },
            dismissSettings = {
              if (settingsVisible) { settingsVisible = false; true } else false
            },
            dismissRunConfig = {
              if (runConfigDialogVisible) { runConfigDialogVisible = false; true } else false
            },
            dismissGoto = {
              if (gotoTargets != null) { gotoTargets = null; true } else false
            },
            dismissRename = {
              if (renameSymbol != null) { renameSymbol = null; true } else false
            },
            dismissFindInFiles = {
              if (findInFilesVisible) { findInFilesVisible = false; true } else false
            },
            dismissProblems = {
              if (problemsVisible) { problemsVisible = false; true } else false
            },
            dismissBreakpoints = {
              if (breakpointsVisible) { breakpointsVisible = false; true } else false
            },
            dismissDebug = {
              if (debugPanelVisible) { debugPanelVisible = false; true } else false
            },
            dismissRun = {
              if (runPanelVisible) { runPanelVisible = false; true } else false
            },
            dismissGit = {
              if (gitPanelVisible) { gitPanelVisible = false; true } else false
            },
            dismissTerminal = {
              if (terminalVisible) { terminalVisible = false; true } else false
            },
            dismissStructure = {
              if (structureVisible) { structureVisible = false; true } else false
            }
          ),
          hasEditor = activeEditor != null,
          hasRunConfig = runConfigManager.selected != null,
          openProject = { chooseAndOpenProject() },
          recentFiles = { recentFilesVisible = true },
          findAction = { commandPaletteVisible = true },
          searchEverywhere = { searchEverywhereVisible = true },
          zoomIn = { zoomEditor(1) },
          zoomOut = { zoomEditor(-1) },
          zoomReset = { resetEditorZoom() },
          toggleBookmark = { toggleBookmark() },
          nextBookmark = { goToNextBookmark() },
          showBookmarks = { bookmarksVisible = true },
          save = { saveActive() },
          saveAll = { saveAll() },
          closeTab = { activeEditor?.let { requestCloseEditor(it) } },
          closeAll = { closeAllEditors() },
          findInFiles = { findInFilesVisible = true },
          build = { buildProject() },
          settings = { settingsVisible = true },
          problems = { problemsVisible = true },
          toggleBreakpoint = {
            activeEditor?.let { ed ->
              BreakpointManager.getInstance().toggle(ed.file.path, 1)
              breakpointsVisible = true
            }
          },
          run = { runConfigManager.selected?.let { runConfiguration(it, debug = false) } },
          debug = { runConfigManager.selected?.let { runConfiguration(it, debug = true) } },
          gotoDeclaration = { gotoDeclaration() },
          findUsages = { findUsages() },
          rename = { startRename() }
        )
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
          recentFiles = { recentFilesVisible = true },
          findAction = { commandPaletteVisible = true },
          searchEverywhere = { searchEverywhereVisible = true },
          copyAbsolutePath = { copyActivePath("absolute") },
          copyRelativePath = { copyActivePath("relative") },
          copyFileName = { copyActivePath("name") },
          revealInProject = { revealInProjectView() },
          toggleBookmark = { toggleBookmark() },
          nextBookmark = { goToNextBookmark() },
          showBookmarks = { bookmarksVisible = true },
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
          runTests = { runTests() },
          showProblems = { problemsVisible = true },
          runSelected = {
            runConfigManager.selected?.let { runConfiguration(it, debug = false) }
          },
          debugSelected = {
            runConfigManager.selected?.let { runConfiguration(it, debug = true) }
          },
          editRunConfigs = { runConfigDialogVisible = true },
          stop = {
            session.stopAll()
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
          openPlugins = { marketplaceVisible = true },
          zoomIn = { zoomEditor(1) },
          zoomOut = { zoomEditor(-1) },
          zoomReset = { resetEditorZoom() }
        )
      )

      if (recentFilesVisible) {
        RecentFilesDialog(
          paths = RecentFiles.list(),
          onSelect = { path ->
            recentFilesVisible = false
            val vf = LocalFileSystem.getInstance().findFileByPath(path)
            if (vf != null && !vf.isDirectory) openFile(vf)
          },
          onDismiss = { recentFilesVisible = false }
        )
      }
      if (commandPaletteVisible) {
        val actionState = IdeActionState(
          hasEditor = activeEditor != null,
          hasModified = openEditors.any { it.isModified },
          hasTabs = openEditors.isNotEmpty(),
          hasRunConfig = runConfigManager.selected != null
        )
        val handlers = IdeActionHandlers(
          openProject = { chooseAndOpenProject() },
          save = { saveActive() },
          saveAll = { saveAll() },
          closeTab = { activeEditor?.let { requestCloseEditor(it) } },
          closeAllTabs = { closeAllEditors() },
          recentFiles = { recentFilesVisible = true },
          searchEverywhere = { searchEverywhereVisible = true },
          findAction = { commandPaletteVisible = true },
          findInFiles = { findInFilesVisible = true },
          gotoDeclaration = { gotoDeclaration() },
          findUsages = { findUsages() },
          rename = { startRename() },
          reformat = {
            val ed = activeEditor
            if (ed != null) {
              val indent = com.nexide.platform.core.settings.NexIdeSettings.tabSize
              val formatted = com.nexide.platform.editor.format.CodeFormatter.format(
                ed.document.getText(), indent
              )
              if (formatted != ed.document.getText()) ed.replaceText(formatted)
            }
          },
          build = { buildProject() },
          runTests = { runTests() },
          showProblems = { problemsVisible = true },
          runSelected = { runConfigManager.selected?.let { runConfiguration(it, false) } },
          debugSelected = { runConfigManager.selected?.let { runConfiguration(it, true) } },
          stop = {
            session.stopAll()
          },
          showGit = { gitPanelVisible = true },
          showTerminal = { terminalVisible = true },
          showStructure = { structureVisible = true },
          showDebug = { debugPanelVisible = true },
          showBreakpoints = { breakpointsVisible = true },
          openSettings = { settingsVisible = true },
          openPlugins = { marketplaceVisible = true },
          toggleTheme = {
            darkTheme = !darkTheme
            IdeStateStore.darkTheme = darkTheme
            IdeStateStore.save()
          },
          toggleSplit = {
            splitEditor = !splitEditor
            IdeStateStore.splitEditor = splitEditor
            IdeStateStore.save()
          },
          toggleSoftWrap = {
            com.nexide.platform.core.settings.NexIdeSettings.softWrap =
              !com.nexide.platform.core.settings.NexIdeSettings.softWrap
            com.nexide.platform.core.settings.NexIdeSettings.save()
            settingsRevision++
          },
          zoomIn = { zoomEditor(1) },
          zoomOut = { zoomEditor(-1) },
          zoomReset = { resetEditorZoom() },
          copyAbsolutePath = { copyActivePath("absolute") },
          copyRelativePath = { copyActivePath("relative") },
          copyFileName = { copyActivePath("name") },
          revealInProject = { revealInProjectView() },
          toggleBookmark = { toggleBookmark() },
          nextBookmark = { goToNextBookmark() },
          showBookmarks = { bookmarksVisible = true }
        )
        CommandPalette(
          actions = IdeActionCatalog.paletteActions(actionState, handlers),
          onDismiss = { commandPaletteVisible = false }
        )
      }
      if (bookmarksVisible) {
        @Suppress("UNUSED_EXPRESSION")
        bookmarksTick
        BookmarksDialog(
          bookmarks = Bookmarks.list(),
          onNavigate = { bm ->
            bookmarksVisible = false
            val vf = LocalFileSystem.getInstance().findFileByPath(bm.filePath)
            if (vf != null) openFile(vf, bm.line)
          },
          onRemove = { bm ->
            Bookmarks.remove(bm.filePath, bm.line)
            bookmarksTick++
          },
          onClear = {
            Bookmarks.clear()
            bookmarksTick++
          },
          onDismiss = { bookmarksVisible = false }
        )
      }
      if (searchEverywhereVisible) {
        SearchEverywhereDialog(
          projectRoot = File(project.basePath),
          onOpenFile = { path, line ->
            searchEverywhereVisible = false
            val vf = LocalFileSystem.getInstance().findFileByPath(path)
            if (vf != null && !vf.isDirectory) openFile(vf, line)
          },
          onDismiss = { searchEverywhereVisible = false }
        )
      }
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

      val problemsForUi = mergedProblems()
      NexIdeApp(
        state = IdeUiState(
          project = project,
          openEditors = openEditors,
          activeEditor = activeEditor,
          selectedFile = selectedFile,
          pendingCloseEditor = pendingCloseEditor,
          processRunner = processRunner,
          debugSessionManager = debugSessionManager,
          jdiConnector = jdiConnector,
          problems = problemsForUi,
          building = building,
          settingsRevision = settingsRevision,
          pendingGoToLine = pendingGoToLine,
          darkTheme = darkTheme,
          splitEditor = splitEditor,
          runPanelVisible = runPanelVisible,
          debugPanelVisible = debugPanelVisible,
          problemsVisible = problemsVisible,
          terminalVisible = terminalVisible,
          gitPanelVisible = gitPanelVisible,
          breakpointsVisible = breakpointsVisible,
          structureVisible = structureVisible,
          findInFilesVisible = findInFilesVisible,
          projectExpandPaths = projectExpandPaths,
          pinnedPaths = pinnedPaths
        ),
        callbacks = IdeUiCallbacks(
          onRunPanelVisibleChange = { runPanelVisible = it },
          onDebugPanelVisibleChange = { debugPanelVisible = it },
          onProblemsVisibleChange = { problemsVisible = it },
          onRebuild = { buildProject() },
          onNavigateProblem = { problem ->
            val path = problem.filePath
            if (path != null) {
              val file = LocalFileSystem.getInstance().findFileByPath(path)
              if (file != null) openFile(file, problem.line)
            }
          },
          onTerminalVisibleChange = { terminalVisible = it },
          onGitPanelVisibleChange = { gitPanelVisible = it },
          onBreakpointsVisibleChange = { breakpointsVisible = it },
          onBreakpointNavigate = { filePath, line ->
            val file = LocalFileSystem.getInstance().findFileByPath(filePath)
            if (file != null) openFile(file, line)
          },
          onStructureVisibleChange = { structureVisible = it },
          onFindInFilesVisibleChange = { findInFilesVisible = it },
          onPendingGoToLineHandled = { pendingGoToLine = null },
          onOpenFile = { file -> openFile(file) },
          onOpenSearchHit = { hit -> openFile(hit.file, hit.lineNumber) },
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
          onRequestClose = { editor -> requestCloseEditor(editor) },
          onTogglePin = { ed ->
            pinnedPaths = if (ed.file.path in pinnedPaths) pinnedPaths - ed.file.path
            else pinnedPaths + ed.file.path
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
      )
    }
  }
}

@Composable
fun NexIdeApp(
  state: IdeUiState,
  callbacks: IdeUiCallbacks
) {
  val project = state.project
  val openEditors = state.openEditors
  val activeEditor = state.activeEditor
  val selectedFile = state.selectedFile
  val pendingCloseEditor = state.pendingCloseEditor
  val processRunner = state.processRunner
  val debugSessionManager = state.debugSessionManager
  val jdiConnector = state.jdiConnector
  val problems = state.problems
  val building = state.building
  val settingsRevision = state.settingsRevision
  val pendingGoToLine = state.pendingGoToLine
  val darkTheme = state.darkTheme
  val splitEditor = state.splitEditor
  val runPanelVisible = state.runPanelVisible
  val debugPanelVisible = state.debugPanelVisible
  val problemsVisible = state.problemsVisible
  val terminalVisible = state.terminalVisible
  val gitPanelVisible = state.gitPanelVisible
  val breakpointsVisible = state.breakpointsVisible
  val structureVisible = state.structureVisible
  val findInFilesVisible = state.findInFilesVisible
  val projectExpandPaths = state.projectExpandPaths
  val pinnedPaths = state.pinnedPaths

  val onRunPanelVisibleChange = callbacks.onRunPanelVisibleChange
  val onDebugPanelVisibleChange = callbacks.onDebugPanelVisibleChange
  val onProblemsVisibleChange = callbacks.onProblemsVisibleChange
  val onRebuild = callbacks.onRebuild
  val onNavigateProblem = callbacks.onNavigateProblem
  val onTerminalVisibleChange = callbacks.onTerminalVisibleChange
  val onGitPanelVisibleChange = callbacks.onGitPanelVisibleChange
  val onBreakpointsVisibleChange = callbacks.onBreakpointsVisibleChange
  val onBreakpointNavigate = callbacks.onBreakpointNavigate
  val onStructureVisibleChange = callbacks.onStructureVisibleChange
  val onFindInFilesVisibleChange = callbacks.onFindInFilesVisibleChange
  val onPendingGoToLineHandled = callbacks.onPendingGoToLineHandled
  val onOpenFile = callbacks.onOpenFile
  val onOpenSearchHit = callbacks.onOpenSearchHit
  val onNavigateToLine = callbacks.onNavigateToLine
  val onOpenProject = callbacks.onOpenProject
  val onOpenRecent = callbacks.onOpenRecent
  val onSelectEditor = callbacks.onSelectEditor
  val onRequestClose = callbacks.onRequestClose
  val onCloseConfirm = callbacks.onCloseConfirm
  val onTogglePin = callbacks.onTogglePin
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
            expandPaths = projectExpandPaths,
            modifier = Modifier
              .width(280.dp)
              .fillMaxHeight()
          )

          VerticalDivider()

          IdeEditorArea(
            openEditors = openEditors,
            activeEditor = activeEditor,
            splitEditor = splitEditor,
            projectPath = project.basePath,
            settingsRevision = settingsRevision,
            problems = problemsForUi,
            pendingGoToLine = pendingGoToLine,
            onPendingGoToLineHandled = onPendingGoToLineHandled,
            onSelectEditor = onSelectEditor,
            onRequestClose = onRequestClose,
            onOpenProject = onOpenProject,
            onOpenRecent = onOpenRecent,
            pinnedPaths = pinnedPaths,
            onTogglePin = onTogglePin,
            modifier = Modifier.weight(1f)
          )

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

        IdeBottomPanels(
          projectRoot = project.baseDir,
          projectPath = project.basePath,
          processRunner = processRunner,
          debugSessionManager = debugSessionManager,
          jdiConnector = jdiConnector,
          problems = problemsForUi,
          building = building,
          findInFilesVisible = findInFilesVisible,
          problemsVisible = problemsVisible,
          breakpointsVisible = breakpointsVisible,
          debugPanelVisible = debugPanelVisible,
          runPanelVisible = runPanelVisible,
          gitPanelVisible = gitPanelVisible,
          terminalVisible = terminalVisible,
          onFindInFilesVisibleChange = onFindInFilesVisibleChange,
          onProblemsVisibleChange = onProblemsVisibleChange,
          onBreakpointsVisibleChange = onBreakpointsVisibleChange,
          onDebugPanelVisibleChange = onDebugPanelVisibleChange,
          onRunPanelVisibleChange = onRunPanelVisibleChange,
          onGitPanelVisibleChange = onGitPanelVisibleChange,
          onTerminalVisibleChange = onTerminalVisibleChange,
          onNavigateProblem = onNavigateProblem,
          onRebuild = onRebuild,
          onBreakpointNavigate = onBreakpointNavigate,
          onOpenSearchHit = onOpenSearchHit
        )
      }
    }
    // Toasts
    Box(Modifier.fillMaxSize()) {
      NotificationHost(modifier = Modifier.align(androidx.compose.ui.Alignment.BottomEnd))
    }
    }
  }
}
