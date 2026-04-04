package us.blindmint.codex.domain.reader

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset

enum class NavigationRegion {
    MENU,
    PREV,
    NEXT,
    LEFT,
    RIGHT
}

data class NavigationRegionConfig(
    val rect: RectF,
    val region: NavigationRegion
)

sealed class ReaderTapZone(
    val displayName: String,
    val regions: List<NavigationRegionConfig>
) {
    object Default : ReaderTapZone(
        displayName = "Default",
        regions = listOf(
            NavigationRegionConfig(RectF(0f, 0f, 0.2f, 1f), NavigationRegion.PREV),
            NavigationRegionConfig(RectF(0.8f, 0f, 1f, 1f), NavigationRegion.NEXT)
        )
    )

    object LShaped : ReaderTapZone(
        displayName = "L-Shaped",
        regions = listOf(
            NavigationRegionConfig(RectF(0f, 0f, 0.3f, 0.33f), NavigationRegion.PREV),
            NavigationRegionConfig(RectF(0f, 0.33f, 0.5f, 0.7f), NavigationRegion.PREV),
            NavigationRegionConfig(RectF(0.7f, 0f, 1f, 0.33f), NavigationRegion.NEXT),
            NavigationRegionConfig(RectF(0.5f, 0.33f, 1f, 0.7f), NavigationRegion.NEXT),
            NavigationRegionConfig(RectF(0f, 0.7f, 1f, 1f), NavigationRegion.NEXT)
        )
    )

    object Kindle : ReaderTapZone(
        displayName = "Kindle",
        regions = listOf(
            NavigationRegionConfig(RectF(0f, 0f, 0.2f, 1f), NavigationRegion.PREV),
            NavigationRegionConfig(RectF(0.8f, 0f, 1f, 1f), NavigationRegion.NEXT)
        )
    )

    object Edge : ReaderTapZone(
        displayName = "Edge",
        regions = listOf(
            NavigationRegionConfig(RectF(0f, 0f, 0.04f, 1f), NavigationRegion.PREV),
            NavigationRegionConfig(RectF(0.96f, 0f, 1f, 1f), NavigationRegion.NEXT)
        )
    )

    object RightAndLeft : ReaderTapZone(
        displayName = "Right and Left",
        regions = listOf(
            NavigationRegionConfig(RectF(0f, 0f, 0.5f, 1f), NavigationRegion.PREV),
            NavigationRegionConfig(RectF(0.5f, 0f, 1f, 1f), NavigationRegion.NEXT)
        )
    )

    object Disabled : ReaderTapZone(
        displayName = "Disabled",
        regions = emptyList()
    )

    companion object {
        fun fromOrdinal(ordinal: Int): ReaderTapZone = when (ordinal) {
            0 -> Default
            1 -> LShaped
            2 -> Kindle
            3 -> Edge
            4 -> RightAndLeft
            5 -> Disabled
            else -> Default
        }
    }
}

fun ReaderTapZone.getRegionAt(x: Float, y: Float, width: Float, height: Float): NavigationRegion? {
    val normalizedX = x / width
    val normalizedY = y / height

    for (regionConfig in regions) {
        if (regionConfig.rect.contains(normalizedX, normalizedY)) {
            return regionConfig.region
        }
    }
    return null
}

enum class TapInversion {
    NONE,
    HORIZONTAL,
    VERTICAL,
    BOTH;

    companion object {
        fun fromString(value: String): TapInversion = when (value.uppercase()) {
            "HORIZONTAL" -> HORIZONTAL
            "VERTICAL" -> VERTICAL
            "BOTH" -> BOTH
            else -> NONE
        }
    }
}

fun RectF.invertForTapInversion(inversion: TapInversion): RectF {
    return when (inversion) {
        TapInversion.NONE -> this
        TapInversion.HORIZONTAL -> RectF(1f - left, top, 1f - right, bottom)
        TapInversion.VERTICAL -> RectF(left, 1f - top, right, 1f - bottom)
        TapInversion.BOTH -> RectF(1f - left, 1f - top, 1f - right, 1f - bottom)
    }
}
