/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolve.extensions

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Represents the Kotlin file which will participate in the Kotlin resolution
 * and extend the resolution by the declarations contained inside.
 *
 * All member implementations should consider caching the results for subsequent invocations.
 */
abstract class KtResolveExtensionFile {
    /**
     * The name a Kotlin file which will be generated.
     *
     * Should have the `.kt` extension.
     *
     * It will be used as a Java facade name, e.g., for the file name `myFile.kt`, the `MyFileKt` facade is generated if the file contains some properties or functions.
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    abstract fun getFileName(): String

    /**
     * [FqName] of the package specified in the file
     *
     * The operation might be called regularly, so the [getFilePackageName] should work fast and avoid building the whole file text.
     *
     * It hould be equal to the package name specified in the [buildFileText].
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    abstract fun getFilePackageName(): FqName

    /**
     * Returns the set of top-level classifier (classes, interfaces, objects, and type-aliases) names in the file.
     *
     * The result may have false-positive entries but cannot have false-negative entries. It should contain all the names in the package but may have some additional names that are not there.
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    abstract fun getTopLevelClassifierNames(): Set<Name>

    /**
     * Returns the set of top-level callable (functions and properties) names in the file.
     *
     * The result may have false-positive entries but cannot have false-negative entries. It should contain all the names in the package but may have some additional names that are not there.
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    abstract fun getTopLevelCallableNames(): Set<Name>

    /**
     * Checks if the file may contain a top-level classifier (class, interface, object, or type-alias) with the given [name].
     *
     * The result may be a false-positive result but cannot be a false-negative.
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    abstract fun mayHaveTopLevelClassifier(name: Name): Boolean

    /**
     * Checks if the file may contain a top-level callable (function or property) with the [name].
     *
     * The result may be a false-positive result but cannot be a false-negative.
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     *
     */
    abstract fun mayHaveTopLevelCallable(name: Name): Boolean

    /**
     * Creates the [KtResolveExtension] Kotlin source file text.
     *
     * The resulted String should be a valid Kotlin code.
     * It should be consistent with other declarations which are present in the [KtResolveExtensionFile], more specifically:
     * 1. [getFilePackageName] should be equal to the file's package name.
     * 2. All classifier names should be contained in the [getTopLevelClassifierNames].
     * 3. All callable names should be contained in the [getTopLevelCallableNames].
     * 4. [mayHaveTopLevelClassifier] should return `true` for any classifier in this file.
     * 5. [mayHaveTopLevelCallable] should return `true` for any callable in this file.
     *
     * Additional restrictions on the file text:
     * 1. The File should not contain the `kotlin.jvm.JvmMultifileClass` and `kotlin.jvm.JvmName` annotations on the file level.
     * 2. All declaration types should be specified explicitly.
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    abstract fun buildFileText(): String

    /**
     * Creates a [KtResolveExtensionReferenceTargetsPsiProvider] for this [KtResolveExtensionFile].
     *
     * @see KtResolveExtensionReferenceTargetsPsiProvider
     *
     * Implementation cannot use the Kotlin resolve inside, as this function is called during session initialization, so Analysis API access is forbidden.
     */
    abstract fun createTargetPsiProvider(): KtResolveExtensionReferenceTargetsPsiProvider

}