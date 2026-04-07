# Vertical PDF Zoom - Double Zoom Bug Analysis

**Date:** 2026-04-06
**File under analysis:** `app/src/main/java/us/blindmint/codex/presentation/reader/PdfReaderLayout.kt`
**Branch:** `pdf-mupdf-renderer`
**Issue:** Images zoom additionally within their containers when panning after zoom

---

## Symptom Summary

1. **During pinch-zoom**: The container/column zooms correctly. The "zoom" part works.
2. **During pan after zoom**: Individual images within pages appear to zoom AGAIN within their individual page containers.
3. **Result**: A "double zoom" effect where pages appear cut off behind their boundaries.

This suggests the zoom IS being applied correctly by the outer `graphicsLayer`, but something about the pan gesture or subsequent recomposition is causing an ADDITIONAL zoom to be applied within `PdfPageSurface`.

---

## Architecture Overview

### Current Vertical Mode Structure

```
Box (viewport-sized)
  graphicsLayer(scaleX = effectiveZoom, scaleY = effectiveZoom, 
                translationX = effectivePanX, translationY = effectivePanY)
    children positioned via offset(y = topPx - verticalScrollPx)
      PdfPageSurface(uiScale = 1f, uiPanX = 0f, uiPanY = 0f)
        inner Box:
          graphicsLayer(scaleX = uiScale, scaleY = uiScale)  ← uiScale = 1f
            Image (ContentScale.Fit)
            Canvas (tiles)
```

### Zoom Flow
1. **Layout**: Pages are laid out at `layoutZoom = 1f` (Bug 3 fix) → dimensions are "natural" size
2. **Container**: Page containers are sized to layout dimensions (e.g., 1080x1512)
3. **Visual zoom**: `graphicsLayer(scaleX = effectiveZoom)` scales the container visually
4. **Bitmap**: Rendered at `committedZoom` resolution (e.g., 2x = 2160x3024 for a 1080x1512 page)
5. **Image display**: `ContentScale.Fit` fits the 2x bitmap into the 1x container (downscaled)
6. **Visual result**: Container appears at 2x zoom (2400x3024 screen space), content is slightly blurry

---

## Potential Root Causes

### Hypothesis 1: Stale `effectiveZoom` in Gesture Lambda

**Location:** `verticalZoomPanGesture` modifier (lines 716-750)

The `onZoomPan` lambda captures `effectiveZoom` which is computed BEFORE `transientZoom` is updated in the same frame:

```kotlin
val effectiveZoom = committedZoom * transientZoom.scale  // line 335
// ...
.onZoomPan = { zoomFactor, panX, panY ->
    val nextScale = (transientZoom.scale * zoomFactor).coerceIn(...)  // transientZoom NOT YET UPDATED
    val newEffectiveZoom = committedZoom * nextScale  // Uses OLD effectiveZoom-derived nextScale
```

When the lambda is executed, `transientZoom` has not yet been updated in that execution, so `effectiveZoom` (from line 335) still has the OLD `transientZoom.scale`. However, this shouldn't affect the visual zoom since `graphicsLayer` is applied during rendering, not in the lambda.

### Hypothesis 2: Tile Rendering Coordinate Space Mismatch

**Location:** `PdfPageSurface` Canvas tile drawing (lines 1189-1204)

```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    val widthScale = size.width / pageLayout.displayWidthPx
    val heightScale = size.height / pageLayout.displayHeightPx
    tiles.forEach { tile ->
        drawImage(
            image = image,
            dstOffset = IntOffset(
                x = (tile.request.tileRect.leftPx * widthScale).toInt(),
                y = (tile.request.tileRect.topPx * heightScale).toInt()
            ),
            dstSize = IntSize(
                width = (tile.request.tileRect.widthPx * widthScale).toInt(),
                height = (tile.request.tileRect.heightPx * heightScale).toInt()
            )
        )
    }
}
```

**The issue:**
- `tile.request.tileRect` values are in **layout coordinates** (at `layoutZoom = 1`, e.g., 1080x1512)
- `widthScale = size.width / pageLayout.displayWidthPx` - if both use layout dimensions, `widthScale = 1`
- But tiles are **rendered at `zoomScale = committedZoom`** resolution

**Example:**
- Page layout: 1080x1512 (at zoom=1)
- Tile rect: (0, 0, 540, 756) - quarter of page in layout coords
- Tile bitmap: 1080x1512 pixels (rendered at zoom=2)
- Container: 1080x1512 px
- `widthScale = 1080 / 1080 = 1`
- Tile drawn at dstSize (540, 756) - matching layout coordinates
- Tile bitmap (1080, 1512) is downscaled to (540, 756)

This seems correct - tiles are drawn at the same size as layout coords, and the bitmap (at 2x resolution) is downscaled.

### Hypothesis 3: graphicsLayer Scale Application After Offset

**Location:** `PdfPageSurface` inner Box (lines 1170-1180)

```kotlin
Box(
    modifier = Modifier
        .offset { IntOffset(contentOffsetX.toInt(), contentOffsetY.toInt()) }
        .width(with(density) { contentWidthPx!!.toDp() })
        .height(with(density) { contentHeightPx!!.toDp() })
        .graphicsLayer(
            scaleX = uiScale,  // 1f in vertical mode
            scaleY = uiScale,  // 1f in vertical mode
            transformOrigin = TransformOrigin.Center,
            clip = true
        )
)
```

**Concern:** The inner `graphicsLayer(scaleX = 1f)` should be a no-op. But if there's any floating-point issue or if `uiScale` is somehow not exactly 1f, it could cause issues.

### Hypothesis 4: visibleTileRequests Uses Different Zoom Than Rendering

**Location:** `renderPage` (lines 470-487)

```kotlin
val shouldUpdateTiles = layout != null && (isVertical || zoom <= 1.02f)
if (shouldUpdateTiles) {
    pageTiles[pageIndex] = activeController.getVisibleTiles(
        layout!!.visibleTileRequests(
            viewport = currentViewport,
            zoomScale = zoom,  // This is committedZoom
            panX = panX,
            panY = panY,
            invertColors = false
        )
    )
}
```

**Issue:** `visibleTileRequests` uses `zoomScale = committedZoom` for tile RENDERING (bitmap dimensions). But the tile POSITIONS are in `pageLayout.displayWidthPx` coordinates (at `layoutZoom = 1`).

If the tile RENDERING zoom doesn't match the tile POSITIONING zoom, tiles would be misaligned.

**Verification:** In `visibleTileRequests`:
- `zoomScale` is used to render tile bitmaps at that resolution
- `displayWidthPx` / `displayHeightPx` are used for viewport calculations

These should both be consistent (both at zoom=1 after Bug 3 fix), so this should be OK.

### Hypothesis 5: Pan Gesture Triggering Zoom Recomposition

**Location:** `onScrollPan` (lines 757-763)

```kotlin
onScrollPan = { panY ->
    val maxScroll = max(
        0f,
        verticalPagesTotalHeight - (viewport?.heightPx ?: 0)
    )
    val scrollDelta = panY / effectiveZoom
    verticalScrollPx = (verticalScrollPx - scrollDelta).coerceIn(0f, maxScroll)
}
```

**Concern:** When `verticalScrollPx` changes, it triggers recomposition. The visible pages recalculate. But does anything else change that could affect zoom?

The `LaunchedEffect` for rendering has `committedZoom` in its keys, so it only re-runs when `committedZoom` changes (not on every scroll).

### Hypothesis 6: Modifier Order Issue Persists

**Location:** Box modifier chain (lines 713-752)

After Phase 2 fix, the order is:
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .verticalZoomPanGesture(...)  // OUTSIDE - receives screen coords
        .graphicsLayer(...)          // INSIDE - applies visual transform
)
```

This should be correct. But let me verify the actual state of the code.

---

## Root Cause Identified

**Issue:** Tile Coordinate Space Mismatch

The comment in the code (lines 472-478) explicitly describes this issue:

> "In paged mode at zoom > 1, tile rects are in zoom=1.0 display space but the render matrix uses zoom=N. This causes the device offset/scissor to capture the wrong (more-zoomed) page region, which overlays the base image and makes the content appear to zoom in an extra time after the render delay."

**The Problem:**
1. `visibleTileRequests` calculates tile positions using `displayWidthPx` (at `layoutZoom = 1`)
2. But tiles are rendered at `zoomScale = committedZoom` (e.g., 2x)
3. When tiles are drawn in `PdfPageSurface`, they use `widthScale = size.width / pageLayout.displayWidthPx`
4. This causes tiles to be positioned at the WRONG SIZE, creating visual mismatch with the base image

**In vertical mode:** `shouldUpdateTiles = isVertical || zoom <= 1.02f` - tiles ARE updated when zoomed, unlike paged mode.

## Fix Applied

**Changed line 479 from:**
```kotlin
val shouldUpdateTiles = layout != null && (isVertical || zoom <= 1.02f)
```

**To:**
```kotlin
val shouldUpdateTiles = layout != null && zoom <= 1.02f
```

This skips tile updates when zoomed in BOTH vertical and paged modes. The base bitmap (re-rendered at `committedZoom` via Bug 4 fix) provides sufficient quality without the coordinate space mismatch.

## Remaining Questions

1. Is the base bitmap quality sufficient without tiles when zoomed?
2. Should tiles be fixed properly instead of skipped?
3. Could tiles be rendered at zoom=1 AND positioned correctly to avoid the mismatch?

## Files Modified

- `app/src/main/java/us/blindmint/codex/presentation/reader/PdfReaderLayout.kt` - Line 479: Tile skip condition
