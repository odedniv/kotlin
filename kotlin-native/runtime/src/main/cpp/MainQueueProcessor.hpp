/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#pragma once

namespace kotlin {

void initializeMainQueueProcessor() noexcept;

// Only valid after `initializeMainQueueProcessor`
bool isMainQueueProcessorAvailable() noexcept;

// Only valid after `initializeMainQueueProcessor`
bool isOnMainQueue() noexcept;

// Run `f(arg)` on main queue without waiting for its completion.
// Only valid after `initializeMainQueueProcessor` and if `isMainQueueProcessorAvailable` returns true.
void runOnMainQueue(void (*f)(void*), void* arg) noexcept;

} // namespace kotlin
