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

import com.nexide.platform.util.Logger
import com.sun.jdi.ArrayReference
import com.sun.jdi.Bootstrap
import com.sun.jdi.LocalVariable
import com.sun.jdi.Location
import com.sun.jdi.ObjectReference
import com.sun.jdi.PrimitiveValue
import com.sun.jdi.StackFrame
import com.sun.jdi.StringReference
import com.sun.jdi.ThreadReference
import com.sun.jdi.Value
import com.sun.jdi.VirtualMachine
import com.sun.jdi.connect.AttachingConnector
import com.sun.jdi.event.BreakpointEvent
import com.sun.jdi.event.StepEvent
import com.sun.jdi.event.VMDeathEvent
import com.sun.jdi.event.VMDisconnectEvent
import com.sun.jdi.request.BreakpointRequest
import com.sun.jdi.request.StepRequest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * One variable or field visible at a suspend point.
 */
data class DebugVariable(
  val name: String,
  val typeName: String,
  val value: String,
  val scope: String = "local" // local | this | arg
)

/**
 * Snapshot of a suspended thread at a breakpoint.
 */
data class SuspendedThreadInfo(
  val threadName: String,
  val threadId: Long,
  val locationDescription: String,
  val frames: List<String>,
  val variables: List<DebugVariable> = emptyList()
)

/**
 * Connects to a JDWP target, installs line breakpoints, listens for suspend events.
 */
class JdiConnector {

  private val myVm = AtomicReference<VirtualMachine?>(null)
  private val myListening = AtomicBoolean(false)
  private val myEventThread = AtomicReference<Thread?>(null)
  private val myListeners = CopyOnWriteArrayList<JdiListener>()
  private val myRequests = mutableListOf<BreakpointRequest>()
  private val mySuspendedThread = AtomicReference<ThreadReference?>(null)
  private val myStepRequests = mutableListOf<StepRequest>()

  val isAttached: Boolean
    get() = myVm.get() != null

  val isSuspended: Boolean
    get() = mySuspendedThread.get() != null

  fun addListener(listener: JdiListener) {
    myListeners.add(listener)
  }

  fun removeListener(listener: JdiListener) {
    myListeners.remove(listener)
  }

  /**
   * Attach to a VM listening on [host]:[port] (default localhost:5005).
   */
  fun attach(host: String = "localhost", port: Int = DebugSessionManager.DEFAULT_PORT): Boolean {
    if (isAttached) {
      Logger.warn("Already attached")
      return true
    }
    return try {
      val connector = findSocketAttachConnector()
        ?: run {
          Logger.error("No socket attach connector found")
          return false
        }
      val args = connector.defaultArguments()
      args["hostname"]?.setValue(host)
      args["port"]?.setValue(port.toString())
      // Short timeout so UI doesn't hang forever
      args["timeout"]?.setValue("5000")

      Logger.info("JDI attaching to $host:$port …")
      val vm = connector.attach(args)
      myVm.set(vm)
      startEventLoop(vm)
      fireAttached(vm.name())
      Logger.info("JDI attached to ${vm.name()}")
      true
    } catch (e: Exception) {
      Logger.error("JDI attach failed: ${e.message}", e)
      fireMessage("Attach failed: ${e.message}")
      false
    }
  }

  /**
   * Install breakpoints from [BreakpointManager] into the attached VM.
   * Matches by source file name (simple name) and line.
   */
  fun installBreakpoints(breakpoints: List<Breakpoint>): Int {
    val vm = myVm.get() ?: return 0
    clearBreakpointRequests()
    var installed = 0
    for (bp in breakpoints) {
      if (!bp.enabled) continue
      val simpleName = bp.filePath.substringAfterLast('/').substringAfterLast('\\')
      try {
        val classes = vm.classesByName(guessClassName(simpleName))
        // Also scan all classes for matching source
        val candidates = if (classes.isNotEmpty()) {
          classes
        } else {
          vm.allClasses().filter { ref ->
            try {
              ref.sourceName() == simpleName
            } catch (_: Exception) {
              false
            }
          }
        }
        for (cls in candidates) {
          try {
            val locations = cls.locationsOfLine(bp.line)
            for (loc in locations) {
              val req = vm.eventRequestManager().createBreakpointRequest(loc)
              req.setSuspendPolicy(BreakpointRequest.SUSPEND_EVENT_THREAD)
              req.enable()
              myRequests.add(req)
              installed++
              Logger.info("Breakpoint installed: $simpleName:${bp.line} @ $loc")
            }
          } catch (e: Exception) {
            Logger.warn("Cannot set BP ${simpleName}:${bp.line}: ${e.message}")
          }
        }
      } catch (e: Exception) {
        Logger.warn("Breakpoint lookup failed for ${bp.filePath}:${bp.line}: ${e.message}")
      }
    }
    fireMessage("Installed $installed breakpoint request(s)")
    return installed
  }

  fun resumeAll() {
    val vm = myVm.get() ?: return
    try {
      clearStepRequests()
      mySuspendedThread.set(null)
      vm.resume()
      fireMessage("VM resumed")
    } catch (e: Exception) {
      Logger.warn("Resume failed: ${e.message}")
    }
  }

  /**
   * Step over the current line in the suspended thread (STEP_LINE + STEP_OVER).
   */
  fun stepOver(): Boolean = step(StepRequest.STEP_OVER)

  /**
   * Step into calls on the current line (STEP_LINE + STEP_INTO).
   */
  fun stepInto(): Boolean = step(StepRequest.STEP_INTO)

  /**
   * Step out of the current method (STEP_LINE + STEP_OUT).
   */
  fun stepOut(): Boolean = step(StepRequest.STEP_OUT)

  private fun step(depth: Int): Boolean {
    val vm = myVm.get() ?: return false
    val thread = mySuspendedThread.get() ?: run {
      fireMessage("Not suspended — cannot step")
      return false
    }
    return try {
      clearStepRequests()
      val req = vm.eventRequestManager().createStepRequest(
        thread,
        StepRequest.STEP_LINE,
        depth
      )
      req.addCountFilter(1)
      req.setSuspendPolicy(StepRequest.SUSPEND_EVENT_THREAD)
      req.enable()
      myStepRequests.add(req)
      mySuspendedThread.set(null)
      vm.resume()
      val label = when (depth) {
        StepRequest.STEP_OVER -> "Step Over"
        StepRequest.STEP_INTO -> "Step Into"
        StepRequest.STEP_OUT -> "Step Out"
        else -> "Step"
      }
      fireMessage(label)
      true
    } catch (e: Exception) {
      Logger.warn("Step failed: ${e.message}")
      fireMessage("Step failed: ${e.message}")
      false
    }
  }

  private fun clearStepRequests() {
    val vm = myVm.get()
    myStepRequests.forEach { req ->
      try {
        req.disable()
        vm?.eventRequestManager()?.deleteEventRequest(req)
      } catch (_: Exception) {
      }
    }
    myStepRequests.clear()
  }

  fun listThreads(): List<String> {
    val vm = myVm.get() ?: return emptyList()
    return try {
      vm.allThreads().map { t ->
        val status = when (t.status()) {
          ThreadReference.THREAD_STATUS_RUNNING -> "RUNNING"
          ThreadReference.THREAD_STATUS_WAIT -> "WAIT"
          ThreadReference.THREAD_STATUS_ZOMBIE -> "ZOMBIE"
          ThreadReference.THREAD_STATUS_MONITOR -> "MONITOR"
          ThreadReference.THREAD_STATUS_NOT_STARTED -> "NOT_STARTED"
          ThreadReference.THREAD_STATUS_SLEEPING -> "SLEEPING"
          else -> "UNKNOWN"
        }
        "${t.name()} [$status]"
      }
    } catch (e: Exception) {
      Logger.warn("listThreads: ${e.message}")
      emptyList()
    }
  }

  fun detach() {
    myListening.set(false)
    myEventThread.get()?.interrupt()
    clearStepRequests()
    clearBreakpointRequests()
    mySuspendedThread.set(null)
    val vm = myVm.getAndSet(null)
    try {
      vm?.dispose()
    } catch (_: Exception) {
    }
    fireDetached()
  }

  private fun startEventLoop(vm: VirtualMachine) {
    myListening.set(true)
    val thread = Thread({
      try {
        while (myListening.get()) {
          val eventSet = vm.eventQueue().remove(1000) ?: continue
          var resume = true
          for (event in eventSet) {
            when (event) {
              is BreakpointEvent -> {
                resume = false
                handleBreakpoint(event)
              }
              is StepEvent -> {
                resume = false
                handleStep(event)
              }
              is VMDeathEvent, is VMDisconnectEvent -> {
                myListening.set(false)
                myVm.set(null)
                mySuspendedThread.set(null)
                fireDetached()
                return@Thread
              }
              else -> {
                // ignore
              }
            }
          }
          if (resume) {
            try {
              eventSet.resume()
            } catch (_: Exception) {
            }
          }
        }
      } catch (e: InterruptedException) {
        // stop
      } catch (e: Exception) {
        if (myListening.get()) {
          Logger.warn("JDI event loop: ${e.message}")
        }
      }
    }, "nexide-jdi-events")
    thread.isDaemon = true
    myEventThread.set(thread)
    thread.start()
  }

  private fun handleBreakpoint(event: BreakpointEvent) {
    try {
      clearStepRequests()
      val thread = event.thread()
      mySuspendedThread.set(thread)
      fireSuspended(buildSuspendedInfo(thread, event.location()))
    } catch (e: Exception) {
      Logger.warn("handleBreakpoint: ${e.message}")
    }
  }

  private fun handleStep(event: StepEvent) {
    try {
      clearStepRequests()
      val thread = event.thread()
      mySuspendedThread.set(thread)
      fireSuspended(buildSuspendedInfo(thread, event.location()))
    } catch (e: Exception) {
      Logger.warn("handleStep: ${e.message}")
    }
  }

  private fun buildSuspendedInfo(thread: ThreadReference, loc: Location): SuspendedThreadInfo {
    val frames = try {
      thread.frames().take(16).map { frame ->
        val fl = frame.location()
        "${fl.declaringType().name()}.${fl.method().name()}:${fl.lineNumber()}"
      }
    } catch (_: Exception) {
      emptyList()
    }
    val variables = try {
      if (thread.frameCount() > 0) collectVariables(thread.frame(0)) else emptyList()
    } catch (e: Exception) {
      Logger.warn("collectVariables: ${e.message}")
      emptyList()
    }
    return SuspendedThreadInfo(
      threadName = thread.name(),
      threadId = thread.uniqueID(),
      locationDescription = formatLocation(loc),
      frames = frames,
      variables = variables
    )
  }

  private fun collectVariables(frame: StackFrame): List<DebugVariable> {
    val result = mutableListOf<DebugVariable>()
    try {
      val visible: List<LocalVariable> = try {
        frame.visibleVariables()
      } catch (_: Exception) {
        emptyList()
      }
      val values = try {
        frame.getValues(visible)
      } catch (_: Exception) {
        emptyMap()
      }
      for (variable in visible) {
        val value = values[variable]
        val scope = if (variable.isArgument) "arg" else "local"
        result.add(
          DebugVariable(
            name = variable.name(),
            typeName = try {
              variable.typeName()
            } catch (_: Exception) {
              "?"
            },
            value = formatValue(value),
            scope = scope
          )
        )
      }
    } catch (e: Exception) {
      Logger.warn("locals: ${e.message}")
    }

    // this fields (shallow)
    try {
      val thisObj = frame.thisObject()
      if (thisObj != null) {
        val type = thisObj.referenceType()
        for (field in type.fields().take(32)) {
          if (field.isStatic) continue
          try {
            val value = thisObj.getValue(field)
            result.add(
              DebugVariable(
                name = "this.${field.name()}",
                typeName = try {
                  field.typeName()
                } catch (_: Exception) {
                  "?"
                },
                value = formatValue(value),
                scope = "this"
              )
            )
          } catch (_: Exception) {
          }
        }
      }
    } catch (_: Exception) {
    }
    return result
  }

  private fun formatValue(value: Value?): String {
    if (value == null) return "null"
    return try {
      when (value) {
        is PrimitiveValue -> value.toString()
        is StringReference -> "\"${value.value()}\""
        is ArrayReference -> {
          val len = value.length()
          val preview = (0 until minOf(len, 8)).joinToString(", ") { i ->
            formatValue(value.getValue(i))
          }
          val more = if (len > 8) ", …" else ""
          "[$preview$more] (len=$len)"
        }
        is ObjectReference -> {
          val typeName = value.referenceType().name().substringAfterLast('.')
          // try toString if safe (may invoke method — skip for safety)
          "$typeName@${Integer.toHexString(value.uniqueID().toInt())}"
        }
        else -> value.toString()
      }
    } catch (e: Exception) {
      "<error: ${e.message}>"
    }
  }

  private fun clearBreakpointRequests() {
    val vm = myVm.get()
    myRequests.forEach { req ->
      try {
        req.disable()
        vm?.eventRequestManager()?.deleteEventRequest(req)
      } catch (_: Exception) {
      }
    }
    myRequests.clear()
  }

  private fun findSocketAttachConnector(): AttachingConnector? {
    return Bootstrap.virtualMachineManager().attachingConnectors()
      .firstOrNull { it.transport().name() == "dt_socket" }
  }

  private fun guessClassName(sourceFileName: String): String {
    // Main.kt / Main.java → Main (package unknown; classesByName needs FQN often)
    return sourceFileName.substringBeforeLast('.').replace('.', '$')
  }

  private fun formatLocation(loc: Location): String {
    return try {
      "${loc.sourceName()}:${loc.lineNumber()} (${loc.declaringType().name()}.${loc.method().name()})"
    } catch (_: Exception) {
      loc.toString()
    }
  }

  private fun fireAttached(name: String) {
    myListeners.forEach { it.onAttached(name) }
  }

  private fun fireDetached() {
    myListeners.forEach { it.onDetached() }
  }

  private fun fireSuspended(info: SuspendedThreadInfo) {
    myListeners.forEach { it.onBreakpointHit(info) }
  }

  private fun fireMessage(msg: String) {
    myListeners.forEach { it.onMessage(msg) }
  }
}

interface JdiListener {
  fun onAttached(vmName: String) {}
  fun onDetached() {}
  fun onBreakpointHit(info: SuspendedThreadInfo) {}
  fun onMessage(message: String) {}
}
