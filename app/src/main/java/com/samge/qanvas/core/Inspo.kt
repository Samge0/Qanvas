package com.samge.qanvas.core

import java.util.Locale

/**
 * Prompt inspiration library — categorized around the model's four flagship abilities.
 * The RGBA sticker template follows the official recommended format.
 * Prompts ship bilingually (en/zh) and resolve by the current app locale.
 */
object Inspo {
    enum class Kind { T2I, STICKER, EDIT, POSTER }

    data class Card(
        val kind: Kind,
        val title: String,
        val titleZh: String,
        val prompt: String,
        val promptZh: String,
        val hint: String = "",
        val hintZh: String = "",
    ) {
        fun titleFor(zh: Boolean) = if (zh) titleZh else title
        fun promptFor(zh: Boolean) = if (zh) promptZh else prompt
        fun hintFor(zh: Boolean) = if (zh) hintZh else hint
    }

    fun isZh(): Boolean {
        val l = Locale.getDefault().language
        return l == "zh"
    }

    val all: List<Card> = listOf(
        // ---- text-to-image ----
        Card(
            Kind.T2I, "Neon café, rainy night", "雨夜霓虹咖啡馆",
            "A cozy coffee shop on a rainy evening, warm light, a neon sign that reads \"QWEN\", reflections on wet pavement",
            "雨夜温馨的咖啡馆，暖光，写着\"QWEN\"的霓虹招牌，湿漉漉路面的倒影",
        ),
        Card(
            Kind.T2I, "Fishing at dawn", "黎明垂钓",
            "An old fisherman casting a net from a wooden boat on a misty lake at dawn, soft golden light, photorealistic",
            "黎明薄雾湖面上，老渔夫在木船上撒网，柔和的金色光线，照片级真实感",
        ),
        Card(
            Kind.T2I, "Cliffside lighthouse", "悬崖灯塔",
            "A lighthouse on a stormy cliff, dramatic clouds, beam of light cutting through rain, cinematic",
            "暴风雨中的悬崖灯塔，翻涌的云层，光束穿透雨幕，电影感",
        ),
        Card(
            Kind.T2I, "Mecha bonsai", "机甲盆景",
            "A tiny mechanical bonsai tree made of brass gears and copper wires on a scholar's desk, macro photography",
            "书桌上黄铜齿轮与铜线制成的迷你机械盆景，微距摄影",
        ),
        Card(
            Kind.T2I, "Cyberpunk street food", "赛博朋克小吃摊",
            "A night market noodle stall in a neon cyberpunk alley, steam rising, holographic menu signs, rain",
            "霓虹赛博朋克小巷里的夜市面摊，蒸汽升腾，全息菜单牌，雨夜",
        ),
        // ---- RGBA stickers (official prompt format) ----
        Card(
            Kind.STICKER, "Cute dragon sticker", "可爱小龙贴纸",
            "This is an RGBA image with transparency. A cute cartoon dragon sticker. The image has alpha channel and the background is transparent.",
            "这是一张带透明通道的RGBA图片。一只可爱的卡通小龙贴纸。图片含alpha通道，背景为透明。",
            "官方推荐透明图格式", "官方推荐透明图格式",
        ),
        Card(
            Kind.STICKER, "Cat emoji pack", "橘猫表情包",
            "This is an RGBA image with transparency. A chubby orange cat cartoon sticker with a happy face, sticker style with bold outline. The image has alpha channel and the background is transparent.",
            "这是一张带透明通道的RGBA图片。一只开心的胖橘猫卡通贴纸，贴纸风格带粗描边。图片含alpha通道，背景为透明。",
            "官方推荐透明图格式", "官方推荐透明图格式",
        ),
        Card(
            Kind.STICKER, "Watercolor peony", "水彩牡丹",
            "This is an RGBA image with transparency. A delicate watercolor peony flower sticker. The image has alpha channel and the background is transparent.",
            "这是一张带透明通道的RGBA图片。精致的水彩牡丹花贴纸。图片含alpha通道，背景为透明。",
            "官方推荐透明图格式", "官方推荐透明图格式",
        ),
        Card(
            Kind.STICKER, "Pixel-art heart", "像素爱心",
            "This is an RGBA image with transparency. A retro pixel-art red heart with sparkle, game UI asset. The image has alpha channel and the background is transparent.",
            "这是一张带透明通道的RGBA图片。复古像素风的红色爱心带闪光，游戏UI素材。图片含alpha通道，背景为透明。",
            "官方推荐透明图格式", "官方推荐透明图格式",
        ),
        // ---- edits ----
        Card(
            Kind.EDIT, "Beach sunset backdrop", "日落海滩背景",
            "Change the background to a sunset beach, keep the subject exactly the same",
            "把背景换成日落海滩，主体保持完全一致",
        ),
        Card(
            Kind.EDIT, "Winter makeover", "冬日改造",
            "Make it winter, snow on the ground, keep the pose and identity",
            "变成冬天，地面有雪，保持姿态与人物身份",
        ),
        Card(
            Kind.EDIT, "Studio portrait light", "棚拍人像光",
            "Relight the photo like a professional studio portrait with softbox lighting, keep the face identical",
            "用柔光箱专业棚拍布光重新打光，保持面部完全一致",
        ),
        Card(
            Kind.EDIT, "Ghibli style", "吉卜力风格",
            "Redraw the photo in Studio Ghibli anime style, keep the composition",
            "把照片重绘成吉卜力动画风格，保持构图",
        ),
        // ---- posters / typography ----
        Card(
            Kind.POSTER, "Bakery signboard", "面包店招牌",
            "A hand-painted wooden bakery signboard that reads \"FRESH BREAD\", morning light, street photography",
            "一块手绘木质面包店招牌，写着\"FRESH BREAD\"，晨光，街头摄影",
            "强项：文字渲染", "强项：文字渲染",
        ),
        Card(
            Kind.POSTER, "Movie poster", "电影海报",
            "A sci-fi movie poster titled \"STAR DRIFT\", bold typography, astronaut silhouette against a nebula",
            "科幻电影海报，标题\"STAR DRIFT\"，粗犷字体，宇航员剪影映衬星云",
            "强项：文字渲染", "强项：文字渲染",
        ),
        Card(
            Kind.POSTER, "Cassette label", "磁带标签",
            "A retro mixtape cassette label that reads \"SUMMER 2026\", pastel colors, flat design",
            "复古磁带标签，写着\"SUMMER 2026\"，柔和色彩，扁平设计",
            "强项：文字渲染", "强项：文字渲染",
        ),
        Card(
            Kind.POSTER, "Neon shop sign", "霓虹店招",
            "A neon shop sign that reads \"QANVAS\", rainy night street, reflections on wet pavement",
            "写着\"QANVAS\"的霓虹店招，雨夜街道，湿漉漉路面的倒影",
            "强项：文字渲染", "强项：文字渲染",
        ),
    )

    fun byKind(kind: Kind): List<Card> = all.filter { it.kind == kind }

    /** Rotating "today's inspiration" (deterministic by day). */
    fun today(): Card = all[Math.floorMod(System.currentTimeMillis() / 86_400_000L, all.size.toLong()).toInt()]
}
