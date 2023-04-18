/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.psi.KtContractEffect
import org.jetbrains.kotlin.psi.stubs.KotlinContractEffectStub
import org.jetbrains.kotlin.psi.stubs.elements.KtContractEffectElementType
import org.jetbrains.kotlin.psi.stubs.elements.KtUserTypeElementType.deserializeType
import org.jetbrains.kotlin.psi.stubs.elements.KtUserTypeElementType.serializeType

class KotlinContractEffectStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: KtContractEffectElementType
) : KotlinPlaceHolderStubImpl<KtContractEffect>(parent, elementType), KotlinContractEffectStub

enum class KotlinContractEffectType {
    CALLS {
        override fun deserialize(dataStream: StubInputStream): KotlinCallsEffectDeclaration {
            val declaration = PARAMETER_REFERENCE.deserialize(dataStream)
            val range = EventOccurrencesRange.values()[dataStream.readInt()]
            return KotlinCallsEffectDeclaration(declaration as KotlinContractValueParameterReference, range)
        }
    },
    RETURNS {
        override fun deserialize(dataStream: StubInputStream): KotlinContractDescriptionElement {
            return KotlinContractReturnsEffectDeclaration(CONSTANT.deserialize(dataStream) as KotlinContractConstantReference)
        }
    },
    CONDITIONAL {
        override fun deserialize(dataStream: StubInputStream): KotlinContractDescriptionElement {
            val descriptionElement = values()[dataStream.readInt()].deserialize(dataStream)
            val condition = values()[dataStream.readInt()].deserialize(dataStream)
            return KotlinContractConditionalEffectDeclaration(
                descriptionElement as KotlinContractEffectDeclaration,
                condition as KotlinContractExpression
            )
        }
    },
    IS_NULL {
        override fun deserialize(dataStream: StubInputStream): KotlinContractDescriptionElement {
            return KotlinContractIsNullPredicate(
                PARAMETER_REFERENCE.deserialize(dataStream) as KotlinContractValueParameterReference,
                dataStream.readBoolean()
            )
        }
    },
    IS_INSTANCE {
        override fun deserialize(dataStream: StubInputStream): KotlinContractDescriptionElement {
            return KotlinContractIsInstancePredicate(
                PARAMETER_REFERENCE.deserialize(dataStream) as KotlinContractValueParameterReference,
                deserializeType(dataStream)!!,
                dataStream.readBoolean()
            )
        }
    },
    NOT {
        override fun deserialize(dataStream: StubInputStream): KotlinContractDescriptionElement {
            return KotlinContractLogicalNot(values()[dataStream.readInt()].deserialize(dataStream) as KotlinContractExpression)
        }
    },
    BOOLEAN_LOGIC {
        override fun deserialize(dataStream: StubInputStream): KotlinContractDescriptionElement {
            val count = dataStream.readInt()
            val args = mutableListOf<KotlinContractExpression>()
            repeat((0 until count).count()) {
                args.add(values()[dataStream.readInt()].deserialize(dataStream) as KotlinContractExpression)
            }
            return KotlinContractBooleanExpression(args, dataStream.readBoolean())
        }
    },
    PARAMETER_REFERENCE {
        override fun deserialize(dataStream: StubInputStream): KotlinContractValueParameterReference {
            return KotlinContractValueParameterReference(dataStream.readInt())
        }
    },
    CONSTANT {
        override fun deserialize(dataStream: StubInputStream): KotlinContractDescriptionElement {
            return when (val nameString = dataStream.readNameString()!!) {
                "TRUE" -> KotlinContractConstantReference.TRUE
                "FALSE" -> KotlinContractConstantReference.FALSE
                "NULL" -> KotlinContractConstantReference.NULL
                "NOT_NULL" -> KotlinContractConstantReference.NOT_NULL
                "WILDCARD" -> KotlinContractConstantReference.WILDCARD
                else -> error("Unexpected $nameString")
            }
        }
    };

    abstract fun deserialize(dataStream: StubInputStream): KotlinContractDescriptionElement
}

interface KotlinContractDescriptionElement {
    fun serializeTo(dataStream: StubOutputStream)

    var effectType: KotlinContractEffectType
}

interface KotlinContractEffectDeclaration : KotlinContractDescriptionElement
data class KotlinCallsEffectDeclaration(
    val valueParameterReference: KotlinContractValueParameterReference,
    val kind: EventOccurrencesRange
) : KotlinContractEffectDeclaration {
    override fun serializeTo(dataStream: StubOutputStream) {
        valueParameterReference.serializeTo(dataStream)
        dataStream.writeInt(kind.ordinal)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.CALLS
}

data class KotlinContractReturnsEffectDeclaration(val value: KotlinContractConstantReference) : KotlinContractEffectDeclaration {
    override fun serializeTo(dataStream: StubOutputStream) {
        value.serializeTo(dataStream)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.RETURNS
}

data class KotlinContractConditionalEffectDeclaration(
    val effect: KotlinContractEffectDeclaration,
    val condition: KotlinContractExpression
) : KotlinContractEffectDeclaration {
    override fun serializeTo(dataStream: StubOutputStream) {
        dataStream.writeInt(effect.effectType.ordinal)
        effect.serializeTo(dataStream)
        dataStream.writeInt(condition.effectType.ordinal)
        condition.serializeTo(dataStream)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.CONDITIONAL
}


interface KotlinContractExpression : KotlinContractDescriptionElement

data class KotlinContractIsNullPredicate(val arg: KotlinContractValueParameterReference, val isNegated: Boolean) :
    KotlinContractExpression {
    override fun serializeTo(dataStream: StubOutputStream) {
        arg.serializeTo(dataStream)
        dataStream.writeBoolean(isNegated)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.IS_NULL
}

data class KotlinContractIsInstancePredicate(
    val arg: KotlinContractValueParameterReference,
    val type: KotlinTypeBean,
    val isNegated: Boolean
) :
    KotlinContractExpression {
    override fun serializeTo(dataStream: StubOutputStream) {
        arg.serializeTo(dataStream)
        serializeType(dataStream, type)
        dataStream.writeBoolean(isNegated)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.IS_INSTANCE
}

data class KotlinContractLogicalNot(val arg: KotlinContractExpression) : KotlinContractExpression {
    override fun serializeTo(dataStream: StubOutputStream) {
        dataStream.writeInt(arg.effectType.ordinal)
        arg.serializeTo(dataStream)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.NOT
}

data class KotlinContractBooleanExpression(
    val args: List<KotlinContractExpression>, val andKind: Boolean
) : KotlinContractExpression {
    override fun serializeTo(dataStream: StubOutputStream) {
        dataStream.writeInt(args.size)
        for (arg in args) {
            dataStream.writeInt(arg.effectType.ordinal)
            arg.serializeTo(dataStream)
        }
        dataStream.writeBoolean(andKind)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.BOOLEAN_LOGIC
}

interface KotlinContractDescriptionValue : KotlinContractExpression

data class KotlinContractValueParameterReference(val paramIdx: Int) : KotlinContractDescriptionValue {
    override fun serializeTo(dataStream: StubOutputStream) {
        dataStream.writeInt(paramIdx)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.PARAMETER_REFERENCE
}

data class KotlinContractConstantReference(val name: String) : KotlinContractDescriptionValue {
    companion object {
        val NULL = KotlinContractConstantReference("NULL")
        val WILDCARD = KotlinContractConstantReference("WILDCARD")
        val NOT_NULL = KotlinContractConstantReference("NOT_NULL")

        val TRUE = KotlinContractConstantReference("TRUE")
        val FALSE = KotlinContractConstantReference("FALSE")
    }

    override fun serializeTo(dataStream: StubOutputStream) {
        dataStream.writeName(name)
    }

    override var effectType: KotlinContractEffectType = KotlinContractEffectType.CONSTANT
}