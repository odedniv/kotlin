/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.superTypes
import org.jetbrains.kotlin.ir.util.isClass
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

internal val addSuperQualifierToJavaFieldAccessPhase = makeIrFilePhase(
    { context ->
        if (context.state.configuration.getBoolean(CommonConfigurationKeys.USE_FIR)) {
            AddSuperQualifierToJavaFieldAccessLowering
        } else {
            FileLoweringPass.Empty
        }
    },
    name = "AddSuperQualifierToJavaFieldAccess",
    description = "Make `\$delegate` methods for optimized delegated properties static",
    // Property references need to be lowered to classes with field access inside. We can't perform this phase on unlowered property
    // references as IrPropertyReference doesn't have superQualifierSymbol.
    prerequisite = setOf(propertyReferencePhase),
)

// This lowering changes field accesses so that codegen will correctly generate access to a public field in a Java superclass of a Kotlin
// class, even if that field is "shadowed" by a property in the Kotlin class, with a _private_ field with the same name.
// See KT-49507 and KT-48954 as good examples for cases we try to handle here.
private object AddSuperQualifierToJavaFieldAccessLowering : IrElementVisitorVoid, FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(this, null)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildren(this, null)
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        val dispatchReceiver = expression.receiver
        if (dispatchReceiver != null) {
            expression.superQualifierSymbol = superQualifierSymbolForField(dispatchReceiver, expression.symbol)
        }
        super.visitFieldAccess(expression)
    }

    // Note that we're abusing superQualifierSymbol to make codegen generate the correct field owner in the bytecode.
    // However, currently there seems to be no better way to support it.
    private fun superQualifierSymbolForField(dispatchReceiver: IrExpression, fieldSymbol: IrFieldSymbol): IrClassSymbol? {
        if (fieldSymbol.owner.correspondingPropertySymbol != null) return null
        val originalContainingClass = fieldSymbol.owner.parentClassOrNull ?: return null
        val dispatchReceiverIrType = dispatchReceiver.type
        val dispatchReceiverRepresentativeClassifierSymbol = dispatchReceiverIrType.classifierOrNull ?: return null
        // Find first Java super class to avoid possible visibility exposure & separate compilation problems
        return getJavaFieldContainingClassSymbol(dispatchReceiverRepresentativeClassifierSymbol, originalContainingClass.symbol)
    }

    // Note: ownContainingClass here is the use-site receiver class,
    // and originalContainingClass is the class which contains Java field we are trying to access
    // ! Interfaces are out of our interests here !
    // This function returns a class symbol which:
    // - is the most derived Java class in hierarchy which has no Kotlin base classes (including transitive ones)
    // E.g. K2 <: J3 <: K1 <: J2 <: J1 ==> J2 is chosen
    // We shouldn't allow base Kotlin classes to avoid possible clashes with invisible properties inside
    private fun getJavaFieldContainingClassSymbol(
        dispatchReceiverRepresentativeClassifierSymbol: IrClassifierSymbol,
        originalContainingClassSymbol: IrClassSymbol
    ): IrClassSymbol {
        val superTypesFromBase = generateSequence(dispatchReceiverRepresentativeClassifierSymbol) { classifierSymbol ->
            if (classifierSymbol == originalContainingClassSymbol) return@generateSequence null
            classifierSymbol.superTypes().firstNotNullOfOrNull { supertype ->
                val erasedUpperBound = supertype.erasedUpperBound
                erasedUpperBound.takeIf { it.isClass || it.isEnumClass }
            }?.symbol
        }
        return superTypesFromBase.filterIsInstance<IrClassSymbol>().findLast {
            it.owner.visibility == DescriptorVisibilities.PUBLIC
        } ?: originalContainingClassSymbol
    }
}
