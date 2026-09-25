package com.samge.qanvas.core

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * App-level downloader for non-default download sources (hf-mirror.com).
 *
 * Deliberately mirrors the semantics of the AAR's ModelDownloader so the
 * on-disk artifacts are interchangeable:
 *  - same file list (QwenImage21.REQUIRED_FILES + EDIT_FILES via reflection —
 *    stays in sync with what the runtime validates at load time)
 *  - same .part partial-file + Range resume scheme
 *  - same .etag sidecar files (SHA-256, falling back to SHA-1), so a download
 *    started from one source resumes/validates identically on the other.
 *
 * The default huggingface.co path still uses the AAR downloader unchanged.
 */
class MirrorDownloader(private val host: String) {

    @Volatile
    private var cancelled = false

    fun cancel() { cancelled = true }

    private fun files(): Array<String> {
        val kc = Class.forName("com.scsonic.qwenimage21.QwenImage21")
        val req = kc.getDeclaredField("REQUIRED_FILES").get(null) as Array<String>
        val edit = kc.getDeclaredField("EDIT_FILES").get(null) as Array<String>
        return req + edit
    }

    private class Remote(var size: Long = 0, var etag: String = "")

    private fun open(rel: String): HttpURLConnection {
        val conn = URL(host + REPO_PATH + rel).openConnection() as HttpURLConnection
        conn.setRequestProperty("Accept-Encoding", "identity")
        conn.instanceFollowRedirects = true
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        return conn
    }

    /**
     * Authoritative metadata. hf-mirror's first hop may return a bare 308 with
     * NO X-Linked-* headers (HF always sends them), and the FINAL CDN response's
     * ETag is the xet OID — NOT sha256(content). So we walk the redirect chain
     * manually and take the first X-Linked-Etag seen on any hop (that is the
     * content hash the AAR validates against); fall back to the last response.
     */
    private fun remote(rel: String): Remote {
        var url: String = host + REPO_PATH + rel
        var hops = 0
        while (true) {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept-Encoding", "identity")
            conn.setRequestProperty("Range", "bytes=0-0")
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            try {
                val code = conn.responseCode
                if (code in 300..399) {
                    val next = conn.getHeaderField("Location") ?: throw IOException("redirect without Location for $rel")
                    val linkedEtag = conn.getHeaderField("X-Linked-Etag")
                    val linkedSize = conn.getHeaderField("X-Linked-Size")?.toLongOrNull()
                    // always resolve (Location may be relative)
                    url = URL(URL(url), next).toString()
                    if (linkedEtag != null && linkedSize != null) {
                        return Remote(linkedSize, normalizeEtag(linkedEtag))
                    }
                    if (++hops > 5) throw IOException("too many redirects for $rel")
                    continue
                }
                if (code != 200 && code != 206) throw IOException("HTTP $code for $rel")
                var etag = conn.getHeaderField("X-Linked-Etag") ?: conn.getHeaderField("ETag")
                    ?: throw IOException("no etag for $rel")
                val size = conn.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                    ?: conn.getHeaderField("Content-Length")?.toLongOrNull() ?: 0
                return Remote(size, normalizeEtag(etag))
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun normalizeEtag(e: String): String =
        e.replace("W/", "").replace("\"", "").trim().lowercase()

    fun download(dir: File, cb: (rel: String, done: Long, total: Long) -> Unit) {
        cancelled = false
        val files = files()
        val remotes = files.map { remote(it) }
        val total = remotes.sumOf { it.size }.coerceAtLeast(1)
        var done = 0L
        for ((i, rel) in files.withIndex()) {
            val r = remotes[i]
            val dst = File(dir, rel)
            val etagFile = File(dir.path + "/" + rel + ".etag")
            if (dst.isFile && dst.length() == r.size) {
                cb("verifying$rel", done, total)
                if (r.etag.isNotEmpty() && r.etag == (readEtag(etagFile) ?: "")) {
                    done += r.size
                    continue
                }
                if (matches(dst, r.etag)) {
                    writeEtag(etagFile, r.etag)
                    done += r.size
                    cb(rel, done, total)
                    continue
                }
            }
            etagFile.delete()
            dst.parentFile?.mkdirs()
            val part = File(dir.path + "/" + rel + ".part")
            var offset = if (part.isFile) part.length() else 0L
            if (offset > r.size) { part.delete(); offset = 0 }
            val conn = open(rel)
            if (offset > 0) conn.setRequestProperty("Range", "bytes=$offset-")
            val code = conn.responseCode
            if (code != 200 && code != 206) throw IOException("HTTP $code for $rel")
            val resumed = code == 206
            if (!resumed) offset = 0
            done += offset
            val input = conn.inputStream
            val out = FileOutputStream(part, resumed)
            val buf = ByteArray(1 shl 20)
            try {
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    if (cancelled) throw IOException("cancelled")
                    out.write(buf, 0, n)
                    done += n
                    cb(rel, done, total)
                }
            } finally {
                out.close(); input.close(); conn.disconnect()
            }
            if (!matches(part, r.etag)) {
                part.delete()
                throw IOException("checksum mismatch: $rel")
            }
            if (!part.renameTo(dst)) throw IOException("rename failed: $rel")
            writeEtag(etagFile, r.etag)
            cb(rel, done, total)
        }
    }

    companion object {
        const val REPO_PATH = "evankuo/Qwen-Image-2.1-MNN/resolve/main/"

        /** Mirrors the AAR's checksum semantics exactly:
         *  - 64-hex etag (LFS): plain SHA-256 of the file content
         *  - 40-hex etag (git blob): SHA-1 of "blob <size>\0" + content */
        private fun digestMatches(f: File, etag: String, algo: String): Boolean {
            val clean = etag.trim().trim('"')
            if (clean.length != if (algo == "SHA-256") 64 else 40) return false
            val md = MessageDigest.getInstance(algo)
            if (algo == "SHA-1") {
                // git blob header: "blob <size>" + NUL byte
                md.update("blob ${f.length()}".toByteArray(Charsets.US_ASCII))
                md.update(0)
            }
            f.inputStream().use { ins ->
                val buf = ByteArray(1 shl 20)
                while (true) {
                    val n = ins.read(buf)
                    if (n <= 0) break
                    md.update(buf, 0, n)
                }
            }
            return clean.equals(buildString { for (b in md.digest()) append("%02x".format(b)) }, ignoreCase = true)
        }

        fun matches(f: File, etag: String): Boolean {
            if (etag.isEmpty()) return f.length() > 0
            return digestMatches(f, etag, "SHA-256") || digestMatches(f, etag, "SHA-1")
        }

        fun readEtag(f: File): String? =
            if (f.isFile) f.readText().trim().ifEmpty { null } else null

        fun writeEtag(f: File, etag: String) {
            f.parentFile?.mkdirs()
            f.writeText(etag)
        }
    }
}
