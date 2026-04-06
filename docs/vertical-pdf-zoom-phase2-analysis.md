# Vertical PDF Zoom — Phase 2 Bug Analysis

**Date:** 2026-04-06
**File under analysis:** `app/src/main/java/us/blindmint/codex/presentation/reader/PdfReaderLayout.kt`
**Branch:** `pdf-mupdf-renderer`
**Precondition:** Fixes from `docs/vertical-pdf-zoom-bug-analysis.md` (Phase 1) have been applied

---

## Reported Symptoms

1. **Image stuttering during pinch zoom**: Individual images within page "frames" rapidly zoom in/out while the overall container zoom is smooth.
2. **Delayed second zoom after pan**: After zooming in, the zoom level looks correct. But when starting a single-finger pan/scroll, individual pages zoom in a second time after a brief delay.
3. **Horizontal lock not working**: The "lock horizontal if zoomed" feature (single-finger should only navigate vertically when zoomed) is not present in the current implementation.

---

## Architecture Context: Current Vertical Mode Transform Stack

After Phase 1 fixes, the vertical mode was refactored from `pointerInteropFilter` to Compose-native gestures. The current structure (lines 711-818):

```
Box(fillMaxSize)
  .graphicsLayer(effectiveZoom, effectivePanX, effectivePanY)    ← OUTER: visual zoom/pan
  .verticalZoomPanGesture(...)                                    ← INNER: gesture handler
    children:
      PdfPageSurface(uiScale=1f, uiPanX=0f, uiPanY=0f)         ← per-page composable
        inner Box:
          .graphicsLayer(scaleX=1f, scaleY=1f, clip=true)        ← no-op in vertical mode
          Image(ContentScale.Fit)
          Canvas(tiles)
```

The paged mode has a different (working) structure:

```
PdfPageSurface
  .pagedZoomPanGesture(...)                                       ← OUTER: gesture handler
    inner Box:
      .offset(panX, panY)
      .graphicsLayer(scale)                                       ← INNER: visual zoom
```

The critical difference: **paged mode puts the gesture handler OUTSIDE `graphicsLayer`; vertical mode puts it INSIDE.**

---

## Root Cause Analysis

### Bug #1 (CRITICAL): Gesture Handler Inside `graphicsLayer` — Causes Stuttering

**Location:** Lines 711-720 (modifier chain on the vertical container Box)

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .graphicsLayer(                          // ← applied FIRST (outermost)
            scaleX = effectiveZoom,
            scaleY = effectiveZoom,
            translationX = effectivePanX,
            translationY = effectivePanY
        )
        .verticalZoomPanGesture(...)             // ← applied SECOND (innermost)
)
```

In Compose's modifier chain, modifiers are applied left-to-right = outermost-to-innermost. Pointer events flow outside-in. So **`graphicsLayer` transforms pointer coordinates before they reach `verticalZoomPanGesture`**.

When `graphicsLayer` has `scaleX = scaleY = S`, pointer coordinates are divided by `S` before reaching inner modifiers. This means:

**During pinch zoom, deltas are measured in a coordinate space that changes every frame:**

| Frame | effectiveZoom | Screen delta (px) | Local delta (px) | Pan applied (px) | Screen effect (px) |
|-------|--------------|-------------------|-----------------|------------------|-------------------|
| 1 | 1.50 | 100 | 66.7 | 66.7 | 66.7 |
| 2 | 1.60 | 100 | 62.5 | 62.5 | 62.5 |
| 3 | 1.70 | 100 | 58.8 | 58.8 | 58.8 |

The zoom ratio (`span / lastSpan`) is scale-invariant, so the **zoom itself is smooth**. But the pan correction (centroid tracking) is **attenuated by the current zoom**, meaning the content drifts away from fingers during pinch. More importantly, the attenuation changes each frame as zoom changes, creating irregular position corrections that manifest as **individual pages appearing to jitter or vibrate** within the smooth zoom envelope.

Additionally, because the pointer coordinate space changes between frames (due to the zoom delta being applied to graphicsLayer), the `lastCentroid` from the previous frame is in a different coordinate space than the current centroid. This creates phantom deltas:

```
// In verticalZoomPanGesture, between frames:
// lastCentroid was recorded in frame N's coordinate space (scale S_n)
// Current centroid is in frame N+1's coordinate space (scale S_{n+1})
// The delta includes a phantom term proportional to (1/S_{n+1} - 1/S_n)
```

For an off-center pinch, this phantom term causes the content to shift each frame even when fingers are stationary. The magnitude depends on the finger distance from the graphicsLayer's transform origin, creating position-dependent jitter that looks like pages "zooming inside their containers."

**Why paged mode doesn't have this problem:** In paged mode, the gesture handler is the OUTERMOST modifier. Pointer events arrive in screen coordinates. The graphicsLayer is applied AFTER, only affecting rendering. Deltas are always in consistent screen-space pixels.

**Fix:** Swap the modifier order so the gesture handler receives screen-space coordinates:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .verticalZoomPanGesture(...)             // ← FIRST: receives screen-space coords
        .graphicsLayer(                          // ← SECOND: only affects rendering
            scaleX = effectiveZoom,
            scaleY = effectiveZoom,
            translationX = effectivePanX,
            translationY = effectivePanY
        )
)
```

After this change, `translationX/Y` in `graphicsLayer` is still in pixels, and pan deltas from the gesture handler are in pixels. They match directly. No attenuation, no phantom deltas.

**Important consideration:** After this fix, two-finger pan deltas during pinch will be in screen pixels. These deltas are added to `transientZoom.panX/Y` which feeds `effectivePanX/Y` → `translationX/Y`. Since `translationX` in `graphicsLayer` shifts the rendered output by that many screen pixels (regardless of scale), this is correct: a 100px screen drag produces a 100px screen shift.

---

### Bug #2 (CRITICAL): Single-Finger-When-Zoomed Pans GraphicsLayer Instead of Scrolling Document

**Location:** Lines 1093-1103 in `verticalZoomPanGesture`

```kotlin
count == 1 && (isPinchGesture || isZoomed()) -> {
    val pointer = pointers[0]
    val prev = lastSinglePosition ?: pointer.position
    lastSinglePosition = pointer.position
    val delta = pointer.position - prev
    if (delta != Offset.Zero) {
        gestureActive = true
        onZoomPan(1f, delta.x, delta.y)    // ← modifies graphicsLayer pan
    }
    pointer.consume()
}
```

When zoomed, single-finger drag calls `onZoomPan(1f, delta.x, delta.y)`, which updates `transientZoom.panX/Y` → `effectivePanX/Y` → `graphicsLayer.translationX/Y`. This:

1. **Pans the viewport, not the document**: The user can only move within the clamped viewport bounds (`maxPanY = (vpH * zoom - vpH) / 2`). At zoom 2x on a 2400px viewport, that's only 1200px of pan range. They cannot scroll through multiple pages.

2. **Includes horizontal movement**: `delta.x` is passed to `onZoomPan`, so horizontal finger movement pans the graphicsLayer horizontally. This defeats the intended horizontal lock.

3. **Causes the "delayed second zoom" symptom**: This is the most likely mechanism for the second zoom the user reports.

#### Mechanism for the "delayed second zoom"

When the user pans with one finger while zoomed, `transientZoom.panX/Y` accumulate. On gesture end:

```kotlin
onGestureEnd = {
    val committedScale = (committedZoom * transientZoom.scale).coerceIn(1f, 5f)
    // transientZoom.scale = 1 (was just panning, no zoom delta)
    // committedScale = committedZoom (unchanged)
    committedZoom = committedScale
    committedPanX = panX    // NEW: includes accumulated pan
    committedPanY = panY    // NEW: includes accumulated pan
    transientZoom = PdfTransientZoom()
}
```

`committedPanX/Y` change. If Bug #1 is also present (gesture inside graphicsLayer), the accumulated pan values are in the SCALED coordinate space, but they're applied as `translationX/Y` which operates in UNSCALED screen space. This coordinate mismatch means the committed pan values are `1/zoom` times what they should be.

On the NEXT gesture, the starting position is in the new graphicsLayer coordinate space (which uses the wrong committed pan values). The first delta includes a correction for this mismatch, which appears as a sudden position shift — the "delayed second zoom."

Additionally, if `committedZoom` is reassigned (even to the same value), it triggers the `LaunchedEffect(viewport, pageLayouts, committedZoom, pagedIndex)` at line 300. While this effect only updates `maxPanX/Y` (currently unused) and `currentPageLayout`, the state writes cause a recomposition. In pathological cases, this recomposition cascade could cause visible flickering.

**Fix — Part A: Route single-finger-when-zoomed to document scroll with horizontal lock:**

```kotlin
count == 1 && (isPinchGesture || isZoomed()) -> {
    val pointer = pointers[0]
    val prev = lastSinglePosition ?: pointer.position
    lastSinglePosition = pointer.position
    val delta = pointer.position - prev
    if (delta != Offset.Zero) {
        gestureActive = true
        if (isPinchGesture) {
            // Continuing after a pinch within the same gesture:
            // allow two-axis graphicsLayer pan
            onZoomPan(1f, delta.x, delta.y)
        } else {
            // New single-finger gesture while zoomed:
            // vertical document scroll only (horizontal lock)
            onScrollPan(delta.y)
        }
    }
    pointer.consume()
}
```

**Fix — Part B: Adjust `onScrollPan` to account for zoom when gesture is outside graphicsLayer:**

After Bug #1's fix (gesture handler outside graphicsLayer), deltas are in screen pixels. To convert to content-space scroll distance:

```kotlin
onScrollPan = { panY ->
    val maxScroll = max(
        0f,
        (buildVerticalPages().lastOrNull()?.let { it.topPx + it.heightPx } ?: 0f) - (viewport?.heightPx ?: 0)
    )
    // panY is in screen pixels; convert to content-space scroll distance
    val scrollDelta = panY / effectiveZoom
    verticalScrollPx = (verticalScrollPx - scrollDelta).coerceIn(0f, maxScroll)
}
```

This ensures consistent scroll speed on screen regardless of zoom level: at 2x zoom, a 100px screen drag scrolls 50px in content coordinates, which appears as 100px of movement on screen (because the content is magnified 2x).

**Fix — Part C: Decide behavior for `isPinchGesture` transition:**

When the user lifts one finger during a pinch (transitioning from 2-finger to 1-finger within the same gesture), `isPinchGesture` is true. The current code (and the fix above) routes this to `onZoomPan` for two-axis graphicsLayer pan. This allows repositioning the zoomed view after a pinch. Once the gesture ends and a new single-finger gesture starts, `isPinchGesture` is false, so it goes to vertical-only document scroll.

---

### Bug #3 (MODERATE): Layout Zoom in Vertical Mode Creates Double-Zoom Risk

**Location:** Lines 460-461 in `renderPage`

```kotlin
val layoutZoom = if (isVertical) zoom else 1f
//                              ^^^^ uses committedZoom (from default param)
val layout = pageLayouts[pageIndex] ?: updatePageLayout(pageIndex, layoutZoom)
```

In vertical mode, `layoutZoom = committedZoom`. If `updatePageLayout` is ever called with `committedZoom > 1` (because a layout is missing for a page), the layout's `displayWidthPx` and `displayHeightPx` are multiplied by `committedZoom`:

```kotlin
// In createPageLayout (PdfPageLayout.kt line 45-46):
val scaledWidth = croppedWidth * fitScale * zoomScale     // zoomScale = committedZoom
val scaledHeight = croppedHeight * fitScale * zoomScale
```

This doubled layout size flows to `buildVerticalPages()` → `PdfPageSurface` container size → graphicsLayer zooms again = **double zoom** for that specific page while all other pages remain at normal zoom.

Currently this is mitigated because all page layouts are created during initial load at `committedZoom = 1`. But the code is fragile — any code path that clears a layout entry and triggers re-render at `committedZoom > 1` would cause visible double-zoom on that page.

**Why paged mode is immune:** Paged mode always passes `layoutZoom = 1f`.

**Fix:** Always use `1f` for layout zoom in vertical mode (the graphicsLayer handles visual zoom):

```kotlin
val layoutZoom = 1f    // Both modes: layout at 1x, graphicsLayer handles visual zoom
val layout = pageLayouts[pageIndex] ?: updatePageLayout(pageIndex, layoutZoom)
```

---

### Bug #4 (MODERATE): No Re-Render Trigger After Zoom Level Changes

**Location:** Lines 623-658 (render LaunchedEffect)

```kotlin
LaunchedEffect(viewport, totalPages, verticalScrollPx, isVertical, pagedIndex) {
    // ...
    visiblePages.forEach { renderPage(it.pageIndex) }
}
```

`committedZoom` is **not** in the dependency list. After the user zooms from 1x to 2x:

1. `committedZoom` changes from 1 to 2
2. The render LaunchedEffect does not re-trigger
3. Visible pages remain rendered at zoom=1 resolution
4. The graphicsLayer scales these zoom=1 bitmaps by 2x → blurry image
5. Pages never re-render at the new zoom level (until something else triggers the effect, like `verticalScrollPx` changing)

The blurriness is visually noticeable and also means the user never gets high-quality zoomed rendering.

**Fix:** Add `committedZoom` to the LaunchedEffect keys:

```kotlin
LaunchedEffect(viewport, totalPages, verticalScrollPx, isVertical, pagedIndex, committedZoom) {
    // ...
    visiblePages.forEach { renderPage(it.pageIndex) }
}
```

After this change, zooming triggers a re-render at the new resolution. The bitmap is larger but `ContentScale.Fit` in PdfPageSurface scales it to fit the (zoom=1-sized) container. The graphicsLayer then scales visually. The net result is the same visual size but at higher resolution (sharper).

**Important:** This fix REQUIRES Bug #3's fix (layout zoom = 1f). If layouts are created at `committedZoom > 1`, the re-render would call `updatePageLayout(page, committedZoom)` for any missing layout, creating double-zoom-sized layouts. With Bug #3's fix, layouts stay at zoom=1, and only the bitmap quality improves.

---

### Bug #5 (MINOR): `buildVerticalPages()` Called on Every Recomposition

**Location:** Lines 410-424, called at lines 708, 763-766, and inside recomposition body

```kotlin
fun buildVerticalPages(): List<PdfVisiblePage> {
    var top = 0f
    return (0 until totalPages).mapNotNull { pageIndex ->
        val layout = pageLayouts[pageIndex] ?: return@mapNotNull null
        // ... allocates PdfVisiblePage for every page
    }
}
```

During a pinch zoom, `transientZoom` changes every frame, triggering recomposition. Each recomposition calls `buildVerticalPages()`, iterating ALL pages and allocating a `PdfVisiblePage` for each. For a 200-page PDF, that's 200 allocations per frame (60+ times per second).

Additionally, `onScrollPan` (line 762-768) calls `buildVerticalPages()` to compute `maxScroll` on every scroll event.

**Impact:** Garbage collection pressure from per-frame allocations can cause frame drops (micro-stuttering), especially on lower-end devices.

**Fix:** Cache the result and invalidate only when `pageLayouts` actually changes:

```kotlin
// At the top of the composable:
var cachedVerticalPages by remember { mutableStateOf(emptyList<PdfVisiblePage>()) }
var lastLayoutsVersion by remember { mutableIntStateOf(0) }

// Rebuild only when layouts change:
val currentLayoutsVersion = pageLayouts.size  // coarse but cheap change detection
if (currentLayoutsVersion != lastLayoutsVersion) {
    cachedVerticalPages = buildVerticalPages()
    lastLayoutsVersion = currentLayoutsVersion
}
```

Or more idiomatically, use `derivedStateOf`:

```kotlin
val verticalPages by remember {
    derivedStateOf { buildVerticalPages() }
}
```

`derivedStateOf` will only recompute when the snapshot state values read inside (the `pageLayouts` map entries) actually change.

---

### Bug #6 (MINOR): Dead State Variables

**Location:** Lines 296-297

```kotlin
var maxPanX by remember { mutableFloatStateOf(0f) }
var maxPanY by remember { mutableFloatStateOf(0f) }
```

These are computed in the `LaunchedEffect` at line 300 but never read anywhere. They were likely used by the old `pdfGestureInterop` code. Now that per-frame clamping is done inline in `onZoomPan` and `onGestureEnd`, these variables are dead code.

Similarly, `currentPageLayout` (line 298) is computed for vertical mode but only used in paged mode's gesture handler.

**Fix:** Remove `maxPanX`, `maxPanY`, and clean up the `LaunchedEffect` at line 300 to only run for paged mode, or remove it if `currentPageLayout` can be computed differently.

---

## Fix Priority and Implementation Order

### Phase 1: Critical fixes (resolve stuttering, second zoom, and horizontal lock)

1. **Swap modifier order** (Bug #1)
   - Move `.verticalZoomPanGesture(...)` before `.graphicsLayer(...)`
   - This immediately fixes pointer coordinate inconsistency and pan attenuation
   - Verify: pinch zoom should feel anchored to fingers, no jitter

2. **Route single-finger-when-zoomed to document scroll** (Bug #2)
   - Add `isPinchGesture` check to distinguish post-pinch pan from new-gesture scroll
   - Add horizontal lock (zero out delta.x for single-finger scroll when zoomed)
   - Adjust scroll delta by `1/effectiveZoom` for consistent screen-speed scrolling
   - Verify: after zoom, single-finger swipe scrolls document vertically only

3. **Fix layout zoom to always 1f** (Bug #3)
   - Change `layoutZoom` to `1f` unconditionally in `renderPage`
   - Verify: no page should appear double-zoomed at any zoom level

### Phase 2: Quality and performance

4. **Add committedZoom to render LaunchedEffect** (Bug #4)
   - Add `committedZoom` to LaunchedEffect keys at line 623
   - Verify: after zooming in, images should become sharp (not blurry upscale)

5. **Cache buildVerticalPages()** (Bug #5)
   - Use `derivedStateOf` to avoid per-frame recomputation
   - Verify: smoother frame rate during zoom on large PDFs

6. **Remove dead state** (Bug #6)
   - Remove `maxPanX`, `maxPanY`
   - Simplify or remove the LaunchedEffect at line 300

---

## How to Verify

After applying fixes, test these scenarios:

1. **Pinch zoom stability**: Pinch-zoom slowly at various positions (center, corners, edges). Individual page images should zoom smoothly with no jitter or independent movement within their frames.

2. **Focal point tracking**: Content under your fingers should stay under your fingers throughout the pinch. No drift toward center.

3. **Post-zoom scroll**: After zooming to 2-3x, single-finger vertical swipe should scroll smoothly through the document. No horizontal drift.

4. **Horizontal lock**: After zooming in, single-finger swipe should NOT move the view horizontally, even if your finger moves diagonally.

5. **Two-finger pan when zoomed**: With two fingers, you should be able to pan in any direction (for occasional horizontal repositioning).

6. **Zoom release stability**: Zoom to 3-5x, release fingers. View should not jump or snap to a different position.

7. **Image sharpness**: After zooming in and waiting briefly, page images should become sharp (not blurry upscale).

8. **Large PDF performance**: On a 100+ page PDF, pinch zoom should maintain smooth frame rate.

9. **Paged mode regression**: Verify LTR/RTL paged modes still work correctly after changes.

---

## Relationship to Phase 1 Fixes

The Phase 1 analysis correctly identified the `pointerInteropFilter` approach as problematic and recommended switching to Compose-native gestures. That refactor was implemented. However, the new Compose-native gesture handler was placed **inside** the `graphicsLayer` modifier (Bug #1 above), which recreates a similar coordinate-space mismatch to what `pointerInteropFilter` had — just through a different mechanism.

The Phase 1 fixes for per-frame pan clamping, correct maxPanY, and ACTION_POINTER_UP handling are still valid and should be preserved. The Phase 2 fixes build on top of Phase 1 by correcting the modifier ordering and adding the missing horizontal-lock scroll behavior.
