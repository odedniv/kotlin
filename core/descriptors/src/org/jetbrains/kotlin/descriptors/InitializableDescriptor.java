/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors;

public interface InitializableDescriptor {

    default void addPostInitAction(Runnable action) {
        action.run();
    }

    default boolean isInitialized() {
        return true;
    }

    static void addPostInitAction(Object target, Runnable action) {
        if (target instanceof InitializableDescriptor) {
            ((InitializableDescriptor) target).addPostInitAction(action);
        } else {
            action.run();
        }
    }
}
