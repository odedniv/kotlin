/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.presetName
import java.util.*

val MASKED_TARGET_NAME = HostManager.host.presetName.lowercase(Locale.getDefault())

fun findParameterInOutput(name: String, output: String): String? =
    output.lineSequence().mapNotNull { line ->
        val (key, value) = line.split('=', limit = 2).takeIf { it.size == 2 } ?: return@mapNotNull null
        if (key.endsWith(name)) value else null
    }.firstOrNull()

/**
 * Extracts classpath of given task's output
 *
 * @param taskOutput debug level output of the task
 * @param toolName compiler type
 *
 * @return list of dependencies in classpath
 */
fun extractNativeCompilerClasspath(taskOutput: String, toolName: NativeToolKind): List<String> =
    extractNativeToolSettings(taskOutput, toolName, NativeToolSettingsKind.COMPILER_CLASSPATH).toList()

enum class NativeToolKind(val title: String) {
    KONANC("konanc")
}

private enum class NativeToolSettingsKind(val title: String) {
    COMPILER_CLASSPATH("Classpath")
}

private fun extractNativeToolSettings(
    taskOutput: String,
    toolName: NativeToolKind,
    settingsKind: NativeToolSettingsKind
): Sequence<String> {
    val settingsPrefix = "${settingsKind.title} = ["
    val settings = taskOutput.lineSequence()
        .dropWhile {
            "Run in-process tool \"${toolName.title}\"" !in it && "Run \"${toolName.title}\" tool in a separate JVM process" !in it
        }
        .drop(1)
        .dropWhile {
            settingsPrefix !in it
        }

    val settingsHeader = settings.firstOrNull()
    check(settingsHeader != null && settingsPrefix in settingsHeader) {
        "Cannot find setting '${settingsKind.title}'"
    }

    return if (settingsHeader.trimEnd().endsWith(']'))
        emptySequence() // No parameters.
    else
        settings.drop(1).map { it.trim() }.takeWhile { it != "]" }
}