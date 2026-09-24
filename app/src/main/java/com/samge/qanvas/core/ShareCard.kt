package com.samge.qanvas.core

import android.util.Base64
import com.samge.qanvas.data.GenRecord
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Qanvas share cards: a generation's parameters packed into a short tagged block
 * and appended to the shared text (Pinduoduo-style "paste to reproduce").
 *
 * Format:  ⟪Q1:<urlsafe-b64(AES(payload))>⟫
 * payload: prompt|mode|w|h|steps|seed|ratio|tier   ("|" = 0x1F unit separator)
 * Key derivation: fixed app prefix + "share" — obfuscation, not security
 * (the card must decode on any install).
 */
object ShareCard {

    const val TAG_START = "⟪Q1:"
    const val TAG_END = "⟫"
    private const val KEY_MATERIAL = "qanvas-share-card-v1"

    private fun key(): SecretKeySpec {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(KEY_MATERIAL.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(bytes, "AES")
    }

    fun encode(rec: GenRecord): String {
        val payload = listOf(
            rec.prompt,
            rec.mode,
            rec.width, rec.height,
            rec.steps, rec.seed,
            rec.ratio, rec.tier,
        ).joinToString("\u001F")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val enc = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        val all = iv + enc
        return TAG_START + Base64.encodeToString(all, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING) + TAG_END
    }

    /** Extracts and decodes the first share card found in [text]; null if none valid. */
    fun decode(text: String): GenRecord? {
        val start = text.indexOf(TAG_START)
        val end = text.indexOf(TAG_END, start)
        if (start < 0 || end < 0 || end <= start + TAG_START.length) return null
        return try {
            val b64 = text.substring(start + TAG_START.length, end)
            val all = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key(), IvParameterSpec(all.copyOfRange(0, 16)))
            val payload = String(cipher.doFinal(all.copyOfRange(16, all.size)), Charsets.UTF_8)
            val p = payload.split("\u001F")
            if (p.size < 8) return null
            GenRecord(
                prompt = p[0],
                outPath = "",
                mode = p[1],
                width = p[2].toIntOrNull() ?: 0,
                height = p[3].toIntOrNull() ?: 0,
                steps = p[4].toIntOrNull() ?: 20,
                seed = p[5].toLongOrNull() ?: 0L,
                ratio = p[6].toIntOrNull() ?: 0,
                tier = p[7].toIntOrNull() ?: 1,
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Full share text: catchy share-phrase + parameter card. */
    fun shareText(rec: GenRecord): String {
        val header = "✨ Created with Qanvas — a 7B image model running fully offline on a phone.\n"
        val params = "${rec.width}×${rec.height} · ${rec.steps} steps · seed ${rec.seed}\n"
        val footer = "\n📥 Install Qanvas, paste this text on first launch to reproduce it in one tap."
        return header + params + encode(rec) + footer
    }
}
