/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.cli.common.messages.*

internal object DefaultMessageCollectorLoggingAdapter : MessageCollector {
    private val messageRenderer = MessageRenderer.PLAIN_FULL_PATHS
    override fun clear() {}

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val renderedMessage = messageRenderer.render(severity, message, location)
        if (severity.isError) {
            System.err.println(renderedMessage)
        } else {
            println(renderedMessage)
        }
    }

    override fun hasErrors() = false
}

internal class KotlinLoggerMessageCollectorAdapter(private val kotlinLogger: KotlinLogger) : MessageCollector {
    private val messageRenderer = MessageRenderer.GRADLE_STYLE

    override fun clear() {}

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val renderedMessage = messageRenderer.render(severity, message, location) // TODO: do not render it here
        when (severity) {
            CompilerMessageSeverity.EXCEPTION, CompilerMessageSeverity.ERROR -> kotlinLogger.error(renderedMessage)
            CompilerMessageSeverity.STRONG_WARNING, CompilerMessageSeverity.WARNING -> kotlinLogger.warn(renderedMessage)
            CompilerMessageSeverity.INFO -> kotlinLogger.info(renderedMessage)
            CompilerMessageSeverity.OUTPUT, CompilerMessageSeverity.LOGGING -> kotlinLogger.debug(renderedMessage)
        }
    }

    override fun hasErrors() = false
}