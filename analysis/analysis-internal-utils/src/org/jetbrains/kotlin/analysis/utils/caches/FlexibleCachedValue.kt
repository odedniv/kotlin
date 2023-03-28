/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.org.jetbrains.kotlin.analysis.utils.caches

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.ModificationTracker
import com.intellij.reference.SoftReference

/**
 * TODO (marco): Document.
 */
public class FlexibleCachedValue<T : Any>(private val compute: () -> Pair<T, ModificationTracker>) {
    @Volatile
    private var softReference: SoftReference<T?> = SoftReference(null)
    private var hardReference: T? = null

    @Volatile
    private var dependency: ModificationTracker? = null

    @Volatile
    private var timestamp: Long? = null

    public val value: T
        get() {
            val value: T? = softReference.get()
            if (value != null && isUpToDate()) {
                // Because `timestamp` isn't synchronized in `isUpToDate`, we have to keep the following scenario in mind: Thread A finds
                // that the value is outdated via `isUpToDate`, thread B gets an outdated `softReference.get()` and stores it in `value`,
                // thread A enters the synchronized block below and updates `timestamp`, thread B calculates `isUpToDate` with the new
                // timestamp, which results in `true`, but thread B returns the old `value` it first got from the old `softReference`. We
                // can avoid this scenario by checking that the `softReference` after `isUpToDate` is still referentially equal to the
                // initially fetched value.
                if (softReference.get() === value) {
                    // TODO (marco): Harden the cached value because it's clearly still used? The point of softening the cached value is
                    //               that unused values can be reclaimed by the GC.
//                  hardReference = value

                    // While `isUpToDate` is checked, `softReference` cannot be garbage collected because it's referenced on the stack, so
                    // `value` should still be valid at this point.
                    return value
                }
            }

            // The synchronization guarantees that at any point in time, `value` does not return an old value while a new value is being
            // computed, because a new value is only computed if `isUpToDate` is `false`, and `isUpToDate` stays `false` until `timestamps`
            // has been updated, so a competing thread won't return `result` from the fast path above. The synchronization also guarantees
            // that a value is only computed once after invalidation.
            return synchronized(this) {
                // We have to check `isUpToDate` again, because another thread might have already computed the new value, and the current
                // thread might have subsequently acquired the lock and just needs to return the up-to-date value.
                var result = softReference.get()
                if (result == null || !isUpToDate()) {
                    if (result != null) {
                        LOG.warn("Flexible cached value `$result` is out of date!")
                    }
                    val (computedValue, computedDependency) = compute()
                    result = computedValue
                    hardReference = result
                    softReference = SoftReference(result)
                    dependency = computedDependency
                    timestamp = computedDependency.modificationCount
                }

                // The code above sets `hardReference` to `value` before `softReference` is overwritten, and `hardReference` cannot have
                // been softened at this point due to the synchronization, so the GC cannot have collected `softReference` yet and `result`
                // is valid.
                result
            }
        }

    /**
     * TODO (marco): Document.
     */
    public fun soften() = synchronized(this) {
        hardReference = null
    }

    private fun isUpToDate(): Boolean {
        val dependency = this.dependency ?: return false
        val timestamp = this.timestamp ?: return false
        return dependency.modificationCount == timestamp
    }

    companion object {
        private val LOG: Logger = Logger.getInstance(FlexibleCachedValue::class.java)
    }
}
