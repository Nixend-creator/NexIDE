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

package com.nexide.platform.core.debug

import com.nexide.platform.core.run.GradleRunSupport
import com.nexide.platform.core.run.ProcessOutputListener
import com.nexide.platform.core.run.ProcessRunner
import com.nexide.platform.util.Logger
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

enum class DebugSessionState {
  IDLE,
  STARTING,
  RUNNING,
  STOPPED
}

/**
 * Manages a debug session: launches a process with JDWP agent enabled.
 *
 * Breakpoints are tracked in [BreakpointManager]. Full JDI attach/step comes next.
 */
class DebugSessionManager(
  private val processRunner: ProcessRunner
) {
  private val myState = AtomicReference(DebugSessionState.IDLE)
  private val myListeners = CopyOnWriteArrayList<(DebugSessionState) -> Unit>()

  var debugPort: Int = DEFAULT_PORT
    private set

  val state: DebugSessionState
    get() = myState.get()

  val isActive: Boolean
    get() = state == DebugSessionState.STARTING || state == DebugSessionState.RUNNING

  fun addStateListener(listener: (DebugSessionState) -> Unit) {
    myListeners.add(listener)
  }

  fun removeStateListener(listener: (DebugSessionState) -> Unit) {
    myListeners.remove(listener)
  }

  fun getProcessRunner(): ProcessRunner = processRunner

  /**
   * Starts a Gradle task under JDWP (JAVA_TOOL_OPTIONS for forked JVMs).
   */
  fun startGradleDebug(projectDir: File, task: String = "build", port: Int = DEFAULT_PORT): Boolean {
    if (isActive) {
      Logger.warn("Debug session already active")
      return false
    }
    if (!GradleRunSupport.isGradleProject(projectDir)) {
      Logger.warn("Not a Gradle project")
      return false
    }
    val gradle = GradleRunSupport.findGradleCommand(projectDir) ?: return false
    return startDebug(gradle + task, projectDir, port)
  }

  fun startDebug(command: List<String>, workingDir: File, port: Int = DEFAULT_PORT): Boolean {
    if (isActive) return false
    debugPort = port
    setState(DebugSessionState.STARTING)

    val listener = object : ProcessOutputListener {
      override fun onStarted(commandLine: String) {
        setState(DebugSessionState.RUNNING)
        Logger.info("Debug session running (JDWP *:$port): $commandLine")
      }

      override fun onOutput(text: String) {}

      override fun onFinished(exitCode: Int) {
        setState(DebugSessionState.STOPPED)
        processRunner.removeListener(this)
        Logger.info("Debug session finished: $exitCode")
      }
    }
    processRunner.addListener(listener)

    val jdwp = "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$port"
    val previous = System.getenv("JAVA_TOOL_OPTIONS")
    val env = mapOf(
      "JAVA_TOOL_OPTIONS" to listOfNotNull(previous?.takeIf { it.isNotBlank() }, jdwp).joinToString(" ")
    )

    val ok = processRunner.start(command, workingDir, env)
    if (!ok) {
      processRunner.removeListener(listener)
      setState(DebugSessionState.STOPPED)
    }
    return ok
  }

  fun stop() {
    if (processRunner.isRunning) {
      processRunner.stop()
    }
    setState(DebugSessionState.STOPPED)
  }

  private fun setState(newState: DebugSessionState) {
    myState.set(newState)
    myListeners.forEach { it.invoke(newState) }
  }

  companion object {
    const val DEFAULT_PORT = 5005
  }
}
