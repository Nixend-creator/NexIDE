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

package com.nexide.platform.psi

import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.plugin.ExtensionPointName

/**
 * Extension point for language-specific PSI builders.
 * Plugins register parsers for their languages.
 */
interface LanguageParser {
  /** Language id this parser handles, e.g. "kotlin", "java". */
  val language: String

  /**
   * Builds a PSI file for the given virtual file and text.
   * Return null to fall back to the default plain-text PSI.
   */
  fun parse(virtualFile: VirtualFile, text: String): PsiFile?
}

val LANGUAGE_PARSER_EP = ExtensionPointName<LanguageParser>("com.nexide.languageParser")
