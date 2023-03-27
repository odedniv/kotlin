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
    /**
     * The directories that compiler will clean in the case of fallback to non-incremental compilation.
     *
     * If the value is set to `null`, then the default value is calculated as a list of [workingDir] and the classes output directory from the compiler arguments.
     *
     * If the value is set explicitly, it must contain the mentioned above default directories.
     */
    val outputDirs: List<File>?,
    /**
     * A compiler working dir for caches
     */
    val workingDir: File,
    /**
     * The root project directory, used to resolve relative paths
     */
    val rootProjectDir: File,
    // TODO: the following options are not stable
    // val usePreciseJavaTracking: Boolean,
    // val preciseCompilationResultsBackup: Boolean = false,
    // val keepIncrementalCompilationCachesInMemory: Boolean = false,
) : CompilationOptions(targetPlatform, kotlinScriptExtensions)

class IntraModuleIncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
    sourcesChanges: SourcesChanges,
    outputDirs: List<File>? = null,
    workingDir: File,
    rootProjectDir: File,
) : IncrementalCompilationOptions(targetPlatform, kotlinScriptExtensions, sourcesChanges, outputDirs, workingDir, rootProjectDir)

class HistoryFilesBasedIncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
    sourcesChanges: SourcesChanges,
    outputDirs: List<File>? = null,
    workingDir: File,
    rootProjectDir: File,
    /**
     * A path where build history of the module will be located
     */
    val buildHistoryFile: File,
    val useModuleDetection: Boolean, // TODO rename it, the name isn't clear
    /**
     * Modules meta information required to find their build-history files.
     */
    val modulesInfo: IncrementalModuleInfo,
) : IncrementalCompilationOptions(targetPlatform, kotlinScriptExtensions, sourcesChanges, outputDirs, workingDir, rootProjectDir)

class ClasspathSnapshotBasedIncrementalCompilationOptions(
    targetPlatform: TargetPlatform,
    kotlinScriptExtensions: List<String>,
    sourcesChanges: SourcesChanges,
    outputDirs: List<File>? = null,
    workingDir: File,
    rootProjectDir: File,
    val classpathChanges: ClasspathChanges,
) : IncrementalCompilationOptions(targetPlatform, kotlinScriptExtensions, sourcesChanges, outputDirs, workingDir, rootProjectDir)