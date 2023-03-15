/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.utils.getOrPut

internal class KotlinToolingDiagnosticsReporter(private val project: Project) {
    fun reportDiagnostics() {
        val collector = project.kotlinToolingDiagnosticsCollector
        val logger = project.logger
        val isVerbose = project.kotlinPropertiesProvider.internalVerboseDiagnostics

        val diagnostics = collector.getDiagnosticsForProject(project)
        for (diagnostic in diagnostics) {
            when (diagnostic.severity) {
                ToolingDiagnostic.Severity.WARNING -> logger.warn("w: ${diagnostic.render(isVerbose)}\n")
                ToolingDiagnostic.Severity.ERROR -> logger.error("e: ${diagnostic.render(isVerbose)}\n")
                ToolingDiagnostic.Severity.FATAL ->
                    error("Internal error: FATAL diagnostics throw an exception immediately in KotlinToolingDiagnosticsCollector")
            }
        }
    }

    private fun ToolingDiagnostic.render(isVerbose: Boolean): String =
        if (isVerbose) toString() + "\n$DIAGNOSTIC_SEPARATOR" else message

    companion object {
        internal const val ID = "kotlin.GradleBuildKotlinDiagnosticsReporter"
        const val DIAGNOSTIC_SEPARATOR = "#diagnostic-end"
    }
}

internal val Project.kotlinToolingDiagnosticsReporter: KotlinToolingDiagnosticsReporter
    get() = project.extraProperties.getOrPut(KotlinToolingDiagnosticsReporter.ID) { KotlinToolingDiagnosticsReporter(this) }
