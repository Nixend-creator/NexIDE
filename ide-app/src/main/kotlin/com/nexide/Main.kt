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
import com.nexide.platform.core.gradle.GradleProjectModel
import com.nexide.platform.core.gradle.GradleProjectModelLoader
import com.nexide.platform.core.gradle.GradleTaskNames
import com.nexide.platform.core.run.GradleClasspathResolver
import com.nexide.platform.ui.history.LocalHistoryDialog
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
import com.nexide.platform.ui.refactor.IntentionDialog
import com.nexide.platform.ui.refactor.RenameDialog
import com.nexide.platform.core.refactor.IntentionAction
import com.nexide.platform.core.refactor.IntentionSupport
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
    var gradleModel by mutableStateOf(GradleProjectModelLoader.load(File(defaultPath)))
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
    var intentionActions by mutableStateOf<List<IntentionAction>?>(null)
    var intentionEditor by mutableStateOf<com.nexide.platform.editor.Editor?>(null)
    var localHistoryVisible by mutableStateOf(false)
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
      IdeNavigationService.jumpToSource(info, project.basePath) { vf, line -> openFile(vf, line) }
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

    fun buildProject(gradleTask: String = "build") {
      session.buildProject(
        projectPath = project.basePath,
        onBuilding = { building = it },
        onProblems = { buildProblems = it },
        onShowProblems = { problemsVisible = true },
        gradleTask = gradleTask,
        onBuildFinished = {
          gradleModel = GradleProjectModelLoader.load(File(project.basePath))
        }
      )
    }

    fun moduleGradleTask(suffix: String): String {
      val file = activeEditor?.let { File(it.file.path) }
      return GradleTaskNames.forModule(gradleModel, file, suffix)
    }

    fun buildModule() {
      val task = moduleGradleTask("build")
      NotificationService.info("Build", task)
      buildProject(task)
    }


    fun runTests(gradleTask: String = "test") {
      session.runTests(
        projectPath = project.basePath,
        onBuilding = { building = it },
        onProblems = { buildProblems = it },
        onShowProblems = { problemsVisible = true },
        onShowRun = { runPanelVisible = true },
        gradleTask = gradleTask
      )
    }

    fun runModuleTests() {
      val task = moduleGradleTask("test")
      NotificationService.info("Tests", task)
      runTests(task)
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

    fun openFile(file: VirtualFile, line: Int? = null): Editor =
      IdeTabService.openFile(
        openEditors = openEditors,
        file = file,
        line = line,
        setActive = { activeEditor = it },
        setSelectedFile = { selectedFile = it },
        setPendingGoToLine = { pendingGoToLine = it }
      )

    fun doCloseEditor(editor: Editor) {
      IdeTabService.doCloseEditor(
        openEditors = openEditors,
        editor = editor,
        activeEditor = activeEditor,
        setActive = { activeEditor = it },
        setSelectedFile = { selectedFile = it }
      )
    }

    fun requestCloseEditor(editor: Editor) {
      IdeTabService.requestCloseEditor(
        editor = editor,
        setPendingClose = { pendingCloseEditor = it },
        doClose = { doCloseEditor(it) }
      )
    }

    fun closeOtherTabs() {
      IdeTabService.closeOtherTabs(openEditors, activeEditor) { doCloseEditor(it) }
    }

    fun closeAllEditors() {
      IdeTabService.closeAllEditors(openEditors, pinnedPaths) { doCloseEditor(it) }
    }

    fun reloadGradleProject() {
      val dir = File(project.basePath)
      gradleModel = GradleProjectModelLoader.load(dir)
      GradleClasspathResolver.invalidate(dir)
      reindexProject()
      runConfigManager.clear()
      runConfigManager.ensureDefaults(dir)
      val n = gradleModel?.modules?.size ?: 0
      Logger.info("Gradle project reloaded: $n module(s)")
      NotificationService.success("Gradle", "Reloaded — $n module(s)")
    }

    fun openProjectFromPath(path: String) {
      IdeTabService.openProjectFromPath(
        path = path,
        application = application,
        runConfigManager = runConfigManager,
        closeAll = { closeAllEditors() },
        setProject = { project = it },
        clearEditors = {
          selectedFile = null
          activeEditor = null
        }
      )
      gradleModel = GradleProjectModelLoader.load(File(path))
      Logger.info("Gradle modules: ${gradleModel?.modules?.size ?: 0}")
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
      IdeTabService.chooseAndOpenProject(project.basePath) { openProjectFromPath(it) }
    }



    fun saveActive() {
      val ed = activeEditor
      val ok = ed?.save() == true
      if (ok && ed != null) {
        reindexSavedFiles(listOf(File(ed.file.path)))
        diagnosticsTick++
      }
    }

    fun toggleBookmark() {
      IdeNavigationService.toggleBookmark(activeEditor) { bookmarksTick++ }
    }

    fun goToNextBookmark() {
      IdeNavigationService.goToNextBookmark(activeEditor) { vf, line -> openFile(vf, line) }
    }

    fun copyActivePath(kind: String) {
      val file = activeEditor?.file ?: selectedFile ?: return
      val text = IdeNavigationService.pathText(kind, file.path, file.name, project.basePath)
      IdeNavigationService.copyToClipboard(text, text)
    }

    fun revealInProjectView() {
      val file = activeEditor?.file ?: return
      selectedFile = file
      projectExpandPaths = IdeNavigationService.ancestorPaths(file.path, project.basePath)
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
      val multi = IdeNavigationService.gotoDeclaration(
        activeEditor,
        project.basePath
      ) { navigateToTarget(it) }
      if (multi != null) {
        gotoDialogTitle = "Go to Declaration"
        gotoTargets = multi
      }
    }

    fun findUsages() {
      val multi = IdeNavigationService.findUsages(activeEditor, project.basePath) ?: return
      gotoDialogTitle = "Find Usages"
      gotoTargets = multi
    }

    fun goToProblem(forward: Boolean) {
      val ed = activeEditor
      val line = if (ed != null) {
        IdeNavigationService.lineNumberAt(ed.document.getText(), ed.caretOffset)
      } else 0
      val p = IdeNavigationService.stepProblem(
        mergedProblems(),
        ed?.file?.path,
        line,
        forward
      ) ?: run {
        NotificationService.info("Problems", "No problems")
        return
      }
      val path = p.filePath ?: return
      val vf = LocalFileSystem.getInstance().findFileByPath(path) ?: return
      openFile(vf, p.line)
      problemsVisible = true
      NotificationService.info(
        "Problem",
        "${p.severity}: ${p.message.take(80)}"
      )
    }

    fun showIntention() {
      val ed = activeEditor ?: return
      val text = ed.document.getText()
      val line = IdeNavigationService.lineNumberAt(text, ed.caretOffset)
      val fileProblems = mergedProblems().filter { it.filePath == ed.file.path }
      val actions = IntentionSupport.actionsForLine(text, line, fileProblems)
      if (actions.isEmpty()) {
        NotificationService.info("Intentions", "No quick-fix on this line")
        return
      }
      if (actions.size == 1) {
        val next = actions[0].apply(text)
        if (next != text) {
          ed.replaceText(next)
          diagnosticsTick++
          NotificationService.info("Intention", actions[0].title)
        }
        return
      }
      intentionEditor = ed
      intentionActions = actions
    }

    fun snapshotBeforeEdit(ed: com.nexide.platform.editor.Editor, label: String) {
      try {
        LocalHistory.record(ed.file.path, ed.document.getText(), label)
      } catch (_: Exception) {
      }
    }

    fun applyIntentionAt(index: Int) {
      val ed = intentionEditor ?: activeEditor
      val actions = intentionActions
      intentionActions = null
      intentionEditor = null
      if (ed == null || actions == null || index !in actions.indices) return
      val text = ed.document.getText()
      val next = actions[index].apply(text)
      if (next != text) {
        snapshotBeforeEdit(ed, "before intention")
        ed.replaceText(next)
        diagnosticsTick++
        NotificationService.info("Intention", actions[index].title)
      }
    }

    fun optimizeImportsActive() {
      val ed = activeEditor ?: return
      val result = com.nexide.platform.core.refactor.ImportOptimizer.optimize(ed.document.getText())
      if (result.text != ed.document.getText()) {
        snapshotBeforeEdit(ed, "before optimize imports")
        ed.replaceText(result.text)
        com.nexide.platform.core.notify.NotificationService.info(
          "Optimize Imports",
          if (result.removed > 0) "Removed ${result.removed} import(s)" else "Sorted imports"
        )
      } else {
        com.nexide.platform.core.notify.NotificationService.info("Optimize Imports", "Nothing to change")
      }
    }

    fun startRename() {
      renameSymbol = IdeNavigationService.symbolForRename(activeEditor)
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
              val paletteActions = if (commandPaletteVisible) {
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
          optimizeImports = { optimizeImportsActive() },
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
          buildModule = { buildModule() },
          runTests = { runTests() },
          runModuleTests = { runModuleTests() },
          buildModule = { buildModule() },
          reloadGradle = { reloadGradleProject() },
          showProblems = { problemsVisible = true },
          runSelected = { runConfigManager.selected?.let { runConfiguration(it, false) } },
          debugSelected = { runConfigManager.selected?.let { runConfiguration(it, true) } },
          stop = { session.stopAll() },
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
          showBookmarks = { bookmarksVisible = true },
          showLocalHistory = { localHistoryVisible = activeEditor != null },
          nextProblem = { goToProblem(true) },
          prevProblem = { goToProblem(false) },
          reloadGradle = { reloadGradleProject() }
        )
        IdeActionCatalog.paletteActions(actionState, handlers)
      } else {
        emptyList()
      }

      IdeDialogHost(
        projectPath = project.basePath,
        activeEditor = activeEditor,
        openEditors = openEditors,
        recentFilesVisible = recentFilesVisible,
        onRecentFilesVisible = { recentFilesVisible = it },
        commandPaletteVisible = commandPaletteVisible,
        onCommandPaletteVisible = { commandPaletteVisible = it },
        paletteActions = paletteActions,
        bookmarksVisible = bookmarksVisible,
        bookmarksTick = bookmarksTick,
        onBookmarksVisible = { bookmarksVisible = it },
        onBookmarksTick = { bookmarksTick++ },
        searchEverywhereVisible = searchEverywhereVisible,
        onSearchEverywhereVisible = { searchEverywhereVisible = it },
        marketplaceVisible = marketplaceVisible,
        onMarketplaceVisible = { marketplaceVisible = it },
        settingsVisible = settingsVisible,
        onSettingsVisible = { settingsVisible = it },
        onSettingsApplied = { settingsRevision++ },
        runConfigDialogVisible = runConfigDialogVisible,
        onRunConfigDialogVisible = { runConfigDialogVisible = it },
        runConfigManager = runConfigManager,
        onRunConfig = { cfg, debug -> runConfiguration(cfg, debug) },
        gotoTargets = gotoTargets,
        gotoDialogTitle = gotoDialogTitle,
        onGotoDismiss = { gotoTargets = null },
        onNavigateTarget = { navigateToTarget(it) },
        localHistoryVisible = localHistoryVisible,
        onLocalHistoryVisible = { localHistoryVisible = it },
        onSnapshotBeforeEdit = { ed, label -> snapshotBeforeEdit(ed, label) },
        intentionActions = intentionActions,
        onIntentionSelect = { applyIntentionAt(it) },
        onIntentionDismiss = { intentionActions = null; intentionEditor = null },
        renameSymbol = renameSymbol,
        onRenameDismiss = { renameSymbol = null },
        onRenameApplied = { files, newName ->
          val symbol = renameSymbol
          NotificationService.success(
            "Rename",
            "'$symbol' → '$newName' ($files file(s))"
          )
          reloadOpenEditorsFromDisk()
          reindexProject()
          diagnosticsTick++
        },
        onOpenFile = { path, line ->
          val vf = LocalFileSystem.getInstance().findFileByPath(path)
          if (vf != null && !vf.isDirectory) openFile(vf, line)
        }
      )

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
          pinnedPaths = pinnedPaths,
          moduleForPath = { path -> gradleModel?.moduleForFile(File(path))?.path },
          gradleModules = gradleModel?.modules?.map { it.path } ?: emptyList()
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
          onRevealModule = { absPath ->
            val ancestors = IdeNavigationService.ancestorPaths(absPath, project.basePath)
            projectExpandPaths = ancestors + absPath
            NotificationService.info("Module", File(absPath).name)
          },
          onBuildModulePath = { modPath ->
            val task = com.nexide.platform.core.gradle.GradleTaskNames.forModulePath(modPath, "build")
            NotificationService.info("Build", task)
            buildProject(task)
          },
          onTestModulePath = { modPath ->
            val task = com.nexide.platform.core.gradle.GradleTaskNames.forModulePath(modPath, "test")
            NotificationService.info("Tests", task)
            runTests(task)
          },
          onOpenModuleBuildFile = { modPath ->
            val rel = if (modPath == ":") "." else modPath.trimStart(':').replace(':', '/')
            val dir = File(project.basePath, rel)
            val buildFile = listOf("build.gradle.kts", "build.gradle")
              .map { File(dir, it) }
              .firstOrNull { it.isFile }
            if (buildFile != null) {
              val vf = LocalFileSystem.getInstance().findFileByPath(buildFile.absolutePath)
              if (vf != null) openFile(vf)
              else NotificationService.warn("Module", "Cannot open ${buildFile.name}")
            } else {
              NotificationService.warn("Module", "No build.gradle in $modPath")
            }
          },
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
            moduleForPath = state.moduleForPath,
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
              },
              modules = state.gradleModules,
              currentModule = editor?.file?.path?.let { state.moduleForPath(it) },
              onModuleClick = { modPath ->
                val rel = modPath.trimStart(':').replace(':', '/')
                val abs = File(project.basePath, rel).absolutePath
                callbacks.onRevealModule(abs)
              },
              onBuildModule = { modPath -> callbacks.onBuildModulePath(modPath) },
              onTestModule = { modPath -> callbacks.onTestModulePath(modPath) },
              onOpenModuleBuildFile = { modPath -> callbacks.onOpenModuleBuildFile(modPath) }
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
