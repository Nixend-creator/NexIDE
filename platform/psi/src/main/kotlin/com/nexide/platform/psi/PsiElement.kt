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

/**
 * Base interface for all elements in the Program Structure Interface tree.
 * Analogous to com.intellij.psi.PsiElement.
 */
interface PsiElement {
  /** Parent element, or null for the root (usually PsiFile). */
  val parent: PsiElement?

  /** Containing file. */
  val containingFile: PsiFile?

  /** Direct children of this element. */
  val children: Array<PsiElement>

  /** Text range of this element in the document (start offset). */
  val textRangeStart: Int

  /** Text range of this element in the document (end offset). */
  val textRangeEnd: Int

  /** Text covered by this element. */
  val text: String

  /** True while the element is still valid (not invalidated by changes). */
  val isValid: Boolean

  /** Accepts a visitor (Visitor pattern). */
  fun accept(visitor: PsiElementVisitor)

  fun acceptChildren(visitor: PsiElementVisitor) {
    children.forEach { it.accept(visitor) }
  }
}
