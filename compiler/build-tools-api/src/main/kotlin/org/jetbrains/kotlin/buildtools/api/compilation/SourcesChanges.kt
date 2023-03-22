/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import java.io.File
import java.io.Serializable

sealed class SourcesChanges : Serializable {
    object Unknown : SourcesChanges()

    class Known(val modifiedFiles: List<File>, val removedFiles: List<File>) : SourcesChanges()
}