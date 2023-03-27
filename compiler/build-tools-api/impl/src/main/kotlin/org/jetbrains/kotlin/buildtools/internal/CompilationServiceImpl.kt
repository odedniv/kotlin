/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.compilation.*
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.arguments.validateArguments
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.compilerRunner.KotlinCompilerRunnerUtils
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.daemon.client.BasicCompilerServicesWithResultsFacadeServer
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.CompilationOptions as DaemonCompilationOptions
import org.jetbrains.kotlin.daemon.common.IncrementalCompilationOptions as DaemonIncrementalCompilationOptions
import java.io.Serializable
import java.rmi.server.UnicastRemoteObject

private val ExitCode.asCompilationResult
    get() = when (this) {
        ExitCode.OK -> CompilationResult.SUCCESSFUL
        ExitCode.COMPILATION_ERROR -> CompilationResult.COMPILATION_ERROR
        ExitCode.INTERNAL_ERROR -> CompilationResult.INTERNAL_ERROR
        ExitCode.OOM_ERROR -> CompilationResult.OOM_ERROR
        else -> error("Unexpected exit code: $this")
    }

val CompilationOptions.asDaemonCompilationOptions: DaemonCompilationOptions
    get() {
        val ktsExtensionsAsArray = if (kotlinScriptExtensions.isEmpty()) null else kotlinScriptExtensions.toTypedArray()
        val reportCategories = arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.IC_MESSAGE.code) // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val reportSeverity = ReportSeverity.INFO.code // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val requestedCompilationResults = emptyArray<Int>() // TODO: automagically compute the value, related to DaemonCompilationResults
        return when (this) {
            is NonIncrementalCompilationOptions -> DaemonCompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = targetPlatform,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = requestedCompilationResults,
                kotlinScriptExtensions = ktsExtensionsAsArray,
            )
            is IncrementalCompilationOptions -> DaemonIncrementalCompilationOptions(
                compilerMode = CompilerMode.INCREMENTAL_COMPILER,
                targetPlatform = targetPlatform,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = requestedCompilationResults,
                kotlinScriptExtensions = ktsExtensionsAsArray,
                areFileChangesKnown = sourcesChanges is SourcesChanges.Known,
                modifiedFiles = (sourcesChanges as? SourcesChanges.Known)?.modifiedFiles,
                deletedFiles = (sourcesChanges as? SourcesChanges.Known)?.removedFiles,
                classpathChanges = (this as? ClasspathSnapshotBasedIncrementalCompilationOptions)?.classpathChanges
                    ?: ClasspathChanges.ClasspathSnapshotDisabled,
                workingDir = workingDir,
                rootProjectDir = rootProjectDir,
                usePreciseJavaTracking = false, // TODO: think how to pass it
                outputFiles = outputDirs,
                multiModuleICSettings = (this as? HistoryFilesBasedIncrementalCompilationOptions)?.run {
                    MultiModuleICSettings(
                        buildHistoryFile = buildHistoryFile,
                        useModuleDetection = useModuleDetection,
                    )
                },
                modulesInfo = (this as? HistoryFilesBasedIncrementalCompilationOptions)?.modulesInfo,
                withAbiSnapshot = false, // the ABI snapshots is not supported to run via the build-tools-api, and there's no handles to run it currently
                preciseCompilationResultsBackup = false, // TODO: think how to pass it
                keepIncrementalCompilationCachesInMemory = false, // TODO: think how to pass it
            )
        }
    }

internal class CompilationServiceImpl : CompilationService {
    override fun compile(compilerOptions: CompilerOptions, arguments: List<String>, compilationOptions: CompilationOptions) =
        when (compilerOptions) {
            is CompilerOptions.Daemon -> compileWithinDaemon(compilerOptions, arguments, compilationOptions)
            is CompilerOptions.InProcess -> compileInProcess(arguments, compilationOptions)
        }

    private fun compileWithinDaemon(
        compilerOptions: CompilerOptions.Daemon,
        arguments: List<String>,
        compilationOptions: CompilationOptions
    ): CompilationResult {
        println("Compiling with daemon")
        val compilerId = CompilerId.makeCompilerId(compilerOptions.classpath)
        val clientIsAliveFlagFile = compilerOptions.sessionDir.resolve("1") // TODO add managing of the file
        val sessionIsAliveFlagFile = compilerOptions.sessionDir.resolve("2") // TODO add managing of the file
        val messageCollector = DefaultMessageCollectorLoggingAdapter() // TODO: add a logger which will return the messages to the caller via callbacks

        val jvmOptions = configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = false,
            inheritAdditionalProperties = true
        ).also { opts ->
            if (compilerOptions.jvmArguments.isNotEmpty()) {
                opts.jvmParams.addAll(
                    compilerOptions.jvmArguments.filterExtractProps(opts.mappers, "", opts.restMapper)
                )
            }
        }
        val (daemon, sessionId) = KotlinCompilerRunnerUtils.newDaemonConnection(
            compilerId,
            clientIsAliveFlagFile,
            sessionIsAliveFlagFile,
            messageCollector,
            false,
            daemonJVMOptions = jvmOptions
        ) ?: error("Can't get connection")
        val daemonCompileOptions = compilationOptions.asDaemonCompilationOptions
        val exitCode = daemon.compile(
            sessionId,
            arguments.toTypedArray(),
            daemonCompileOptions,
            BasicCompilerServicesWithResultsFacadeServer(messageCollector),
            DaemonCompilationResults()
        ).get()
        return (ExitCode.values().find { it.code == exitCode } ?: if (exitCode == 0) {
            ExitCode.OK
        } else {
            ExitCode.COMPILATION_ERROR
        }).asCompilationResult
    }

    private fun compileInProcess(
        arguments: List<String>,
        compilationOptions: CompilationOptions
    ) = when (compilationOptions) {
        is IncrementalCompilationOptions -> TODO("Incremental compilation is not yet supported for running via build-tools-api")
        is NonIncrementalCompilationOptions -> {
            @Suppress("UNCHECKED_CAST")
            val compiler = when (val targetPlatform = compilationOptions.targetPlatform) {
                TargetPlatform.JVM -> K2JVMCompiler()
                TargetPlatform.JS -> K2JSCompiler()
                TargetPlatform.METADATA -> K2MetadataCompiler()
                else -> error("Unsupported target platform: $targetPlatform")
            } as CLICompiler<CommonCompilerArguments>
            val parsedArguments = compiler.createArguments()
            parseCommandLineArguments(arguments, parsedArguments)
            val argumentParseError = validateArguments(parsedArguments.errors)
            if (argumentParseError != null) {
                throw CompilationArgumentsParseException(argumentParseError)
            }
            // TODO: add a logger which will return the messages to the caller via callbacks
            compiler.exec(DefaultMessageCollectorLoggingAdapter(), Services.EMPTY, parsedArguments).asCompilationResult
        }
    }
}

private class DaemonCompilationResults : CompilationResults,
    UnicastRemoteObject(
        SOCKET_ANY_FREE_PORT,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory
    ) {
    /**
     * Possible combinations:
     * 1. [CompilationResultCategory.IC_COMPILE_ITERATION.code]       -> a [CompileIterationResult] instance
     * 2. [CompilationResultCategory.BUILD_REPORT_LINES.code]         -> a [List] of [String]
     * 3. [CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES.code] -> a [List] of [String]
     * 4. [CompilationResultCategory.BUILD_METRICS.code]              -> a [BuildMetrics] instance
     **/
    override fun add(compilationResultCategory: Int, value: Serializable) {
        // TODO propagate the values to the caller via callbacks, requires to make metrics a part of the API
        println("Result category=$compilationResultCategory value=$value")
    }
}
