package com.samge.qanvas.core

import java.util.Locale

/**
 * Prompt inspiration library — categorized around the model's four flagship abilities,
 * borrowing structures from popular community prompt styles (cinematic, studio product,
 * flat illustration, isometric, anime, double-exposure, miniature, macro …).
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

    fun isZh(): Boolean = Locale.getDefault().language == "zh"

    private fun rgba(en: String, zh: String) = Pair(
        "This is an RGBA image with transparency. $en The image has alpha channel and the background is transparent.",
        "这是一张带透明通道的RGBA图片。$zh 图片含alpha通道，背景为透明。",
    )

    val all: List<Card> = listOf(
        // ================ T2I · cinematic / photography ================
        Card(Kind.T2I, "Neon café, rainy night", "雨夜霓虹咖啡馆",
            "A cozy coffee shop on a rainy evening, warm light, a neon sign that reads \"QWEN\", reflections on wet pavement",
            "雨夜温馨的咖啡馆，暖光，写着\"QWEN\"的霓虹招牌，湿漉漉路面的倒影"),
        Card(Kind.T2I, "Fishing at dawn", "黎明垂钓",
            "An old fisherman casting a net from a wooden boat on a misty lake at dawn, soft golden light, photorealistic",
            "黎明薄雾湖面上，老渔夫在木船上撒网，柔和的金色光线，照片级真实感"),
        Card(Kind.T2I, "Cliffside lighthouse", "悬崖灯塔",
            "A lighthouse on a stormy cliff, dramatic clouds, beam of light cutting through rain, cinematic",
            "暴风雨中的悬崖灯塔，翻涌的云层，光束穿透雨幕，电影感"),
        Card(Kind.T2I, "Cyberpunk street food", "赛博朋克小吃摊",
            "A night market noodle stall in a neon cyberpunk alley, steam rising, holographic menu signs, rain",
            "霓虹赛博朋克小巷里的夜市面摊，蒸汽升腾，全息菜单牌，雨夜"),
        Card(Kind.T2I, "Golden-hour portrait", "黄金时刻人像",
            "Studio-quality portrait of a young woman at golden hour, rim light through hair, shallow depth of field, 85mm lens look",
            "黄金时刻的棚拍级少女肖像，发丝轮廓光，浅景深，85mm 镜头质感"),
        Card(Kind.T2I, "Double exposure", "双重曝光",
            "Double exposure of a city skyline and a forest, silhouette blend, minimal white background, high contrast",
            "城市天际线与森林的双重曝光，剪影融合，极简白底，高对比"),
        Card(Kind.T2I, "Miniature world", "微缩世界",
            "Tilt-shift miniature photography of a tiny train station with commuters, exaggerated bokeh, bright daylight",
            "移轴微缩摄影：小小火车站与通勤人群，夸张焦外光斑，明亮日光"),
        Card(Kind.T2I, "Macro dew", "微距露珠",
            "Extreme macro of a dew drop on a spider web at sunrise, refraction of the whole meadow inside the drop",
            "日出时蛛网上露珠的极微距，整片草甸折射在露珠内"),
        Card(Kind.T2I, "Isometric room", "等距视角小屋",
            "Cozy isometric cutaway of a tiny bookshop apartment, warm palette, soft shadows, game-art style",
            "温馨的等距剖面小书店公寓，暖色调，柔和阴影，游戏美术风"),
        Card(Kind.T2I, "Product hero shot", "产品大片",
            "Commercial product photography of a glass perfume bottle on wet black stone, single hard light, mist, luxury",
            "玻璃香水瓶置于湿润黑石上的商业产品摄影，单硬光，薄雾，奢侈品感"),
        Card(Kind.T2I, "Ink wash mountains", "水墨山水",
            "Traditional Chinese ink wash painting of misty layered mountains, a lone boat, vast negative space",
            "传统水墨画：云雾层叠的群山，一叶孤舟，大面积留白"),
        Card(Kind.T2I, "Paper-cut layers", "剪纸层叠",
            "Layered paper-cut craft illustration of an autumn forest, warm tones, visible paper texture and soft drop shadows",
            "秋季森林的多层剪纸工艺插画，暖色调，可见纸张纹理与柔和投影"),
        Card(Kind.T2I, "Vaporwave grid", "蒸汽波网格",
            "Vaporwave sunset over an endless chrome grid, palm silhouettes, pink and cyan gradient sky",
            "无尽铬合金网格上的蒸汽波日落，棕榈剪影，粉青渐变天空"),
        Card(Kind.T2I, "Ghibli meadow", "吉卜力草地",
            "Anime style vast green meadow with wind-blown grass, cumulus clouds, a red-roofed cottage far away",
            "动画风格：风吹草浪的绿色草原，积云，远处红顶小屋"),

        // ================ RGBA stickers ================
        Card(Kind.STICKER, "Cute dragon", "可爱小龙",
            rgba("A cute cartoon dragon sticker.", "一只可爱的卡通小龙贴纸。").first,
            rgba("A cute cartoon dragon sticker.", "一只可爱的卡通小龙贴纸。").second,
            "官方推荐透明图格式", "官方推荐透明图格式"),
        Card(Kind.STICKER, "Chubby cat", "胖橘猫",
            rgba("A chubby orange cat sticker with a happy face, bold outline sticker style.", "一只开心的胖橘猫贴纸，粗描边贴纸风格。").first,
            rgba("A chubby orange cat sticker with a happy face, bold outline sticker style.", "一只开心的胖橘猫贴纸，粗描边贴纸风格。").second,
            "官方推荐透明图格式", "官方推荐透明图格式"),
        Card(Kind.STICKER, "Watercolor peony", "水彩牡丹",
            rgba("A delicate watercolor peony flower sticker.", "精致的水彩牡丹花贴纸。").first,
            rgba("A delicate watercolor peony flower sticker.", "精致的水彩牡丹花贴纸。").second,
            "官方推荐透明图格式", "官方推荐透明图格式"),
        Card(Kind.STICKER, "Pixel heart", "像素爱心",
            rgba("A retro pixel-art red heart with sparkle, game UI asset.", "复古像素风红色爱心带闪光，游戏UI素材。").first,
            rgba("A retro pixel-art red heart with sparkle, game UI asset.", "复古像素风红色爱心带闪光，游戏UI素材。").second,
            "官方推荐透明图格式", "官方推荐透明图格式"),
        Card(Kind.STICKER, "Emoji pack sheet", "表情包九宫格",
            rgba("A sticker sheet of nine cute round emoji faces showing different emotions, bold outlines.", "九个圆形可爱表情贴纸排成一版，各带不同情绪，粗描边。").first,
            rgba("A sticker sheet of nine cute round emoji faces showing different emotions, bold outlines.", "九个圆形可爱表情贴纸排成一版，各带不同情绪，粗描边。").second,
            "官方推荐透明图格式", "官方推荐透明图格式"),
        Card(Kind.STICKER, "Arrow icon set", "箭头图标组",
            rgba("A set of four flat-design neon arrows pointing up, down, left and right, UI assets.", "四个扁平风霓虹箭头分别指向上/下/左/右，UI素材组。").first,
            rgba("A set of four flat-design neon arrows pointing up, down, left and right, UI assets.", "四个扁平风霓虹箭头分别指向上/下/左/右，UI素材组。").second,
            "官方推荐透明图格式", "官方推荐透明图格式"),

        // ================ Edits ================
        Card(Kind.EDIT, "Beach sunset backdrop", "日落海滩背景",
            "Change the background to a sunset beach, keep the subject exactly the same",
            "把背景换成日落海滩，主体保持完全一致"),
        Card(Kind.EDIT, "Winter makeover", "冬日改造",
            "Make it winter, snow on the ground, keep the pose and identity",
            "变成冬天，地面有雪，保持姿态与人物身份"),
        Card(Kind.EDIT, "Studio portrait light", "棚拍人像光",
            "Relight the photo like a professional studio portrait with softbox lighting, keep the face identical",
            "用柔光箱专业棚拍布光重新打光，保持面部完全一致"),
        Card(Kind.EDIT, "Ghibli style", "吉卜力风格",
            "Redraw the photo in Studio Ghibli anime style, keep the composition",
            "把照片重绘成吉卜力动画风格，保持构图"),
        Card(Kind.EDIT, "Business attire", "换正装",
            "Change the outfit to a tailored navy business suit, keep the face and pose unchanged",
            "把服装换成合身的藏青色正装西装，面部与姿态保持不变"),
        Card(Kind.EDIT, "Remove background", "纯色背景",
            "Replace the background with a clean studio light-gray gradient, keep the subject edges crisp",
            "把背景换成干净的影棚浅灰渐变，主体边缘保持利落"),
        Card(Kind.EDIT, "Age progression", "年代照",
            "Make it look like a photo from the 1990s: film grain, slightly faded colors, keep the person identical",
            "变成 1990 年代的照片质感：胶片颗粒、轻微褪色，人物保持一致"),
        Card(Kind.EDIT, "Seasonal poster style", "杂志封面感",
            "Restyle as a fashion magazine cover photo: high contrast, editorial lighting, keep the pose",
            "改造成时尚杂志封面照：高对比、编辑布光，姿态保持"),

        // ================ Posters / typography ================
        Card(Kind.POSTER, "Bakery signboard", "面包店招牌",
            "A hand-painted wooden bakery signboard that reads \"FRESH BREAD\", morning light, street photography",
            "一块手绘木质面包店招牌，写着\"FRESH BREAD\"，晨光，街头摄影",
            "强项：文字渲染", "强项：文字渲染"),
        Card(Kind.POSTER, "Movie poster", "电影海报",
            "A sci-fi movie poster titled \"STAR DRIFT\", bold typography, astronaut silhouette against a nebula",
            "科幻电影海报，标题\"STAR DRIFT\"，粗犷字体，宇航员剪影映衬星云",
            "强项：文字渲染", "强项：文字渲染"),
        Card(Kind.POSTER, "Cassette label", "磁带标签",
            "A retro mixtape cassette label that reads \"SUMMER 2026\", pastel colors, flat design",
            "复古磁带标签，写着\"SUMMER 2026\"，柔和色彩，扁平设计",
            "强项：文字渲染", "强项：文字渲染"),
        Card(Kind.POSTER, "Neon shop sign", "霓虹店招",
            "A neon shop sign that reads \"QANVAS\", rainy night street, reflections on wet pavement",
            "写着\"QANVAS\"的霓虹店招，雨夜街道，湿漉漉路面的倒影",
            "强项：文字渲染", "强项：文字渲染"),
        Card(Kind.POSTER, "Sneaker poster", "球鞋海报",
            "Minimal sports poster: a white sneaker floating, huge bold text \"RUN 26\", coral accent, swiss grid",
            "极简运动海报：漂浮的白色球鞋，超大粗体\"RUN 26\"，珊瑚色点缀，瑞士网格",
            "强项：文字渲染", "强项：文字渲染"),
        Card(Kind.POSTER, "Music festival", "音乐节海报",
            "Indie music festival poster titled \"SOUND & SUMMER\", risograph texture, sun and wave motifs",
            "独立音乐节海报\"SOUND & SUMMER\"，Risograph 印刷质感，太阳与波浪母题",
            "强项：文字渲染", "强项：文字渲染"),
        Card(Kind.POSTER, "Menu board", "手写菜单板",
            "A chalkboard café menu listing \"LATTE 4.5 / MOCHA 5.0\", hand-drawn doodles, warm lamp light",
            "黑板咖啡菜单，列着\"LATTE 4.5 / MOCHA 5.0\"，手绘涂鸦，暖灯光",
            "强项：文字渲染", "强项：文字渲染"),
        Card(Kind.POSTER, "Book cover", "书籍封面",
            "A minimal literary book cover titled \"THE QUIET SEA\", matte paper texture, one thin line of horizon",
            "极简文学书籍封面《THE QUIET SEA》，哑光纸质感，一条细细的地平线",
            "强项：文字渲染", "强项：文字渲染"),
    )

    fun byKind(kind: Kind): List<Card> = all.filter { it.kind == kind }

    /** Rotating "today's inspiration" (deterministic by day). */
    fun today(): Card = all[Math.floorMod(System.currentTimeMillis() / 86_400_000L, all.size.toLong()).toInt()]
}
