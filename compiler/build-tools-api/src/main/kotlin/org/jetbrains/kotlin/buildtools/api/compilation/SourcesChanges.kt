/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UNUSED_PARAMETER")

package org.jetbrains.kotlin.buildtools.api.compilation

import java.io.File

sealed class SourcesChanges {
    object NotKnown : SourcesChanges()

    class Known(modifiedFiles: List<File>, removedFiles: List<File>)
}