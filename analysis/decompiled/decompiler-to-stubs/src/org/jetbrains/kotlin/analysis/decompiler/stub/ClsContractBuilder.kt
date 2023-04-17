/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.Flags
import org.jetbrains.kotlin.metadata.deserialization.TypeTable
import org.jetbrains.kotlin.metadata.deserialization.isInstanceType
import org.jetbrains.kotlin.psi.stubs.impl.*
import org.jetbrains.kotlin.utils.addIfNotNull

class ClsContractBuilder(private val typeTable: TypeTable, private val typeStubBuilder: TypeClsStubBuilder) {
    fun loadContract(proto: ProtoBuf.Contract): List<KotlinContractEffect>? {
        return proto.effectList.map { loadPossiblyConditionalEffect(it) ?: return null }
    }

    private fun loadPossiblyConditionalEffect(
        proto: ProtoBuf.Effect
    ): KotlinContractEffect? {
        if (proto.hasConclusionOfConditionalEffect()) {
            val conclusion = loadExpression(proto.conclusionOfConditionalEffect) ?: return null
            val effect = loadSimpleEffect(proto) ?: return null
            return effect.copy(conclusion = conclusion)
        }
        return loadSimpleEffect(proto)
    }

    private fun loadSimpleEffect(proto: ProtoBuf.Effect): KotlinContractEffect? {
        val type: ProtoBuf.Effect.EffectType = if (proto.hasEffectType()) proto.effectType else return null
        return when(type) {
            ProtoBuf.Effect.EffectType.RETURNS_CONSTANT -> {
                val argument = proto.effectConstructorArgumentList.firstOrNull()
                val returnValue = if (argument == null) {
                    null
                } else {
                    loadExpression(argument) ?: return null
                }
                KotlinContractEffect(KotlinContractEffectType.RETURNS_CONSTANT, listOfNotNull(returnValue),
                                     conclusion = null,
                                     invocationKind = null
                )
            }
            ProtoBuf.Effect.EffectType.RETURNS_NOT_NULL -> {
                KotlinContractEffect(KotlinContractEffectType.RETURNS_NOT_NULL, arguments = null, conclusion = null, invocationKind = null)
            }
            ProtoBuf.Effect.EffectType.CALLS -> {
                val argument = proto.effectConstructorArgumentList.firstOrNull() ?: return null
                val callable = extractVariable(argument) ?: return null
                val invocationKind = if (proto.hasKind())
                    proto.kind.toDescriptorInvocationKind()
                else
                    return null
                KotlinContractEffect(KotlinContractEffectType.CALLS, listOf(callable), conclusion = null, invocationKind)
            }
        }
    }

    private fun loadExpression(proto: ProtoBuf.Expression): KotlinContractExpression? {
        val primitiveType = getPrimitiveType(proto)
        val primitiveExpression = extractPrimitiveExpression(proto, primitiveType)

        val complexType = getComplexType(proto)
        val childs: MutableList<KotlinContractExpression> = mutableListOf()
        childs.addIfNotNull(primitiveExpression)

        return when (complexType) {
            ComplexExpressionType.AND_SEQUENCE -> {
                KotlinContractExpression(
                    isNegated = false,
                    isInNullPredicate = false,
                    valueParameter = null,
                    type = null,
                    constantValue = null,
                    andArgs = proto.andArgumentList.mapTo(childs) { loadExpression(it) ?: return null },
                    orArgs = null
                )
            }

            ComplexExpressionType.OR_SEQUENCE -> {
                KotlinContractExpression(
                    isNegated = false,
                    isInNullPredicate = false,
                    valueParameter = null,
                    type = null,
                    constantValue = null,
                    andArgs = null,
                    orArgs = proto.orArgumentList.mapTo(childs) { loadExpression(it) ?: return null },
                )
            }

            null -> primitiveExpression
        }
    }

    private fun extractPrimitiveExpression(proto: ProtoBuf.Expression, primitiveType: PrimitiveExpressionType?): KotlinContractExpression? {
        val isInverted = Flags.IS_NEGATED.get(proto.flags)

        return when (primitiveType) {
            PrimitiveExpressionType.VALUE_PARAMETER_REFERENCE, PrimitiveExpressionType.RECEIVER_REFERENCE -> {
                extractVariable(proto)?.copy(isNegated = isInverted)
            }

            PrimitiveExpressionType.CONSTANT ->
                KotlinContractExpression(
                    isNegated = isInverted,
                    isInNullPredicate = false,
                    valueParameter = null,
                    type = null,
                    constantValue = loadConstant(proto.constantValue),
                    andArgs = null,
                    orArgs = null
                )

            PrimitiveExpressionType.INSTANCE_CHECK -> {
                val variable = extractVariable(proto) ?: return null
                val type = extractType(proto) ?: return null
                variable.copy(isNegated = isInverted, type = type)
            }

            PrimitiveExpressionType.NULLABILITY_CHECK -> {
                val variable = extractVariable(proto) ?: return null
                variable.copy(isNegated = isInverted, isInNullPredicate = true)
            }

            null -> null
        }
    }

    private fun extractVariable(proto: ProtoBuf.Expression): KotlinContractExpression? {
        if (!proto.hasValueParameterReference()) return null

        val valueParameterIndex = proto.valueParameterReference - 1
        return KotlinContractExpression(
            isNegated = false,
            isInNullPredicate = false,
            valueParameter = valueParameterIndex,
            type = null,
            constantValue = null,
            andArgs = null,
            orArgs = null
        )
    }

    private fun ProtoBuf.Effect.InvocationKind.toDescriptorInvocationKind(): EventOccurrencesRange = when (this) {
        ProtoBuf.Effect.InvocationKind.AT_MOST_ONCE -> EventOccurrencesRange.AT_MOST_ONCE
        ProtoBuf.Effect.InvocationKind.EXACTLY_ONCE -> EventOccurrencesRange.EXACTLY_ONCE
        ProtoBuf.Effect.InvocationKind.AT_LEAST_ONCE -> EventOccurrencesRange.AT_LEAST_ONCE
    }

    private fun extractType(proto: ProtoBuf.Expression): KotlinTypeBean? {
        return typeStubBuilder.createKotlinTypeBean(proto.isInstanceType(typeTable))
    }

    private fun loadConstant(value: ProtoBuf.Expression.ConstantValue): KotlinContractConstantValue = when (value) {
        ProtoBuf.Expression.ConstantValue.TRUE -> KotlinContractConstantValue.TRUE
        ProtoBuf.Expression.ConstantValue.FALSE -> KotlinContractConstantValue.FALSE
        ProtoBuf.Expression.ConstantValue.NULL -> KotlinContractConstantValue.NULL
    }


    private fun getComplexType(proto: ProtoBuf.Expression): ComplexExpressionType? {
        val isOrSequence = proto.orArgumentCount != 0
        val isAndSequence = proto.andArgumentCount != 0
        return when {
            isOrSequence && isAndSequence -> null
            isOrSequence -> ComplexExpressionType.OR_SEQUENCE
            isAndSequence -> ComplexExpressionType.AND_SEQUENCE
            else -> null
        }
    }

    private fun getPrimitiveType(proto: ProtoBuf.Expression): PrimitiveExpressionType? {
        // Expected to be one element, but can be empty (unknown expression) or contain several elements (invalid data)
        val expressionTypes: MutableList<PrimitiveExpressionType> = mutableListOf()

        // Check for predicates
        when {
            proto.hasValueParameterReference() && proto.hasType() ->
                expressionTypes.add(PrimitiveExpressionType.INSTANCE_CHECK)

            proto.hasValueParameterReference() && Flags.IS_NULL_CHECK_PREDICATE.get(proto.flags) ->
                expressionTypes.add(PrimitiveExpressionType.NULLABILITY_CHECK)
        }

        // If message contains correct predicate, then predicate's type overrides type of value,
        // even is message has one
        if (expressionTypes.isNotEmpty()) {
            return expressionTypes.singleOrNull()
        }

        // Otherwise, check if it is a value
        when {
            proto.hasValueParameterReference() && proto.valueParameterReference > 0 ->
                expressionTypes.add(PrimitiveExpressionType.VALUE_PARAMETER_REFERENCE)

            proto.hasValueParameterReference() && proto.valueParameterReference == 0 ->
                expressionTypes.add(PrimitiveExpressionType.RECEIVER_REFERENCE)

            proto.hasConstantValue() -> expressionTypes.add(PrimitiveExpressionType.CONSTANT)
        }

        return expressionTypes.singleOrNull()
    }

    private fun ProtoBuf.Expression.hasType(): Boolean = this.hasIsInstanceType() || this.hasIsInstanceTypeId()

    // Arguments of expressions with such types are never other expressions
    private enum class PrimitiveExpressionType {
        VALUE_PARAMETER_REFERENCE,
        RECEIVER_REFERENCE,
        CONSTANT,
        INSTANCE_CHECK,
        NULLABILITY_CHECK
    }

    // Expressions with such type can take other expressions as arguments.
    // Additionally, for performance reasons, "complex expression" and "primitive expression"
    // can co-exist in the one and the same message. If "primitive expression" is present
    // in the current message, it is treated as the first argument of "complex expression".
    private enum class ComplexExpressionType {
        AND_SEQUENCE,
        OR_SEQUENCE

    }
}