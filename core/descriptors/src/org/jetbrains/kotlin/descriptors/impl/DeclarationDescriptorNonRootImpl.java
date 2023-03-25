/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.descriptors.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.name.Name;

import java.util.ArrayList;
import java.util.List;

public abstract class DeclarationDescriptorNonRootImpl
        extends DeclarationDescriptorImpl
        implements DeclarationDescriptorNonRoot {

    @NotNull
    private final DeclarationDescriptor containingDeclaration;

    @NotNull
    private final SourceElement source;

    private List<Runnable> postInitActions;

    private boolean initialized;

    protected DeclarationDescriptorNonRootImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull SourceElement source
    ) {
        this(containingDeclaration, annotations, name, source, false);
    }

    protected DeclarationDescriptorNonRootImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull SourceElement source,
            boolean initialized
    ) {
        super(annotations, name);

        this.containingDeclaration = containingDeclaration;
        this.source = source;
        this.initialized = initialized;
    }


    @NotNull
    @Override
    public DeclarationDescriptorWithSource getOriginal() {
        return (DeclarationDescriptorWithSource) super.getOriginal();
    }

    @Override
    @NotNull
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @Override
    @NotNull
    public SourceElement getSource() {
        return source;
    }

    public final void initialize() {
        if (initialized && allowReInitialization()) {
            if (postInitActions != null && !postInitActions.isEmpty()) {
                throw new IllegalStateException();
            }
            return;
        }

        checkUninitialized();
        initialized = true;
        if (postInitActions != null) {
            for (Runnable action : postInitActions) {
                action.run();
            }
            postInitActions.clear();
        }
    }

    @Override
    public void addPostInitAction(Runnable action) {
        if (initialized) {
            action.run();
        } else {
            if (postInitActions == null) {
                postInitActions = new ArrayList<>();
            }
            postInitActions.add(action);
        }
    }

    protected boolean allowReInitialization() {
        return false;
    }

    protected void checkUninitialized() {
        if (initialized) {
            throw new IllegalStateException(initializedMessage());
        }
    }

    protected void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(uninitializedMessage());
        }
    }

    protected String initializedMessage() {
        return this + " descriptor is already initialized";
    }

    protected String uninitializedMessage() {
        return this + " descriptor is not initialized";
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

}
