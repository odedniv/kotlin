/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.caches

import com.intellij.util.containers.SLRUMap
import org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches.NullValue
import org.jetbrains.kotlin.analysis.low.level.api.fir.fir.caches.nullValueToNull

/**
 * A limited-size LRU (least recently used) cache with two segments, a probationary and a protected segment.
 *
 * Elements are first added to the probationary segment. If an element in the probationary segment is accessed again, it is added to the
 * protected segment. The protected segment thus contains elements which are accessed more frequently.
 *
 * If the probationary segment is full, adding an element to the cache will first remove the least recently used element in that segment.
 * If the protected segment is full, adding an element to it may force out the least recently used element there, which will be added to the
 * probationary segment again. (This operation can also force an element out of the probationary segment.)
 *
 * [ThreadSafeSlruCache] is backed by a map that is not thread safe, but achieves thread safety via synchronization.
 */
internal class ThreadSafeSlruCache<K, V>(protectedSegmentSize: Int, probationarySegmentSize: Int) {
    private val slruMap = SLRUMap<K, Any>(protectedSegmentSize, probationarySegmentSize)

    /**
     * Returns the value for the given [key]. If [V] is nullable, a return value of `null` might mean "value not in cache" or "null value in
     * cache". Should this be an issue, use [getAndApply].
     */
    fun get(key: K): V? = synchronized(slruMap) { slruMap.get(key)?.nullValueToNull() }

    /**
     * Applies [f] to the value of [key] if [key] is contained in the cache.
     *
     * This should be preferred over [get] if [V] is nullable and a "null value in cache" has some specific meaning.
     */
    inline fun getAndApply(key: K, f: (V) -> Unit) {
        val value = synchronized(slruMap) { slruMap.get(key) ?: return }
        f(value.nullValueToNull())
    }

    fun put(key: K, value: V) {
        synchronized(slruMap) {
            slruMap.put(key, value ?: NullValue)
        }
    }

    /**
     * Returns the value for the given [key] if it's contained in the cache, or computes the value with [computeValue] and adds it to the
     * cache.
     */
    fun getOrCompute(key: K, computeValue: () -> V): V {
        getAndApply(key) { return it }
        return computeValue().also { put(key, it) }
    }
}
