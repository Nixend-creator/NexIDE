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

package com.nexide.platform.util

/**
 * Simple logger facade used across the platform.
 * Will later be replaced by a more advanced logging infrastructure.
 */
object Logger {
  private val ourLock = Any()

  fun info(message: String) {
    log("INFO", message)
  }

  fun warn(message: String) {
    log("WARN", message)
  }

  fun error(message: String, throwable: Throwable? = null) {
    log("ERROR", message)
    throwable?.printStackTrace()
  }

  private fun log(level: String, message: String) {
    synchronized(ourLock) {
      println("[$level] $message")
    }
  }
}
