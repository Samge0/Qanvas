package com.samge.qanvas.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Moves model files between directories (default ⇄ user-selected).
 * Renames within the same volume are instant; cross-volume falls back to
 * copy+delete with progress callbacks.
 */
object ModelMigrator {

    suspend fun move(context: android.content.Context, from: File, to: File, onProgress: (Int) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (!from.isDirectory) return@withContext false
                if (!to.isDirectory && !to.mkdirs()) return@withContext false
                val files = from.walkTopDown().filter { it.isFile }.toList()
                val total = files.sumOf { it.length() }.coerceAtLeast(1L)
                var done = 0L
                for (f in files) {
                    val dst = File(to, f.relativeTo(from).path)
                    dst.parentFile?.mkdirs()
                    if (!f.renameTo(dst)) {
                        f.copyTo(dst, overwrite = true)
                        f.delete()
                    }
                    done += f.length()
                    onProgress((100L * done / total).toInt())
                }
                // clean now-empty source dirs (deepest first)
                from.walkTopDown().filter { it.isDirectory }
                    .sortedByDescending { it.path.length }
                    .forEach { d -> if (d.listFiles()?.isEmpty() == true) d.delete() }
                true
            } catch (e: Exception) {
                false
            }
        }
}
