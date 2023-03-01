/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.extensions

import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.SerialInfoCodegenImpl
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.SerializableCodegenImpl
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.SerializableCompanionCodegenImpl
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.SerializerCodegenImpl

open class SerializationCodegenExtension : ExpressionCodegenExtension {
    override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
        SerialInfoCodegenImpl.generateSerialInfoImplBody(codegen)
        SerializableCodegenImpl.generateSerializableExtensions(codegen)
        SerializerCodegenImpl.generateSerializerExtensions(codegen)
        SerializableCompanionCodegenImpl.generateSerializableExtensions(codegen)
    }

    override val shouldGenerateClassSyntheticPartsInLightClassesMode: Boolean
        get() = false
}
