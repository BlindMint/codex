# Vertical PDF Zoom Bug Analysis

**Date:** 2026-04-05
**File under analysis:** `app/src/main/java/us/blindmint/codex/presentation/reader/PdfReaderLayout.kt`
**Branch:** `pdf-mupdf-renderer`

## Reported Symptoms

1. **Zoom direction drift**: When pinch-zooming in vertical/webtoon mode, the zoom does not anchor to the finger position. Instead the view drifts "down and to the right."
2. **Zoom off-page**: Zooming far enough pushes the view off the page entirely into the black background.
3. **Jump and stuck on release**: After releasing fingers from a deep zoom, the view jumps back near the viewport center and becomes stuck — zooming out no longer works; the app must be restarted.

Paged PDF modes (LTR/RTL) do **not** exhibit these issues.

---

## Architecture Overview: Two Completely Different Zoom Approaches

### Vertical Mode (BROKEN) — Lines 697-845

```
Box(fillMaxSize)                         ← viewport-sized container
  .graphicsLayer(scale, translation)     ← zoom/pan transform
  .pdfGestureInterop(pointerInteropFilter) ← Android ScaleGestureDetector
    children: pages at offset(topPx - verticalScrollPx)
```

- Uses `pointerInteropFilter` (Android MotionEvent bridge) **inside** the `graphicsLayer`
- Uses `ScaleGestureDetector` for pinch detection
- Applies an explicit **focal-point formula** to compute pan correction during zoom
- **No per-frame pan clamping** during zoom — only on gesture end
- Pan clamp on gesture end uses `totalContentHeight` (entire document)

### Paged Mode (WORKS) — Lines 847-987, 1010-1077

```
PdfPageSurface (content-sized)
  .pagedZoomPanGesture(pointerInput)     ← Compose gesture detection
    inner Box:
      .offset(panX, panY)
      .graphicsLayer(scale)              ← zoom transform
```

- Uses `Modifier.pointerInput` (Compose-native gesture API) **outside** the `graphicsLayer`
- Uses manual pointer tracking (`awaitEachGesture`) for pinch detection
- Uses **centroid-tracking pan** (panDelta = centroid - lastCentroid) — no focal-point formula
- **Per-frame pan clamping** during zoom (lines 913-921)
- Pan clamp uses the single page's actual dimensions

---

## Root Cause Analysis

There are **five concrete bugs** in the vertical mode zoom implementation, plus one structural design issue. Bugs #1 and #2 are the primary cause of the symptoms; the others compound the problem.

### Bug #1 (CRITICAL): No Per-Frame Pan Clamping During Zoom

**Location:** Lines 761-766 (onZoomPan callback)

```kotlin
// VERTICAL MODE — no clamping!
transientZoom = PdfTransientZoom(
    scale = nextScale,
    panX = transientZoom.panX + if (notZoomed) 0f else deltaX,  // unclamped
    panY = transientZoom.panY + if (notZoomed) 0f else deltaY,  // unclamped
    active = true
)
```

Compare to paged mode (lines 911-927):

```kotlin
// PAGED MODE — clamped every frame
val totalPanX = committedPanX + transientZoom.panX + panDelta.x
val totalPanY = committedPanY + transientZoom.panY + panDelta.y
val (clampedTotalPanX, clampedTotalPanY) = if (newEffectiveZoom > 1.02f ...) {
    totalPanX.coerceIn(-maxPanXLocal, maxPanXLocal) to
    totalPanY.coerceIn(-maxPanYLocal, maxPanYLocal)
} else { 0f to 0f }
transientZoom = PdfTransientZoom(
    scale = newScale,
    panX = clampedTotalPanX - committedPanX,
    panY = clampedTotalPanY - committedPanY,
    active = true
)
```

**Impact:** In vertical mode, pan accumulates without any bounds check during the gesture. Any drift from finger movement, floating-point accumulation, or the dead-zone offset (Bug #3) compounds frame-over-frame without correction. This is why the user can zoom entirely off the page.

**Fix:** Add per-frame pan clamping to the vertical onZoomPan handler, matching the paged mode pattern:

```kotlin
onZoomPan = { zoomFactor, panX, panY ->
    val nextScale = (transientZoom.scale * zoomFactor).coerceIn(1f / committedZoom, 5f / committedZoom)
    val notZoomed = nextScale <= 1.02f / committedZoom
    val deltaX: Float
    val deltaY: Float
    if (zoomFactor != 1f) {
        // ... existing focal-point formula (unchanged) ...
    } else {
        deltaX = panX
        deltaY = panY
    }

    val newEffectiveZoom = committedZoom * nextScale
    val totalPanX = committedPanX + transientZoom.panX + if (notZoomed) 0f else deltaX
    val totalPanY = committedPanY + transientZoom.panY + if (notZoomed) 0f else deltaY

    // ---- ADD: Per-frame clamping (matching paged mode) ----
    val (clampedPanX, clampedPanY) = if (newEffectiveZoom > 1.02f && viewport != null) {
        val vp = viewport!!
        val maxPanXLocal = max(0f, (vp.widthPx * newEffectiveZoom - vp.widthPx) / 2f)
        val maxPanYLocal = max(0f, (vp.heightPx * newEffectiveZoom - vp.heightPx) / 2f)
        totalPanX.coerceIn(-maxPanXLocal, maxPanXLocal) to
        totalPanY.coerceIn(-maxPanYLocal, maxPanYLocal)
    } else {
        0f to 0f
    }

    transientZoom = PdfTransientZoom(
        scale = nextScale,
        panX = clampedPanX - committedPanX,
        panY = clampedPanY - committedPanY,
        active = true
    )
    // ... existing vertical scroll code ...
}
```

---

### Bug #2 (CRITICAL): Incorrect maxPanY in onGestureEnd for Vertical Mode

**Location:** Lines 779-785 (onGestureEnd callback)

```kotlin
val contentWidth = if (isVertical) vp.widthPx.toFloat() else currentPageLayout!!.displayWidthPx
val contentHeight = if (isVertical) totalContentHeight else currentPageLayout!!.displayHeightPx
//                                  ^^^^^^^^^^^^^^^^^^
//                                  BUG: uses ENTIRE DOCUMENT HEIGHT
val maxPanXLocal = max(0f, (contentWidth * committedScale - vp.widthPx) / 2f)
val maxPanYLocal = max(0f, (contentHeight * committedScale - vp.heightPx) / 2f)
```

**The problem:** `totalContentHeight` is the sum of ALL pages stacked vertically (potentially 240,000+ px for a 100-page PDF). But the `graphicsLayer` scales a **viewport-sized** Box. The content visible within the Box at any time is at most ~`vpH` pixels tall (the pages positioned via `verticalScrollPx`).

**Concrete example:**
- 100-page PDF, each page 2400px tall: `totalContentHeight ≈ 241,200px`
- Viewport: 1080x2400
- At zoom 2x: `maxPanYLocal = (241200 * 2 - 2400) / 2 = 240,000px`
- Correct value should be: `(2400 * 2 - 2400) / 2 = 1,200px`

The Y-axis pan constraint is **200x too loose**. This effectively allows unconstrained vertical panning.

**Impact:** On gesture end, the pan "clamp" for Y barely constrains anything, so wildly drifted pan values from Bug #1 are preserved. This causes the view to stay off-page after release, or jump unpredictably when the X clamp (which IS correct) snaps but Y doesn't.

**Fix:** Use viewport height, not total content height:

```kotlin
val contentWidth = if (isVertical) vp.widthPx.toFloat() else currentPageLayout!!.displayWidthPx
val contentHeight = if (isVertical) vp.heightPx.toFloat() else currentPageLayout!!.displayHeightPx
//                                  ^^^^^^^^^^^^^^^^^^
//                                  FIXED: viewport height
```

This should also be applied to the new per-frame clamping from Bug #1's fix (already shown correctly above).

---

### Bug #3 (MODERATE): `notZoomed` Dead Zone Causes Initial Focal-Point Offset

**Location:** Lines 741-764

```kotlin
val nextScale = (transientZoom.scale * zoomFactor).coerceIn(1f / committedZoom, 5f / committedZoom)
val notZoomed = nextScale <= 1.02f / committedZoom
// ...
transientZoom = PdfTransientZoom(
    scale = nextScale,
    panX = transientZoom.panX + if (notZoomed) 0f else deltaX,  // zeroed when notZoomed
    panY = transientZoom.panY + if (notZoomed) 0f else deltaY,  // zeroed when notZoomed
    active = true
)
```

**The problem:** When starting a fresh zoom from `committedZoom=1`, the first ~2% of zoom increase (`nextScale` from 1.0 to ~1.02) has `notZoomed=true`, which **discards the focal-point pan correction**. The `graphicsLayer` zooms around `TransformOrigin.Center` (viewport center) without any offset to track the finger position.

**Concrete example:** Pinching at (300, 800) on a 1080x2400 viewport:
- At zoom 1.02 with pan=(0,0): content at (300, 800) moved to screen position (293, 792)
- The 8px Y offset means the view shifted DOWN relative to the fingers
- When the formula kicks in at 1.02+, it correctly tracks FROM the offset position, but the initial shift persists

**Impact:** This creates a small but systematic "nudge toward center" at the start of every fresh zoom gesture. If the user pinches above-center, the view shifts down. If right-of-center, it shifts right. This compounds with Bug #1 (no clamping) since the initial offset feeds into subsequent focal-point calculations with a persistent error.

**Fix:** Either remove the dead zone entirely (let the formula run from the start), or apply a retroactive pan correction when transitioning from notZoomed to zoomed:

Option A (simpler — remove the dead zone):
```kotlin
transientZoom = PdfTransientZoom(
    scale = nextScale,
    panX = transientZoom.panX + deltaX,   // always apply
    panY = transientZoom.panY + deltaY,   // always apply
    active = true
)
```

Option B (preserve the dead zone for scroll behavior but add one-time correction):
```kotlin
// When transitioning from notZoomed to zoomed, compute the
// retroactive pan correction for the zoom that already happened.
if (!notZoomed && transientZoom.panX == 0f && transientZoom.panY == 0f && transientZoom.scale > 1f) {
    // The zoom went from 1.0 to transientZoom.scale without pan correction.
    // Apply the full correction now.
    val fullDeltaX = (transientZoom.scale - 1f) * (committedPanX - focusX + vpW / 2f)
    val fullDeltaY = (transientZoom.scale - 1f) * (committedPanY - focusY + vpH / 2f)
    // Add fullDelta + current delta
}
```

**Recommendation:** Option A is cleaner. The `notZoomed` guard's purpose is to avoid jitter at 1x, but with per-frame clamping (Bug #1 fix), the clamp to (0,0) when effectiveZoom <= 1.02 achieves the same effect.

---

### Bug #4 (MODERATE): Missing ACTION_POINTER_UP Handling

**Location:** Lines 157-184 (handleMotionEvent)

```kotlin
when (event.actionMasked) {
    MotionEvent.ACTION_DOWN -> {
        lastX = event.x; lastY = event.y
        // ...
    }
    MotionEvent.ACTION_MOVE -> {
        if (scaleDetector?.isInProgress != true && event.pointerCount == 1) {
            val dx = event.x - lastX   // ← uses stale lastX from 2-finger state
            val dy = event.y - lastY
            // ...
        }
        lastX = event.x; lastY = event.y
    }
    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { ... }
    // ACTION_POINTER_DOWN and ACTION_POINTER_UP are NOT handled!
}
```

**The problem:** When one finger lifts during/after a pinch (`ACTION_POINTER_UP`), `lastX`/`lastY` are not updated. If pointer 0 lifts and the remaining pointer (formerly pointer 1) becomes the new pointer 0, the next `ACTION_MOVE` computes `dx = newPointer0.x - oldPointer0.lastX`, which can be a very large delta — causing an abrupt pan jump.

**Impact:** After a pinch gesture, transitioning to single-finger pan produces a sudden large pan offset, contributing to the "zoom jumps" symptom.

**Fix:** Handle `ACTION_POINTER_UP` to reset `lastX`/`lastY` to the remaining pointer's position:

```kotlin
MotionEvent.ACTION_POINTER_UP -> {
    // Find which pointer remains and update lastX/Y to its position
    val upIndex = event.actionIndex
    val remainingIndex = if (upIndex == 0) 1 else 0
    if (remainingIndex < event.pointerCount) {
        lastX = event.getX(remainingIndex)
        lastY = event.getY(remainingIndex)
    }
}
```

---

### Bug #5 (MINOR): Stale `effectiveZoom` in onZoomPan Lambda

**Location:** Line 767

```kotlin
if (effectiveZoom <= 1.02f && abs(panY) > 0f) {
    verticalScrollPx = (verticalScrollPx - panY).coerceIn(0f, maxScroll)
}
```

**The problem:** `effectiveZoom` is a `val` computed at line 323 during composition:
```kotlin
val effectiveZoom = committedZoom * transientZoom.scale
```

Inside the `onZoomPan` lambda (which runs from MotionEvent callbacks between recompositions), `effectiveZoom` holds the value from the **last composition**, not the value after `transientZoom` was just updated a few lines above.

**Impact:** Low in practice, because `panY` is 0 during scale events (ScaleGestureDetector passes `0f, 0f` for pan). The condition `abs(panY) > 0f` prevents the stale effectiveZoom from causing incorrect scroll updates during pinch zoom. During single-finger drag (`zoomFactor == 1`), the transientZoom scale doesn't change, so effectiveZoom is less stale.

**Fix:** Compute the current effective zoom locally:

```kotlin
val currentEffZoom = committedZoom * transientZoom.scale  // use the just-updated transientZoom
if (currentEffZoom <= 1.02f && abs(panY) > 0f) {
```

Wait — `transientZoom` was just reassigned above, and it's a `mutableStateOf`. Reads after the write will see the new value. But the `transientZoom` referenced in this line is the **new** PdfTransientZoom (the one just written). So this would work. However, to be explicit and safe:

```kotlin
val currentEffZoom = committedZoom * nextScale
if (currentEffZoom <= 1.02f && abs(panY) > 0f) {
```

---

### Structural Issue: Gesture Approach Mismatch

The vertical mode uses `pointerInteropFilter` (Android MotionEvent bridge) nested inside a `graphicsLayer` transform, while paged mode uses Compose-native `pointerInput` outside the transform. This creates several subtle differences:

| Aspect | Vertical Mode | Paged Mode |
|--------|--------------|------------|
| Gesture API | `pointerInteropFilter` + `ScaleGestureDetector` | `Modifier.pointerInput` + manual pointer tracking |
| Coordinate space | Inside `graphicsLayer` (may or may not transform coords depending on Compose version) | Outside `graphicsLayer` (always layout coords) |
| Focal point strategy | Explicit mathematical formula | Implicit centroid tracking (`panDelta = centroid - lastCentroid`) |
| Pan clamping | Only on gesture end | Every frame |
| Container size | Viewport-sized Box | Content-sized Box |

The `pointerInteropFilter`-inside-`graphicsLayer` placement is risky because Compose's handling of pointer coordinates through `graphicsLayer` has evolved across versions. In your current dependency set (Compose UI 1.7.8 / foundation 1.8.0-beta03), this behavior may be inconsistent. If `graphicsLayer` transforms pointer coordinates before they reach `pointerInteropFilter`, the focal-point formula (which assumes untransformed layout coordinates) would produce increasingly wrong results as zoom increases — explaining the progressive drift.

**Recommendation:** For a robust long-term fix, consider refactoring vertical mode to use the same gesture approach as paged mode:

```kotlin
// Replace pointerInteropFilter with Compose-native gesture handling:
Modifier.pointerInput(Unit) {
    awaitEachGesture {
        // Same approach as pagedZoomPanGesture but with vertical scroll support
        // Compute zoomDelta from span ratio, panDelta from centroid movement
        // This avoids the ScaleGestureDetector coordinate space ambiguity
    }
}
```

This eliminates the `pointerInteropFilter` + `graphicsLayer` coordinate space question entirely.

---

## MuPDF Renderer Assessment

Your instinct is correct — the MuPDF renderer is **not involved** in this bug. The renderer produces bitmaps at requested zoom levels; the zoom/pan transform is applied entirely by the `graphicsLayer` in Compose. Both vertical and paged modes use the same renderer (`PdfRenderController`), and paged mode works correctly. The issue is purely in the gesture handling and transform application in `PdfReaderLayout.kt`.

---

## Fix Priority and Implementation Order

### Phase 1: Fix the concrete bugs (should resolve all three symptoms)

1. **Add per-frame pan clamping to vertical onZoomPan** (Bug #1)
   - Use viewport dimensions (not totalContentHeight) for clamp bounds
   - This alone should prevent "zooming off the page"

2. **Fix maxPanY in onGestureEnd** (Bug #2)
   - Change `totalContentHeight` to `vp.heightPx.toFloat()`
   - This prevents the "jump" and "stuck" on release

3. **Handle ACTION_POINTER_UP** (Bug #4)
   - Reset `lastX`/`lastY` to remaining pointer position
   - Prevents pan jumps on finger transition

4. **Remove notZoomed dead zone** (Bug #3)
   - Per-frame clamping makes this guard unnecessary
   - Eliminates the initial center-biased offset

5. **Fix stale effectiveZoom** (Bug #5)
   - Use `committedZoom * nextScale` instead of captured `effectiveZoom`

### Phase 2 (Optional): Refactor gesture handling

6. **Replace `pointerInteropFilter` with Compose-native gestures** for vertical mode
   - Eliminates coordinate space ambiguity
   - Aligns both modes on the same proven gesture approach
   - Biggest change but most robust solution

---

## How to Verify

After applying fixes, test these scenarios:

1. **Focal point tracking**: Pinch-zoom at various positions (center, corners, edges). The content under your fingers should stay under your fingers throughout the zoom.
2. **Pan bounds**: At maximum zoom (5x), try panning in all directions. You should not be able to pan beyond the page edges.
3. **Release stability**: Zoom to 3-5x, release fingers. The view should stay at the zoomed position without jumping.
4. **Zoom out**: After zooming in, verify you can pinch to zoom back out to 1x smoothly.
5. **Scroll at 1x**: At normal zoom, verify vertical scrolling still works (single-finger swipe).
6. **Finger transition**: Zoom with two fingers, lift one, continue panning with the remaining finger. Should be smooth with no jump.
7. **Paged mode regression**: Verify LTR/RTL paged modes still work correctly after changes.
