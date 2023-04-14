#include "Barriers.hpp"

#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "GCImpl.hpp"
#include <atomic>

using namespace kotlin;

namespace {

[[clang::no_destroy]] std::atomic<bool> weakRefBarriersEnabled = false;

void checkpointAction(mm::ThreadData& thread) {
    thread.gc().impl().gc().barriers().onCheckpoint();
}

void waitForThreadsToCheckpoint() { // TODO rename
    // resetCheckpoint
    for (auto& thr: mm::ThreadRegistry::Instance().LockForIter()) {
        thr.gc().impl().gc().barriers().resetCheckpoint();
    }

    // requestCheckpoint
    bool safePointSet = mm::TrySetSafePointAction(checkpointAction);
    RuntimeAssert(safePointSet, "TODO we are the only GC here");

    // waitForAllThreadsToVisitCheckpoint
    auto threads = mm::ThreadRegistry::Instance().LockForIter();
    // TODO assert no suspended threads?
    while (!forall(threads, [](mm::ThreadData& thr) { return thr.gc().impl().gc().barriers().visitedCheckpoint() || thr.suspensionData().suspendedOrNative(); })) {
        std::this_thread::yield();
    }

    //unsetSafePointAction
    mm::UnsetSafePointAction();
}

}

void gc::BarriersThreadData::onCheckpoint() {
    visitedCheckpoint_.store(true, std::memory_order_seq_cst);
}

void gc::BarriersThreadData::resetCheckpoint() {
    visitedCheckpoint_.store(false, std::memory_order_seq_cst);
}

bool gc::BarriersThreadData::visitedCheckpoint() const {
    return visitedCheckpoint_.load(std::memory_order_relaxed); // TODO?
}

void gc::EnableWeakRefBarriers(bool inSTW) {
    // TODO maybe in STW vs concurrent
    weakRefBarriersEnabled.store(true, std::memory_order_seq_cst);
    if (!inSTW) {
        waitForThreadsToCheckpoint();
    }
}

void gc::DisableWeakRefBarriers(bool inSTW) {
    weakRefBarriersEnabled.store(false, std::memory_order_seq_cst);
    if (!inSTW) {
        waitForThreadsToCheckpoint();
    }
}

OBJ_GETTER(kotlin::gc::WeakRefRead, ObjHeader* weakReferee) noexcept {
    // TODO weakRefReadImpl only changes inside STW. Access is always synchronized.
    // When weak ref barriers are on, marked state cannot change and the
    // object cannot be deleted.
    if (compiler::concurrentExtraSweep()) {
        if (weakReferee != nullptr) {
            if (weakRefBarriersEnabled.load(std::memory_order_relaxed)) {
                if (!gc::isMarked(weakReferee)) {
                    RETURN_OBJ(nullptr);
                }
            }
        }
    }
    RETURN_OBJ(weakReferee);
}

