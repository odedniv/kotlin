/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

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
import org.jetbrains.kotlin.config.Services

private val ExitCode.asCompilationResult
    get() = when (this) {
        ExitCode.OK -> CompilationResult.SUCCESSFUL
        ExitCode.COMPILATION_ERROR -> CompilationResult.COMPILATION_ERROR
        ExitCode.INTERNAL_ERROR -> CompilationResult.INTERNAL_ERROR
        ExitCode.OOM_ERROR -> CompilationResult.OOM_ERROR
        else -> error("Unexpected exit code: $this")
    }


internal class CompilationServiceImpl : CompilationService {
    override fun compile(compilerOptions: CompilerOptions, arguments: List<String>, compilationOptions: CompilationOptions) =
        when (compilerOptions) {
            is CompilerOptions.Daemon -> TODO("Compilation with daemon is not yet supported for running via build-tools-api")
            is CompilerOptions.InProcess -> compileInProcess(arguments, compilationOptions)
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
            compiler.exec(DefaultMessageCollectorLoggingAdapter(), Services.EMPTY, parsedArguments).asCompilationResult
        }
    }
}