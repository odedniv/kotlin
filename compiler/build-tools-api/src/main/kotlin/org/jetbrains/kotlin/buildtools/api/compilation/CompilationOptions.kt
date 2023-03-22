/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import java.io.File

sealed class CompilationOptions(
    val targetPlatform: TargetPlatform,
    val kotlinScriptExtensions: List<String>,
)

class NonIncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
) : CompilationOptions(targetPlatform, kotlinScriptExtensions)

abstract class IncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
    val sourcesChanges: SourcesChanges,
    // val outputFiles: List<File>,
    // val workingDir: File,
    // TODO: the following options are not stable
    // val usePreciseJavaTracking: Boolean,
    // val preciseCompilationResultsBackup: Boolean = false,
    // val keepIncrementalCompilationCachesInMemory: Boolean = false,
    // TODO: probably do not pass the following args directly, automatically determine their values depending on callbacks
    // reportCategories: Array<Int>,
    // reportSeverity: Int,
    // requestedCompilationResults: Array<Int>,
) : CompilationOptions(targetPlatform, kotlinScriptExtensions)

class IntraModuleIncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
    sourcesChanges: SourcesChanges,
) : IncrementalCompilationOptions(targetPlatform, kotlinScriptExtensions, sourcesChanges)

class HistoryFilesBasedIncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
    sourcesChanges: SourcesChanges,
    val buildHistoryFile: File,
    val useModuleDetection: Boolean, // TODO rename it, the name isn't clear
) : IncrementalCompilationOptions(targetPlatform, kotlinScriptExtensions, sourcesChanges)

class ClasspathSnapshotBasedIncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
    sourcesChanges: SourcesChanges,
    val classpathChanges: ClasspathChanges,
) : IncrementalCompilationOptions(targetPlatform, kotlinScriptExtensions, sourcesChanges)