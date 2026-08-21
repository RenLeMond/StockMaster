package com.stockmaster.app.data

/** 常用尺码模板。 */
val SIZE_PRESETS = listOf(
    listOf("S", "M", "L", "XL", "XXL", "XXXL"),
    (39..45).map { "${it}码" },
    (35..40).map { "${it}码" },
    (28..36).map { "${it}码" },
    (90..150 step 10).map { "${it}cm" }
)

val SIZE_PRESET_LABELS = listOf("服装 S-3XL", "男鞋 39-45", "女鞋 35-40", "裤码 28-36", "童装 90-150")
val COMMON_UNITS = listOf("件", "个", "箱", "套", "双", "支", "包", "瓶", "盒", "卷")
