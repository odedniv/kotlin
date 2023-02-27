/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import java.nio.file.FileSystem

interface ZipFileSystemAccessor {
    fun <T> withZipFileSystem(zipFile: File, action: (FileSystem) -> T): T
}

object ZipFileSystemInPlaceAccessor : ZipFileSystemAccessor {
    override fun <T> withZipFileSystem(zipFile: File, action: (FileSystem) -> T): T {
        return zipFile.withZipFileSystem(action)
    }
}

class ZipFileSystemCacheableAccessor : ZipFileSystemAccessor {
    private val openFileSystems = hashMapOf<File, FileSystem>()

    override fun <T> withZipFileSystem(zipFile: File, action: (FileSystem) -> T): T {
        val fileSystem = openFileSystems.getOrPut(zipFile) { zipFile.zipFileSystem() }
        return action(fileSystem)
    }

    fun close() {
        val closingFileSystems = openFileSystems.values.toMutableList()
        closingFileSystems.clear()

        var lastException: Exception? = null
        while (true) {
            val fileSystem = closingFileSystems.removeLastOrNull() ?: break
            try {
                fileSystem.close()
            } catch (e: Exception) {
                lastException = e
            }
        }
        lastException?.let {
            throw it
        }
    }
}
