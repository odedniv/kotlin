/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MainQueueProcessor.hpp"

#include <atomic>

#include "KAssert.h"

#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
#include <dispatch/dispatch.h>
#endif

using namespace kotlin;

namespace {

#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
struct MainQueueData {
    std::atomic<bool> isBeingProcessed = false;
};

MainQueueData mainQueueData;
#endif

} // namespace

void kotlin::initializeMainQueueProcessor() noexcept {
#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
    dispatch_queue_set_specific(dispatch_get_main_queue(), &mainQueueData, &mainQueueData, nullptr);
    dispatch_async_f(
            dispatch_get_main_queue(), nullptr, [](void*) { mainQueueData.isBeingProcessed.store(true, std::memory_order_relaxed); });
#endif
}

bool kotlin::isMainQueueProcessorAvailable() noexcept {
#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
    return mainQueueData.isBeingProcessed.load(std::memory_order_relaxed);
#else
    return false;
#endif
}

bool kotlin::isOnMainQueue() noexcept {
#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
    return dispatch_get_specific(&mainQueueData) == &mainQueueData;
#else
    return false;
#endif
}

void kotlin::runOnMainQueue(void (*f)(void*), void* arg) noexcept {
    RuntimeAssert(isMainQueueProcessorAvailable(), "Running on main queue when it's not processed");
#if KONAN_SUPPORTS_GRAND_CENTRAL_DISPATCH
    dispatch_async_f(dispatch_get_main_queue(), arg, f);
#endif
}
