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

import com.nexide.platform.psi.PsiElement
import com.nexide.platform.psi.PsiElementVisitor
import com.nexide.platform.psi.PsiFile

/**
 * Base implementation of [PsiElement].
 */
abstract class PsiElementBase : PsiElement {

  private var myParent: PsiElement? = null
  private var myValid = true

  override val parent: PsiElement?
    get() = myParent

  override val containingFile: PsiFile?
    get() {
      var current: PsiElement? = this
      while (current != null) {
        if (current is PsiFile) return current
        current = current.parent
      }
      return null
    }

  override val isValid: Boolean
    get() = myValid

  internal fun setParent(parent: PsiElement?) {
    myParent = parent
  }

  internal fun markInvalid() {
    myValid = false
  }

  override fun accept(visitor: PsiElementVisitor) {
    visitor.visitElement(this)
  }

  override fun toString(): String = "${this::class.simpleName}($textRangeStart-$textRangeEnd)"
}
