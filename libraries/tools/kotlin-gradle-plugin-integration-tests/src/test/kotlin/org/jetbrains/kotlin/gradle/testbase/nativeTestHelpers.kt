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