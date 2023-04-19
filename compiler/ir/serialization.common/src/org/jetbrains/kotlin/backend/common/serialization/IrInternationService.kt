/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.Name
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet

interface IrInternationService {
    fun string(string: String): String {
        return string
    }

    fun name(name: Name): Name {
        return name
    }

    fun simpleType(type: IrSimpleType): IrSimpleType {
        return type
    }

    fun clear() {}
}

class DefaultIrInternationService : IrInternationService {
    private val strings by lazy { ObjectOpenHashSet<String>() }
    private val names by lazy { ObjectOpenHashSet<Name>() }
    private val simpleTypes by lazy { Object2ObjectOpenHashMap<Pair<IdSignature, SimpleTypeNullability>, IrSimpleType>() }

    override fun string(string: String): String {
        return strings.addOrGet(string)
    }

    override fun name(name: Name): Name {
        return names.addOrGet(name)
    }

    override fun simpleType(type: IrSimpleType): IrSimpleType {
        val signature = type.classifier.signature
        if (
            signature != null &&
            type.arguments.isEmpty() &&
            type.annotations.isEmpty() && type.abbreviation == null
        ) {
            return simpleTypes.computeIfAbsent(signature to type.nullability) { type }
        }
        return super.simpleType(type)
    }

    override fun clear() {
        strings.clear()
        names.clear()
        simpleTypes.clear()
    }
}