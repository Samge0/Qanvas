package com.samge.qanvas.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Moves/copies model files between directories (default ⇄ user-selected).
 * Same-volume moves are instant renames; cross-volume falls back to copy.
 * mode: "move" (rename/copy+delete) or "copy" (copy, then clean the source).
 */
object ModelMigrator {

    suspend fun move(
        context: android.content.Context,
        from: File, to: File,
        copyThenClean: Boolean = false,
        onProgress: (Int) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!from.isDirectory) return@withContext false
            if (!to.isDirectory && !to.mkdirs()) return@withContext false
            val files = from.walkTopDown().filter { it.isFile }.toList()
            val total = files.sumOf { it.length() }.coerceAtLeast(1L)
            var done = 0L
            for (f in files) {
                val dst = File(to, f.relativeTo(from).path)
                dst.parentFile?.mkdirs()
                val renamed = !copyThenClean && f.renameTo(dst)
                if (!renamed) {
                    f.copyTo(dst, overwrite = true)
                    if (!copyThenClean) f.delete()
                }
                done += f.length()
                onProgress((100L * done / total).toInt())
            }
            if (copyThenClean) {
                // delete old files only after every file verified at the destination
                files.forEach { f ->
                    val dst = File(to, f.relativeTo(from).path)
                    if (dst.isFile && dst.length() == f.length()) f.delete()
                }
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
