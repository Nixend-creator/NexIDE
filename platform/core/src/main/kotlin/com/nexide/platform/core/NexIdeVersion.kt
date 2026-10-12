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

package com.nexide.platform.core

/**
 * Single source of truth for the product version shown in About / logs.
 * Keep in sync with README and ide-app packageVersion when releasing.
 */
object NexIdeVersion {
  const val NAME = "NexIDE"
  const val VERSION = "0.2.2"
  const val DISPLAY = "$NAME $VERSION"
}
