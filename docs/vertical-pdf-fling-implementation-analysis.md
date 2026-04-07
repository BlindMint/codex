# Vertical PDF Scroll Fling Implementation Analysis

**Date:** 2026-04-06  
**Status:** Research Complete - Implementation Options Identified

## Executive Summary

This document analyzes vertical scroll fling behavior in Mihon and LibreraReader to identify a workable solution for improving the PDF reader's fling deceleration in Codex. Three approaches are evaluated: using standard Android `Scroller`, adapting Mihon's custom fling calculation, or adopting Compose's `animateDecay` with proper physics.

---

## 1. Current Codex Implementation

**File:** `app/src/main/java/us/blindmint/codex/presentation/reader/PdfReaderLayout.kt`  
**Lines:** 1144-1166

### Current Fling Code

```kotlin
if (gestureActive) {
    if (abs(velocityY) > 2000f) {
        val distanceTimeFactor = 1.5f
        val totalDy = (distanceTimeFactor * velocityY / 2) / getEffectiveZoom().coerceAtLeast(1f)
        scope.launch {
            var lastValue = 0f
            val flingAnimatable = Animatable(0f)
            flingAnimatable.animateTo(
                targetValue = totalDy,
                animationSpec = tween(
                    durationMillis = 1500,
                    easing = LinearEasing
                )
            ) {
                val delta = this.value - lastValue
                lastValue = this.value
                onScrollPan(delta)
            }
        }
    } else {
        onGestureEnd()
    }
}
```

### Issues Identified

1. **LinearEasing produces constant velocity** - Initial velocity matches finger speed, but there's no natural deceleration feel
2. **Custom velocity calculation** - Uses `delta.y / deltaTime * 1e9f` which may not match system gesture detector accuracy
3. **Animation based approach** - Uses Compose `Animatable.animateTo()` which is frame-based, not physics-based

---

## 2. Mihon Implementation Analysis

### Project Location
`/home/samurai/dev/git/Mihon/`

### Key Files

| File | Path | Purpose |
|------|------|---------|
| `WebtoonRecyclerView.kt` | `app/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/webtoon/` | Custom RecyclerView with fling handling |
| `WebtoonFrame.kt` | Same directory | Gesture detectors wrapping RecyclerView |
| `WebtoonViewer.kt` | Same directory | Main viewer component |

### Fling Implementation

**File:** `WebtoonRecyclerView.kt` (lines 145-171)

```kotlin
fun zoomFling(velocityX: Int, velocityY: Int): Boolean {
    if (currentScale <= 1f) return false

    val distanceTimeFactor = 0.4f
    val animatorSet = AnimatorSet()

    if (velocityX != 0) {
        val dx = (distanceTimeFactor * velocityX / 2)
        val newX = getPositionX(x + dx)
        val translationXAnimator = ValueAnimator.ofFloat(x, newX)
        translationXAnimator.addUpdateListener { animation ->
            x = getPositionX(animation.animatedValue as Float)
        }
        animatorSet.play(translationXAnimator)
    }
    if (velocityY != 0 && (atFirstPosition || atLastPosition)) {
        val dy = (distanceTimeFactor * velocityY / 2)
        val newY = getPositionY(y + dy)
        val translationYAnimator = ValueAnimator.ofFloat(y, newY)
        translationYAnimator.addUpdateListener { animation ->
            y = getPositionY(animation.animatedValue as Float)
        }
        animatorSet.play(translationYAnimator)
    }

    animatorSet.duration = 400
    animatorSet.interpolator = DecelerateInterpolator()
    animatorSet.start()

    return true
}
```

### Important Note About Mihon

**This `zoomFling()` is NOT the normal vertical scroll fling.** It's specifically for edge bounce-back when zoomed past boundaries at the first/last page position. The **normal vertical scrolling uses RecyclerView's built-in fling physics**, which is handled by Android's framework and automatically provides smooth deceleration.

Mihon's key insight is the separation of concerns:
- **WebtoonFrame**: Handles gesture detection (ScaleGestureDetector + GestureDetector)
- **WebtoonRecyclerView**: Handles scroll, zoom, and fling animations
- Uses `GestureDetector.SimpleOnGestureListener` which provides velocity from the system gesture detector

### Key Constants from Mihon

| Constant | Value | Purpose |
|----------|-------|---------|
| `distanceTimeFactor` | 0.4f | Distance multiplier (distance = factor * velocity / 2) |
| `animatorSet.duration` | 400ms | Animation duration |
| `Interpolator` | `DecelerateInterpolator` | Natural deceleration easing |
| `MIN_RATE` | 0.5f | Minimum zoom scale |
| `MAX_SCALE_RATE` | 3.0f | Maximum zoom scale |

---

## 3. LibreraReader Implementation Analysis

### Project Location
`/home/samurai/dev/git/LibreraReader/`

### Key Files

| File | Path | Purpose |
|------|------|---------|
| `PdfSurfaceView.java` | `org/ebookdroid/ui/viewer/viewers/` | Main PDF view with Scroller |
| `VScrollController.java` | `org/ebookdroid/core/` | Vertical scroll controller |
| `AbstractScrollController.java` | `org/ebookdroid/core/` | Base scroll controller |
| `AdvGuestureDetector.java` | `com/foobnix/sys/` | Advanced gesture handling |

### Fling Implementation

**File:** `PdfSurfaceView.java`

```java
protected final Scroller scroller;

public PdfSurfaceView(final IActivityController baseActivity) {
    super(baseActivity.getContext());
    this.base = baseActivity;
    this.scroller = new Scroller(getContext());  // Standard Android Scroller
}

public void startFling(final float vX, final float vY, final Rect limits) {
    scroller.fling(getScrollX(), getScrollY(), -(int) vX, -(int) vY,
                   limits.left, limits.right, limits.top, limits.bottom);
}

public void continueScroll() {
    if (scroller.computeScrollOffset()) {
        scrollTo(scroller.getCurrX(), scroller.getCurrY());
    }
}
```

### Animation Loop

**File:** `AbstractScrollController.java` (line 56)

```java
public void drawView(final ViewState viewState) {
    if (view != null) {
        view.drawView(viewState);  // Triggers view.onDraw()
    }
    while (controller.isFling) {
        view.continueScroll();
    }
}
```

**File:** `AdvGuestureDetector.java` (lines 135-148) - Stop fling on touch:

```java
@Override
public boolean onDown(final MotionEvent e) {
    isScrollFinished = avc.getView().getScroller().isFinished();
    if (!isScrollFinished) {
        avc.getView().getScroller().forceFinished(true);
        isScrollFinished = true;
    }
    return true;
}
```

### Key Pattern from LibreraReader

1. **Uses standard Android `Scroller`** - Provides well-tested, physics-based fling
2. **Calls `computeScrollOffset()` in animation loop** - Standard Android pattern for fling
3. **Stops fling on touch down** - Essential for responsive feel
4. **Velocity filtering** - Ignores horizontal/vertical based on velocity ratio:
   ```java
   if (Math.abs(vX / vY) < 0.5) { x = 0; }
   if (Math.abs(vY / vX) < 0.5) { y = 0; }
   ```

---

## 4. Comparison of Approaches

### Approach Comparison Matrix

| Aspect | Mihon | LibreraReader | Codex (Current) |
|--------|-------|---------------|-----------------|
| **Scroll Container** | RecyclerView | Custom View + Scroller | Custom gesture + Compose animation |
| **Fling Algorithm** | ValueAnimator + DecelerateInterpolator | Android Scroller (physics-based) | Compose Animatable + LinearEasing |
| **Duration** | Fixed 400ms | Physics-based (Scroller computes) | Fixed 1500ms |
| **Velocity Source** | System GestureDetector | System GestureDetector | Custom calculation |
| **Deceleration** | Exponential (DecelerateInterpolator) | Physics-based friction | Linear (constant velocity) |
| **Edge Bounce** | Custom `zoomFling()` | Scroller limits | Not applicable (no bounds bounce) |

### Key Insight: RecyclerView's Built-in Fling

Both Mihon and the comic reader in Codex use **RecyclerView's built-in fling physics**. This is why the comic reader "feels right" - it's using Android's standard `View.onScrollChanged()` + `Scroller` internals through `RecyclerView`.

The PDF reader's vertical mode doesn't use `RecyclerView` or any scrollable container - it uses a custom `pointerInput` modifier with manual scroll offset management. This means it doesn't automatically get the system fling behavior.

---

## 5. Recommended Solutions

### Option A: Adopt Android Scroller (Recommended for Native Feel)

**Approach:** Use standard Android `Scroller` with `computeScrollOffset()` pattern, adapted for Compose.

**Implementation:**
1. Create a Compose wrapper that uses Android's `Scroller` for fling physics
2. Use `GestureDetector` to get accurate velocity from system
3. Call `computeScrollOffset()` in a `LaunchedEffect` or animation loop
4. Stop fling on subsequent touch events

**Pros:**
- Physics-based, natural feel
- Well-tested Android framework behavior
- Consistent with other Android apps

**Cons:**
- Requires interop between Compose and Android View/Scroller
- More complex than pure Compose solution

**Key Code Pattern:**
```kotlin
val scroller = remember { Scroller(context) }

LaunchedEffect(scroller) {
    while (scroller.computeScrollOffset()) {
        onScrollPan(scroller.currY.toFloat())
        yield()
    }
}
```

### Option B: Improve Current Compose Animation

**Approach:** Keep current `Animatable` approach but use proper deceleration easing.

**Implementation:**
1. Replace `LinearEasing` with a deceleration curve
2. Consider using `animateDecay` with `exponentialDecay` properly
3. Adjust distance/duration ratio for natural feel

**Current Problem with `animateDecay`:**
```kotlin
flingAnimatable.animateDecay(
    initialVelocity = velocityY,
    animationSpec = exponentialDecay(frictionMultiplier = 4f)
) {
    onScrollPan(this.value / getEffectiveZoom())  // BUG: value is velocity, not delta
}
```

The issue is passing `this.value` directly - `animateDecay` provides velocity per frame, not cumulative position.

**Fix would require tracking cumulative position separately.**

**Pros:**
- Pure Compose solution
- No Android View interop needed

**Cons:**
- Requires careful handling of animateDecay's velocity-based API
- May still not match system fling feel exactly

### Option C: Use LazyColumn with Custom FlingBehavior

**Approach:** Restructure PDF vertical mode to use `LazyColumn` with custom `FlingBehavior`.

**Implementation:**
1. Render pages as `LazyColumn` items
2. Implement custom `FlingBehavior` with desired deceleration
3. Handle zoom separately from scroll

**Pros:**
- Leverages Compose's scroll infrastructure
- Automatic prefetching, visibility tracking

**Cons:**
- Significant restructuring required
- PDF tile rendering doesn't fit LazyColumn item model
- Zoom architecture would need redesign

**See Section 6 for detailed analysis of why this is complex.**

---

## 6. LazyColumn Adoption Complexity

### Why LazyColumn Is Non-Trivial

#### Current Architecture

```
PdfReaderLayout
└── Box (graphicsLayer transform)
    └── PdfPageSurface (for each visible page)
        └── Page bitmap/tiles at calculated positions
```

All pages share a single `graphicsLayer` transform for zoom. Pages are positioned using cumulative `topPx` calculations, not Compose layout.

#### LazyColumn Architecture

```
LazyColumn
└── item { PdfPageSurface }
└── item { PdfPageSurface }
└── ...
```

Each item is independently laid out. Shared zoom transform would require:
- Per-item scaling
- Recalculated page positions at different zoom levels
- Custom `contentPadding` for zoom centering

#### PDF-Specific Complications

1. **Tile-based rendering** - LazyColumn items can't easily share tile calculations
2. **Viewport-dependent layouts** - Page layouts computed based on viewport dimensions
3. **Zoom as single transform** - Currently zoom is applied once to container, not per-page
4. **Visible page filtering** - Custom logic for preloading pages outside viewport

### Conclusion on LazyColumn

**Not recommended without significant architectural changes.** The current PDF rendering is optimized for tile-based rendering and shared zoom transform. LazyColumn would require breaking this apart.

---

## 7. Recommended Implementation Plan

### Step 1: Use System GestureDetector for Velocity

**Problem:** Current velocity calculation may be inaccurate:
```kotlin
velocityY = (delta.y / deltaTime) * 1e9f
```

**Solution:** Use Android's `GestureDetector` to get accurate velocity:
```kotlin
val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        thisVelocityY = velocityY
        return true
    }
})
```

### Step 2: Switch to Exponential Decay

**Problem:** `LinearEasing` provides constant velocity with no natural deceleration.

**Solution:** Use `FastOutSlowInEasing` or implement proper exponential decay:

```kotlin
flingAnimatable.animateTo(
    targetValue = totalDy,
    animationSpec = tween(
        durationMillis = 1500,
        easing = FastOutSlowInEasing  // Starts fast, decelerates naturally
    )
) {
    val delta = this.value - lastValue
    lastValue = this.value
    onScrollPan(delta)
}
```

### Step 3: Adjust Distance Calculation

**Current:** `distance = 1.5f * velocity / 2`

**For natural feel:** Match typical Android fling:
- Distance should be proportional to initial velocity
- Duration should allow velocity to naturally decay

### Step 4: Consider Scroller Interop (Long-term)

For the most native feel, consider creating a Compose `Scroller` wrapper:

```kotlin
@Composable
fun rememberScrollableState(): ScrollableState {
    val scroller = remember { Scroller(context) }
    // Implement scroll/fling logic using scroller
}
```

---

## 8. Files to Modify

### Primary Changes

| File | Changes |
|------|---------|
| `PdfReaderLayout.kt` | Update fling animation, potentially use GestureDetector |

### Specific Line Changes

**Lines 1144-1166** - Fling animation:
- Replace `LinearEasing` with `FastOutSlowInEasing` or custom deceleration
- Consider duration adjustments

**Lines 1073-1082** - Velocity tracking:
- Consider using `GestureDetector` for velocity instead of manual calculation

---

## 9. Summary

| Solution | Effort | Feel | Maintainability |
|----------|--------|------|-----------------|
| **A: Scroller interop** | Medium | Excellent (native) | Standard Android pattern |
| **B: Improved Compose** | Low | Good | Pure Compose |
| **C: LazyColumn** | High | Good | Complex architecture change |

### Recommended Next Steps

1. **Immediate:** Change `LinearEasing` to `FastOutSlowInEasing` for natural deceleration
2. **Short-term:** Investigate using `GestureDetector` for accurate velocity
3. **Long-term:** Consider Scroller interop wrapper for truly native fling feel

The comic reader's natural feel comes from using RecyclerView (which uses Scroller internally). The PDF reader bypasses this by using custom scroll offset management. Either embrace custom animations with proper physics, or adopt Scroller interop for the native feel.
