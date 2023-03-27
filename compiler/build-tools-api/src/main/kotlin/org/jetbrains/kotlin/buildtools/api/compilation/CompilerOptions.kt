/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import java.io.File

sealed class CompilerOptions {
    class Daemon(
        /**
         * Kotlin daemon classpath
         */
        val classpath: List<File>,
        /**
         * A directory for storing some state of Kotlin daemon
         */
        val sessionDir: File,
        /**
         * A list of JVM arguments used at the daemon startup
         */
        val jvmArguments: List<String>,
    ) : CompilerOptions()

    class InProcess : CompilerOptions()
}