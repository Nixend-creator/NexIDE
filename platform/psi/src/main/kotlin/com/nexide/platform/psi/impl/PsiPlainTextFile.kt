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

package com.nexide.platform.psi.impl

import com.nexide.platform.core.vfs.VirtualFile
import com.nexide.platform.psi.PsiElement
import com.nexide.platform.psi.PsiElementVisitor
import com.nexide.platform.psi.PsiFile

/**
 * Simple PSI file that treats the whole content as a single plain-text leaf.
 * Used as a fallback when no language-specific parser is available.
 */
class PsiPlainTextFile(
  override val virtualFile: VirtualFile,
  private val myText: String,
  override val language: String = "TEXT"
) : PsiElementBase(), PsiFile {

  override val children: Array<PsiElement> = emptyArray()

  override val textRangeStart: Int = 0

  override val textRangeEnd: Int
    get() = myText.length

  override val text: String
    get() = myText

  override fun accept(visitor: PsiElementVisitor) {
    visitor.visitFile(this)
  }

  override fun toString(): String = "PsiPlainTextFile:${virtualFile.name}"
}
