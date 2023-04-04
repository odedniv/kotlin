/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

sealed class CompilationOptions(
    val targetPlatform: TargetPlatform,
    val kotlinScriptExtensions: List<String>,
)

class IncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
    val sourcesChanges: SourcesChanges,
    val classpathChanges: ClasspathChanges,
) : CompilationOptions(targetPlatform, kotlinScriptExtensions)

class NonIncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
) : CompilationOptions(targetPlatform, kotlinScriptExtensions)