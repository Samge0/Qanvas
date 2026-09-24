package com.samge.qanvas.core

/**
 * Prompt inspiration library — categorized around the model's four flagship abilities.
 * The RGBA sticker template follows the official recommended format.
 */
object Inspo {
    enum class Kind { T2I, STICKER, EDIT, POSTER }

    data class Card(val kind: Kind, val title: String, val prompt: String, val hint: String = "")

    val all: List<Card> = listOf(
        // ---- text-to-image ----
        Card(
            Kind.T2I, "Neon café, rainy night",
            "A cozy coffee shop on a rainy evening, warm light, a neon sign that reads \"QWEN\", reflections on wet pavement",
        ),
        Card(
            Kind.T2I, "Fishing at dawn",
            "An old fisherman casting a net from a wooden boat on a misty lake at dawn, soft golden light, photorealistic",
        ),
        Card(
            Kind.T2I, "Cliffside lighthouse",
            "A lighthouse on a stormy cliff, dramatic clouds, beam of light cutting through rain, cinematic",
        ),
        Card(
            Kind.T2I, "Mecha bonsai",
            "A tiny mechanical bonsai tree made of brass gears and copper wires on a scholar's desk, macro photography",
        ),
        Card(
            Kind.T2I, "Cyberpunk street food",
            "A night market noodle stall in a neon cyberpunk alley, steam rising, holographic menu signs, rain",
        ),
        // ---- RGBA stickers (official prompt format) ----
        Card(
            Kind.STICKER, "Cute dragon sticker",
            "This is an RGBA image with transparency. A cute cartoon dragon sticker. The image has alpha channel and the background is transparent.",
            "官方推荐透明图格式",
        ),
        Card(
            Kind.STICKER, "Cat emoji pack",
            "This is an RGBA image with transparency. A chubby orange cat cartoon sticker with a happy face, sticker style with bold outline. The image has alpha channel and the background is transparent.",
            "官方推荐透明图格式",
        ),
        Card(
            Kind.STICKER, "Watercolor flower",
            "This is an RGBA image with transparency. A delicate watercolor peony flower sticker. The image has alpha channel and the background is transparent.",
            "官方推荐透明图格式",
        ),
        Card(
            Kind.STICKER, "Pixel-art heart",
            "This is an RGBA image with transparency. A retro pixel-art red heart with sparkle, game UI asset. The image has alpha channel and the background is transparent.",
            "官方推荐透明图格式",
        ),
        // ---- edits ----
        Card(
            Kind.EDIT, "Beach sunset backdrop",
            "Change the background to a sunset beach, keep the subject exactly the same",
        ),
        Card(
            Kind.EDIT, "Winter makeover",
            "Make it winter, snow on the ground, keep the pose and identity",
        ),
        Card(
            Kind.EDIT, "Studio portrait light",
            "Relight the photo like a professional studio portrait with softbox lighting, keep the face identical",
        ),
        Card(
            Kind.EDIT, "Ghibli style",
            "Redraw the photo in Studio Ghibli anime style, keep the composition",
        ),
        // ---- posters / typography ----
        Card(
            Kind.POSTER, "Bakery signboard",
            "A hand-painted wooden bakery signboard that reads \"FRESH BREAD\", morning light, street photography",
            "强项：文字渲染",
        ),
        Card(
            Kind.POSTER, "Movie poster",
            "A sci-fi movie poster titled \"STAR DRIFT\", bold typography, astronaut silhouette against a nebula",
            "强项：文字渲染",
        ),
        Card(
            Kind.POSTER, "Cassette label",
            "A retro mixtape cassette label that reads \"SUMMER 2026\", pastel colors, flat design",
            "强项：文字渲染",
        ),
        Card(
            Kind.POSTER, "Neon shop sign",
            "A neon shop sign that reads \"QANVAS\", rainy night street, reflections on wet pavement",
            "强项：文字渲染",
        ),
    )

    fun byKind(kind: Kind): List<Card> = all.filter { it.kind == kind }

    /** Rotating "today's inspiration" (deterministic by day). */
    fun today(): Card = all[Math.floorMod(System.currentTimeMillis() / 86_400_000L, all.size.toLong()).toInt()]
}
