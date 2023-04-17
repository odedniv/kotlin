/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertTrue

@TestDataPath("\$PROJECT_ROOT")
class LinkerOutputTestKT55578 : AbstractNativeLinkerOutputTest() {
    @Test
    fun testLinkerOutput() {
        val testDir = File("kotlin-native/backend.native/tests/interop/userSetupHint/")
        val defFile = testDir.resolve("userSetupHint.def")

        val libraryTestCase: TestCase = generateCInteropTestCaseWithSingleDef(defFile, emptyList())
        val klib = libraryTestCase.cinteropToLibrary().assertSuccess().resultingArtifact

        val module = TestModule.Exclusive("userSetupHint", emptySet(), emptySet(), emptySet()).apply {
            files += TestFile.createCommitted(testDir.resolve("userSetupHint.kt"), this)
        }

        val compilationResult = compileToExecutable(module, listOf(klib.asLibraryDependency()))

        assertTrue(compilationResult is TestCompilationResult.Failure, "Compilation is expected to fail with linkage errors")

        assertContains(compilationResult.loggedData.toString(), "<<HINT>>", false, "Error output should contain expected hint")
    }
}
