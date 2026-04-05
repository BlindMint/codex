/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.graphics.Bitmap
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ViewConfiguration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.SideEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import us.blindmint.codex.data.util.CachedFileFactory
import us.blindmint.codex.domain.library.book.Book
import us.blindmint.codex.domain.reader.ReaderPageState
import us.blindmint.codex.pdf.engine.PdfDocumentSession
import us.blindmint.codex.pdf.model.PdfCropBounds
import us.blindmint.codex.pdf.model.PdfPageGeometry
import us.blindmint.codex.pdf.model.PdfPageLayout
import us.blindmint.codex.pdf.model.PdfRenderRequest
import us.blindmint.codex.pdf.model.PdfSelectionBounds
import us.blindmint.codex.pdf.model.PdfSelectionState
import us.blindmint.codex.pdf.model.PdfTileBitmap
import us.blindmint.codex.pdf.model.PdfViewport
import us.blindmint.codex.pdf.model.createPageLayout
import us.blindmint.codex.pdf.model.visibleTileRequests
import us.blindmint.codex.pdf.render.PdfRenderController
import us.blindmint.codex.ui.reader.ReaderEvent
import kotlin.math.abs
import kotlin.math.max

private const val TAG = "CodexPdf"
private const val VERTICAL_PAGE_SPACING_PX = 12f

private data class PdfGestureCallbacks(
    val onTap: (tapX: Float, width: Float) -> Unit,
    val onLongPress: (tapX: Float, tapY: Float) -> Unit,
    val onZoomPan: (zoomFactor: Float, panX: Float, panY: Float) -> Unit,
    val onGestureEnd: () -> Unit
)

private data class PdfVisiblePage(
    val pageIndex: Int,
    val topPx: Float,
    val leftPx: Float,
    val widthPx: Float,
    val heightPx: Float
)

private data class PdfTransientZoom(
    val scale: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val active: Boolean = false
)

private fun Modifier.pdfGestureInterop(callbacks: PdfGestureCallbacks): Modifier {
    return this.pointerInteropFilter {
        val gestureState = PdfGestureInteropStateHolder.state ?: return@pointerInteropFilter false
        gestureState.handleMotionEvent(it, callbacks)
    }
}

private object PdfGestureInteropStateHolder {
    var state: PdfGestureInteropState? = null
}

private class PdfGestureInteropState {
    private var accumulatedPanX = 0f
    private var accumulatedPanY = 0f
    private var gestureDetector: GestureDetector? = null
    private var scaleDetector: ScaleGestureDetector? = null
    private var lastX = 0f
    private var lastY = 0f

    var lastScaleFocusX = 0f
    var lastScaleFocusY = 0f

    var callbacks: PdfGestureCallbacks? = null
    var widthProvider: (() -> Float)? = null
    var contextProvider: (() -> android.content.Context)? = null
    // In paged mode the HorizontalPager must handle single-finger swipes when not
    // zoomed, so we only consume events when a pinch is in progress or the view is
    // already zoomed.  In vertical mode we always consume (original behaviour).
    var isZoomed: Boolean = false
    var isPagedMode: Boolean = false

    fun handleMotionEvent(event: MotionEvent, currentCallbacks: PdfGestureCallbacks): Boolean {
        callbacks = currentCallbacks
        ensureDetectors()
        gestureDetector?.onTouchEvent(event)
        scaleDetector?.onTouchEvent(event)

        val isMultiTouch = event.pointerCount > 1 || scaleDetector?.isInProgress == true

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                lastScaleFocusX = event.x
                lastScaleFocusY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                if (scaleDetector?.isInProgress != true && event.pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    val context = contextProvider?.invoke() ?: return true
                    val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
                    if (abs(dx) > slop || abs(dy) > slop) {
                        callbacks?.onZoomPan?.invoke(1f, dx, dy)
                    }
                    lastX = event.x
                    lastY = event.y
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                scalingInProgress = false
                callbacks?.onGestureEnd?.invoke()
                accumulatedPanX = 0f
                accumulatedPanY = 0f
            }
        }

        // In paged mode pass single-finger events through to the pager when not
        // zoomed so that horizontal swipe navigation keeps working.
        return if (isPagedMode) isMultiTouch || isZoomed else true
    }

    // Set to true from onScaleBegin so that onLongPress (which fires after ~500 ms
    // of the *first* finger being held down) is ignored during a pinch gesture.
    private var scalingInProgress = false

    private fun ensureDetectors() {
        if (gestureDetector != null && scaleDetector != null) return
        val context = contextProvider?.invoke() ?: return
        gestureDetector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    callbacks?.onTap?.invoke(e.x, widthProvider?.invoke() ?: 0f)
                    return true
                }

                override fun onLongPress(e: MotionEvent) {
                    if (!scalingInProgress) {
                        callbacks?.onLongPress?.invoke(e.x, e.y)
                    }
                }
            }
        )
        scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    scalingInProgress = true
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    lastScaleFocusX = detector.focusX
                    lastScaleFocusY = detector.focusY
                    callbacks?.onZoomPan?.invoke(detector.scaleFactor, 0f, 0f)
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    scalingInProgress = false
                }
            }
        )
    }
}

@Composable
fun PdfReaderLayout(
    book: Book,
    currentPage: Int,
    initialPage: Int = 0,
    onPageChanged: (Int) -> Unit,
    backgroundColor: Color,
    readingDirection: String = "LTR",
    comicScaleType: Int = 1,
    modifier: Modifier = Modifier,
    showMenu: Boolean = false,
    showPageIndicator: Boolean = true,
    onLoadingComplete: () -> Unit = {},
    onScrollRestorationComplete: () -> Unit = {},
    onMenuToggle: () -> Unit = {},
    onTotalPagesLoaded: (Int) -> Unit = {},
    onPageSelected: (Int) -> Unit = {},
    onTextSelected: (ReaderEvent.OnTextSelected) -> Unit = {},
    invertColors: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnTextSelected by rememberUpdatedState(onTextSelected)

    var session by remember { mutableStateOf<PdfDocumentSession?>(null) }
    var renderController by remember { mutableStateOf<PdfRenderController?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var viewport by remember { mutableStateOf<PdfViewport?>(null) }
    var committedZoom by remember { mutableFloatStateOf(1f) }
    var committedPanX by remember { mutableFloatStateOf(0f) }
    var committedPanY by remember { mutableFloatStateOf(0f) }
    var transientZoom by remember { mutableStateOf(PdfTransientZoom()) }
    var verticalScrollPx by remember { mutableFloatStateOf(0f) }
    var pagedIndex by remember { mutableIntStateOf(initialPage) }
    var selectionState by remember { mutableStateOf(PdfSelectionState()) }
    var lastReadingDirection by remember { mutableStateOf(readingDirection) }

    val pageGeometries = remember { mutableStateMapOf<Int, PdfPageGeometry>() }
    val pageCropBounds = remember { mutableStateMapOf<Int, PdfCropBounds>() }
    val pageLayouts = remember { mutableStateMapOf<Int, PdfPageLayout>() }
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val pageTiles = remember { mutableStateMapOf<Int, List<PdfTileBitmap>>() }
    val activeBitmapVersions = remember { mutableStateMapOf<Int, Int>() }
    val pageStates = remember { mutableStateMapOf<Int, ReaderPageState>() }

    val isVertical = readingDirection == "VERTICAL"

    @Suppress("UNUSED_VARIABLE")
    val _unusedComicScaleType = comicScaleType

    val imageColorFilter: ColorFilter? = if (invertColors) {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    } else {
        null
    }

    val effectiveZoom = committedZoom * transientZoom.scale
    val effectivePanX = committedPanX + transientZoom.panX
    val effectivePanY = committedPanY + transientZoom.panY
    val interopState = remember { PdfGestureInteropState() }

    DisposableEffect(Unit) {
        interopState.contextProvider = { context }
        interopState.widthProvider = { viewport?.widthPx?.toFloat() ?: 0f }
        PdfGestureInteropStateHolder.state = interopState
        onDispose {
            if (PdfGestureInteropStateHolder.state === interopState) {
                PdfGestureInteropStateHolder.state = null
            }
        }
    }

    SideEffect {
        Log.d(
            TAG,
            "state: isLoading=$isLoading totalPages=$totalPages viewport=$viewport pagedIndex=$pagedIndex currentPage=$currentPage hasSession=${session != null} hasController=${renderController != null} layouts=${pageLayouts.size} bitmaps=${pageBitmaps.size}"
        )
        // Keep gesture interop flags in sync after every recomposition so the
        // handler always sees the latest zoom / mode state without needing to
        // re-run the DisposableEffect.
        interopState.isZoomed = effectiveZoom > 1.02f
        interopState.isPagedMode = !isVertical
    }

    val visibleBitmapCount = if (isVertical) {
        1
    } else {
        if (pageBitmaps[pagedIndex] != null) 1 else 0
    }

    LaunchedEffect(readingDirection) {
        if (lastReadingDirection != readingDirection) {
            lastReadingDirection = readingDirection
            committedZoom = 1f
            committedPanX = 0f
            committedPanY = 0f
            transientZoom = PdfTransientZoom()
        }
    }

    suspend fun ensureGeometry(pageIndex: Int): PdfPageGeometry? {
        pageGeometries[pageIndex]?.let { return it }
        val activeSession = session ?: return null
        val geometry = activeSession.loadPageGeometry(pageIndex)
        pageGeometries[pageIndex] = geometry
        return geometry
    }

    suspend fun ensureCropBounds(pageIndex: Int): PdfCropBounds? {
        pageCropBounds[pageIndex]?.let { return it }
        val activeSession = session ?: return null
        val cropBounds = activeSession.estimateCropBounds(pageIndex)
        pageCropBounds[pageIndex] = cropBounds
        return cropBounds
    }

    fun updatePageLayout(pageIndex: Int, zoom: Float = committedZoom): PdfPageLayout? {
        val currentViewport = viewport ?: return null
        val geometry = pageGeometries[pageIndex] ?: return null
        val cropBounds = pageCropBounds[pageIndex]
        val layout = geometry.createPageLayout(
            viewport = currentViewport,
            zoomScale = zoom,
            fitWidth = isVertical,
            cropLeftRatio = cropBounds?.leftRatio ?: 0f,
            cropTopRatio = cropBounds?.topRatio ?: 0f,
            cropRightRatio = cropBounds?.rightRatio ?: 1f,
            cropBottomRatio = cropBounds?.bottomRatio ?: 1f
        )
        pageLayouts[pageIndex] = layout
        return layout
    }

    fun buildVerticalPages(): List<PdfVisiblePage> {
        var top = 0f
        return (0 until totalPages).mapNotNull { pageIndex ->
            val layout = pageLayouts[pageIndex] ?: return@mapNotNull null
            val page = PdfVisiblePage(
                pageIndex = pageIndex,
                topPx = top,
                leftPx = 0f,
                widthPx = layout.displayWidthPx,
                heightPx = layout.displayHeightPx
            )
            top += layout.displayHeightPx + VERTICAL_PAGE_SPACING_PX
            page
        }
    }

    LaunchedEffect(currentPage, isVertical, totalPages, pageLayouts.size) {
        if (totalPages <= 0) return@LaunchedEffect
        if (isVertical) {
            val pages = buildVerticalPages()
            val target = pages.firstOrNull { it.pageIndex == currentPage } ?: return@LaunchedEffect
            val vpHeight = viewport?.heightPx?.toFloat() ?: return@LaunchedEffect
            // Only snap when the target page is not already visible — prevents fighting
            // with user-initiated scrolling and layout-load-triggered recompositions.
            val isAlreadyVisible =
                target.topPx < verticalScrollPx + vpHeight &&
                target.topPx + target.heightPx > verticalScrollPx
            if (!isAlreadyVisible) {
                verticalScrollPx = target.topPx.coerceAtLeast(0f)
            }
        } else if (currentPage != pagedIndex) {
            pagedIndex = currentPage.coerceIn(0, totalPages - 1)
        }
    }

    suspend fun renderPage(pageIndex: Int, zoom: Float = committedZoom, panX: Float = committedPanX, panY: Float = committedPanY) {
        val activeController = renderController ?: run {
            pageStates[pageIndex] = ReaderPageState.Error("No render controller")
            return
        }
        val currentViewport = viewport ?: run {
            pageStates[pageIndex] = ReaderPageState.Error("No viewport")
            return
        }
        pageStates[pageIndex] = ReaderPageState.Loading
        try {
            val layout = pageLayouts[pageIndex] ?: updatePageLayout(pageIndex, zoom)
            val result = activeController.render(
                PdfRenderRequest(
                    pageIndex = pageIndex,
                    viewport = currentViewport,
                    zoomScale = zoom,
                    invertColors = false
                )
            )
            if (layout != null) {
                pageTiles[pageIndex] = activeController.getVisibleTiles(
                    layout.visibleTileRequests(
                        viewport = currentViewport,
                        zoomScale = zoom,
                        panX = panX,
                        panY = panY,
                        invertColors = false
                    )
                )
            }
            val bitmap = result.bitmap
            val oldBitmap = pageBitmaps.put(pageIndex, bitmap)
            activeBitmapVersions[pageIndex] = (activeBitmapVersions[pageIndex] ?: 0) + 1
            if (oldBitmap != null && oldBitmap !== bitmap && !oldBitmap.isRecycled) {
                withContext(Dispatchers.Main) {
                    withFrameNanos { }
                    oldBitmap.recycle()
                }
            }
            pageStates[pageIndex] = ReaderPageState.Ready
        } catch (e: Exception) {
            pageStates[pageIndex] = ReaderPageState.Error(e.message ?: "Unknown error", e)
        }
    }

    fun clampPan(pageIndex: Int, zoom: Float, panX: Float, panY: Float): Pair<Float, Float> {
        val layout = pageLayouts[pageIndex] ?: return 0f to 0f
        val currentViewport = viewport ?: return 0f to 0f
        if (zoom <= 1.02f) return 0f to 0f
        // Page layouts are kept at their base (zoom=1) size; the graphicsLayer applies
        // all visual scaling.  The visual content size is therefore
        //   displayWidthPx * zoom  ×  displayHeightPx * zoom
        // and the pan bounds keep that content within the viewport.
        val maxPanX = max(0f, (layout.displayWidthPx * zoom - currentViewport.widthPx) / 2f)
        val maxPanY = max(0f, (layout.displayHeightPx * zoom - currentViewport.heightPx) / 2f)
        return panX.coerceIn(-maxPanX, maxPanX) to panY.coerceIn(-maxPanY, maxPanY)
    }

    // bitmapX / bitmapY are pixel coordinates in the rendered bitmap at committedZoom.
    // Callers are responsible for inverting the graphicsLayer transform from screen
    // coordinates to page-local display coordinates, then multiplying by committedZoom.
    fun handlePdfLongPress(
        pageIndex: Int,
        bitmapX: Float,
        bitmapY: Float
    ) {
        val activeSession = session ?: return
        val currentViewport = viewport ?: return

        scope.launch {
            val selection = withContext(Dispatchers.IO) {
                activeSession.findWordAtBitmapPoint(
                    pageIndex = pageIndex,
                    bitmapX = bitmapX,
                    bitmapY = bitmapY,
                    viewport = currentViewport,
                    zoomScale = committedZoom
                )
            }

            if (selection != null) {
                selectionState = PdfSelectionState(
                    selectedText = selection.word,
                    paragraphText = selection.lineText,
                    bounds = PdfSelectionBounds(
                        pageIndex = pageIndex,
                        left = selection.left,
                        top = selection.top,
                        right = selection.right,
                        bottom = selection.bottom
                    )
                )
                currentOnTextSelected(
                    ReaderEvent.OnTextSelected(
                        selectedText = selection.word,
                        paragraphText = selection.lineText
                    )
                )
            }
        }
    }

    LaunchedEffect(book.id) {
        isLoading = true
        errorMessage = null
        pageBitmaps.values.forEach(Bitmap::recycle)
        pageBitmaps.clear()
        pageTiles.clear()
        activeBitmapVersions.clear()
        pageGeometries.clear()
        pageCropBounds.clear()
        pageLayouts.clear()
        committedZoom = 1f
        committedPanX = 0f
        committedPanY = 0f
        transientZoom = PdfTransientZoom()
        verticalScrollPx = 0f
        pagedIndex = initialPage

        try {
            withContext(Dispatchers.IO) {
                val cachedFile = CachedFileFactory.fromBook(context, book) ?: run {
                    errorMessage = "Failed to access PDF file"
                    return@withContext emptyList<PdfPageLayout>()
                }
                val rawFile = cachedFile.rawFile ?: run {
                    errorMessage = "Failed to cache PDF file"
                    return@withContext emptyList<PdfPageLayout>()
                }
                val newSession = PdfDocumentSession.open(rawFile.absolutePath)
                val pageCount = newSession.countPages()
                val geometries = mutableMapOf<Int, PdfPageGeometry>()
                val crops = mutableMapOf<Int, PdfCropBounds>()
                val layouts = mutableListOf<PdfPageLayout>()
                for (index in 0 until pageCount) {
                    val geometry = newSession.loadPageGeometry(index)
                    val crop = newSession.estimateCropBounds(index)
                    geometries[index] = geometry
                    crops[index] = crop
                    viewport?.let { currentViewport ->
                        layouts += geometry.createPageLayout(
                            viewport = currentViewport,
                            zoomScale = committedZoom,
                            fitWidth = isVertical,
                            cropLeftRatio = crop.leftRatio,
                            cropTopRatio = crop.topRatio,
                            cropRightRatio = crop.rightRatio,
                            cropBottomRatio = crop.bottomRatio
                        )
                    }
                }
                withContext(Dispatchers.Main) {
                    session = newSession
                    renderController = PdfRenderController(newSession)
                    totalPages = pageCount
                    pageGeometries.putAll(geometries)
                    pageCropBounds.putAll(crops)
                    layouts.forEach { pageLayouts[it.pageIndex] = it }
                    Log.d(TAG, "Prepared PDF session for '${book.title}' with $pageCount pages and ${layouts.size} layouts")
                    onTotalPagesLoaded(pageCount)
                    onLoadingComplete()
                    onScrollRestorationComplete()
                }
                layouts
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to open PDF", e)
            errorMessage = "Failed to load PDF: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(viewport, totalPages, committedZoom, committedPanX, committedPanY, verticalScrollPx, isVertical, pagedIndex) {
        val currentViewport = viewport ?: return@LaunchedEffect
        if (totalPages <= 0) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            if (isVertical) {
                val pages = buildVerticalPages()
                val preloadMargin = currentViewport.heightPx.toFloat()
                val visiblePages = pages.filter {
                    it.topPx + it.heightPx >= verticalScrollPx - preloadMargin &&
                        it.topPx <= verticalScrollPx + currentViewport.heightPx + preloadMargin
                }
                visiblePages.forEach { renderPage(it.pageIndex) }
                if (visiblePages.isNotEmpty() && !transientZoom.active) {
                    val viewportCenter = verticalScrollPx + currentViewport.heightPx / 2f
                    val currentVisible = visiblePages.minByOrNull { abs((it.topPx + it.heightPx / 2f) - viewportCenter) }?.pageIndex
                    if (currentVisible != null && currentVisible != currentPage) {
                        withContext(Dispatchers.Main) {
                            onPageChanged(currentVisible)
                            onPageSelected(currentVisible)
                        }
                    }
                }
            } else {
                listOf(
                    pagedIndex,
                    pagedIndex - 1,
                    pagedIndex + 1,
                    pagedIndex - 2,
                    pagedIndex + 2
                )
                    .filter { it in 0 until totalPages }
                    .distinct()
                    .forEach { renderPage(it) }
            }
        }
    }

    DisposableEffect(book.id) {
        onDispose {
            session?.close()
            session = null
            renderController?.clear()
            renderController = null
            pageBitmaps.values.forEach(Bitmap::recycle)
            pageBitmaps.clear()
            pageTiles.clear()
            activeBitmapVersions.clear()
            pageStates.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    viewport = PdfViewport(size.width, size.height)
                }
            }
    ) {
        SideEffect {
            Log.d(
                TAG,
                "render-branch: isLoading=$isLoading error=${errorMessage != null} totalPages=$totalPages viewportReady=${viewport != null} activePagedBitmap=${pageBitmaps[pagedIndex] != null}"
            )
        }

        when {
            errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            totalPages <= 0 || viewport == null -> Box(modifier = Modifier.fillMaxSize())

            else -> {
                if (isVertical) {
                    val pages = buildVerticalPages()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = effectiveZoom,
                                scaleY = effectiveZoom,
                                translationX = effectivePanX,
                                translationY = effectivePanY
                            )
                            .pdfGestureInterop(
                                PdfGestureCallbacks(
                                    onTap = { _, _ -> onMenuToggle() },
                                    onLongPress = { x, y ->
                                        val vpW = viewport?.widthPx?.toFloat() ?: 0f
                                        val vpH = viewport?.heightPx?.toFloat() ?: 0f
                                        val effZoom = committedZoom * transientZoom.scale
                                        val effPanX = committedPanX + transientZoom.panX
                                        val effPanY = committedPanY + transientZoom.panY
                                        // Inverse graphicsLayer(TransformOrigin.Center) on the
                                        // fillMaxSize outer Box to get Box-local coordinates.
                                        val localX = (x - effPanX - vpW / 2f) / effZoom + vpW / 2f
                                        val localY = (y - effPanY - vpH / 2f) / effZoom + vpH / 2f
                                        val pages = buildVerticalPages()
                                        val targetPage = pages.firstOrNull { page ->
                                            localX >= page.leftPx &&
                                            localX < page.leftPx + page.widthPx &&
                                            localY >= page.topPx - verticalScrollPx &&
                                            localY < page.topPx - verticalScrollPx + page.heightPx
                                        } ?: pages.firstOrNull { it.pageIndex == currentPage }
                                        if (targetPage != null) {
                                            val pageLocalX = localX - targetPage.leftPx
                                            val pageLocalY = localY - (targetPage.topPx - verticalScrollPx)
                                            handlePdfLongPress(
                                                targetPage.pageIndex,
                                                pageLocalX * committedZoom,
                                                pageLocalY * committedZoom
                                            )
                                        }
                                    },
                                    onZoomPan = { zoomFactor, panX, panY ->
                                        val nextScale = (transientZoom.scale * zoomFactor).coerceIn(1f / committedZoom, 5f / committedZoom)
                                        val notZoomed = nextScale <= 1.02f / committedZoom
                                        val deltaX: Float
                                        val deltaY: Float
                                        if (zoomFactor != 1f) {
                                            val focusX = interopState.lastScaleFocusX
                                            val focusY = interopState.lastScaleFocusY
                                            val vpW = viewport?.widthPx?.toFloat() ?: 0f
                                            val vpH = viewport?.heightPx?.toFloat() ?: 0f
                                            val currentEffPanX = committedPanX + transientZoom.panX
                                            val currentEffPanY = committedPanY + transientZoom.panY
                                            // Correct focal-point formula for graphicsLayer with
                                            // TransformOrigin.Center: keep the pinch centroid
                                            // stationary on screen as the scale changes.
                                            deltaX = (zoomFactor - 1f) * (currentEffPanX - focusX + vpW / 2f)
                                            deltaY = (zoomFactor - 1f) * (currentEffPanY - focusY + vpH / 2f)
                                        } else {
                                            deltaX = panX
                                            deltaY = panY
                                        }
                                        transientZoom = PdfTransientZoom(
                                            scale = nextScale,
                                            panX = transientZoom.panX + if (notZoomed) 0f else deltaX,
                                            panY = transientZoom.panY + if (notZoomed) 0f else deltaY,
                                            active = true
                                        )
                                        if (effectiveZoom <= 1.02f && abs(panY) > 0f) {
                                            val maxScroll = max(
                                                0f,
                                                (buildVerticalPages().lastOrNull()?.let { it.topPx + it.heightPx } ?: 0f) - (viewport?.heightPx ?: 0)
                                            )
                                            verticalScrollPx = (verticalScrollPx - panY).coerceIn(0f, maxScroll)
                                        }
                                    },
                                    onGestureEnd = {
                                        val committedScale = (committedZoom * transientZoom.scale).coerceIn(1f, 5f)
                                        val (panX, panY) = clampPan(
                                            pageIndex = currentPage,
                                            zoom = committedScale,
                                            panX = committedPanX + transientZoom.panX,
                                            panY = committedPanY + transientZoom.panY
                                        )
                                        committedZoom = committedScale
                                        committedPanX = panX
                                        committedPanY = panY
                                        transientZoom = PdfTransientZoom()
                                    }
                                )
                            )
                    ) {
                        // Derive the visible content range in un-zoomed content
                        // coordinates from the graphicsLayer transform
                        // (TransformOrigin.Center + translation).
                        //
                        // For a container of height vpH scaled by `effectiveZoom`
                        // around its centre with translationY = effectivePanY, a
                        // page at local offset `y` (= topPx - verticalScrollPx) is
                        // on-screen when:
                        //
                        //   screenY = (y - vpH/2) * zoom + vpH/2 + panY  ∈ [0, vpH]
                        //
                        // Solving for y gives the visible content band below.
                        val vpH = viewport!!.heightPx.toFloat()
                        val halfVp = vpH / 2f
                        val minVisible = verticalScrollPx + halfVp - (halfVp + effectivePanY) / effectiveZoom
                        val maxVisible = verticalScrollPx + halfVp + (halfVp - effectivePanY) / effectiveZoom
                        pages.filter {
                            it.topPx + it.heightPx >= minVisible && it.topPx <= maxVisible
                        }.forEach { page ->
                            PdfPageSurface(
                                pageIndex = page.pageIndex,
                                pageBitmap = pageBitmaps[page.pageIndex],
                                tiles = pageTiles[page.pageIndex].orEmpty(),
                                bitmapVersion = activeBitmapVersions[page.pageIndex] ?: 0,
                                pageLayout = pageLayouts[page.pageIndex],
                                selectionState = selectionState,
                                uiScale = 1f,
                                uiPanX = 0f,
                                uiPanY = 0f,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = page.leftPx.toInt(),
                                            y = (page.topPx - verticalScrollPx).toInt()
                                        )
                                    }
                                    .size(
                                        width = with(density) { page.widthPx.toDp() },
                                        height = with(density) { page.heightPx.toDp() }
                                    ),
                                contentWidthPx = page.widthPx,
                                contentHeightPx = page.heightPx,
                                colorFilter = imageColorFilter,
                                onTap = { _, _ -> onMenuToggle() },
                                onLongPress = { _, _ -> }
                            )
                        }
                    }
                } else {
                    val pagerState = rememberPagerState(
                        initialPage = pagedIndex,
                        pageCount = { totalPages }
                    )

                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage != pagedIndex) {
                            pagedIndex = pagerState.currentPage
                            // Reset zoom so each new page starts unzoomed.
                            committedZoom = 1f
                            committedPanX = 0f
                            committedPanY = 0f
                            transientZoom = PdfTransientZoom()
                            onPageChanged(pagerState.currentPage)
                            onPageSelected(pagerState.currentPage)
                        }
                    }

                    LaunchedEffect(pagedIndex) {
                        if (pagerState.currentPage != pagedIndex) {
                            pagerState.animateScrollToPage(pagedIndex)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            userScrollEnabled = committedZoom <= 1.02f,
                            beyondViewportPageCount = 1
                        ) { pageIndex ->
                            val pageLayout = pageLayouts[pageIndex]
                            if (pageLayout != null) {
                                LaunchedEffect(pageIndex) {
                                    renderPage(pageIndex)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(backgroundColor)
                                ) {
                                    // Only apply zoom/pan to the current page; adjacent
                                    // pages visible during swipe animation stay at 1:1.
                                    val isCurrentPage = pageIndex == pagedIndex
                                    PdfPageSurface(
                                        pageIndex = pageIndex,
                                        pageBitmap = pageBitmaps[pageIndex],
                                        tiles = pageTiles[pageIndex].orEmpty(),
                                        bitmapVersion = activeBitmapVersions[pageIndex] ?: 0,
                                        pageLayout = pageLayout,
                                        selectionState = selectionState,
                                        uiScale = if (isCurrentPage) effectiveZoom else 1f,
                                        uiPanX = if (isCurrentPage) effectivePanX else 0f,
                                        uiPanY = if (isCurrentPage) effectivePanY else 0f,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .padding(horizontal = 2.dp)
                                            .then(
                                                if (isCurrentPage) {
                                                    Modifier.pagedZoomPanGesture(
                                                        isZoomed = { effectiveZoom > 1.02f },
                                                        onZoomPan = { zoomDelta, panDelta ->
                                                            val newScale = (transientZoom.scale * zoomDelta).coerceIn(1f / committedZoom, 5f / committedZoom)
                                                            // Check effective zoom (committed * transient) not just transient,
                                                            // so pan is preserved when zooming out from a committed zoom > 1.
                                                            val newEffectiveZoom = committedZoom * newScale
                                                            transientZoom = PdfTransientZoom(
                                                                scale = newScale,
                                                                panX = if (newEffectiveZoom > 1.02f) transientZoom.panX + panDelta.x else 0f,
                                                                panY = if (newEffectiveZoom > 1.02f) transientZoom.panY + panDelta.y else 0f,
                                                                active = true
                                                            )
                                                        },
                                                        onGestureEnd = {
                                                            val committedScale = (committedZoom * transientZoom.scale).coerceIn(1f, 5f)
                                                            val (newPanX, newPanY) = clampPan(
                                                                pageIndex = pagedIndex,
                                                                zoom = committedScale,
                                                                panX = committedPanX + transientZoom.panX,
                                                                panY = committedPanY + transientZoom.panY
                                                            )
                                                            committedZoom = committedScale
                                                            committedPanX = newPanX
                                                            committedPanY = newPanY
                                                            transientZoom = PdfTransientZoom()
                                                        }
                                                    )
                                                } else Modifier
                                            ),
                                        contentWidthPx = pageLayout.displayWidthPx,
                                        contentHeightPx = pageLayout.displayHeightPx,
                                        colorFilter = imageColorFilter,
                                        onTap = { tapX, width ->
                                            when {
                                                effectiveZoom <= 1.02f && tapX < width * 0.2f && pageIndex > 0 -> {
                                                    scope.launch { pagerState.animateScrollToPage(pageIndex - 1) }
                                                }
                                                effectiveZoom <= 1.02f && tapX > width * 0.8f && pageIndex < totalPages - 1 -> {
                                                    scope.launch { pagerState.animateScrollToPage(pageIndex + 1) }
                                                }
                                                else -> onMenuToggle()
                                            }
                                        },
                                        onLongPress = { x, y ->
                                            // Only handle for the current page; adjacent pages
                                            // shown during swipe animation stay interactive but
                                            // should not trigger selection.
                                            if (isCurrentPage) {
                                                val dw = pageLayout.displayWidthPx
                                                val dh = pageLayout.displayHeightPx
                                                val effZoom = committedZoom * transientZoom.scale
                                                val effPanX = committedPanX + transientZoom.panX
                                                val effPanY = committedPanY + transientZoom.panY
                                                // (x,y) are in PdfPageSurface outer-Box layout coords.
                                                // Invert the inner content-Box graphicsLayer transform
                                                // (offset+scale around center) to get page display
                                                // coords, then scale to bitmap pixels.
                                                val lx = (x - dw / 2f - effPanX) / effZoom + dw / 2f
                                                val ly = (y - dh / 2f - effPanY) / effZoom + dh / 2f
                                                handlePdfLongPress(pageIndex, lx * committedZoom, ly * committedZoom)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showPageIndicator) {
                    PageIndicator(
                        currentPage = if (isVertical) currentPage else pagedIndex,
                        totalPages = totalPages,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

/**
 * Handles zoom and pan gestures for paged PDF reading using PointerEventPass.Initial
 * so events are intercepted before HorizontalPager's scrollable (which uses Main pass).
 *
 * - Pinch: consumed, triggers zoom
 * - Single-finger drag when already zoomed: consumed, triggers pan
 * - Single-finger drag when not zoomed: passes through to HorizontalPager for page navigation
 */
private fun Modifier.pagedZoomPanGesture(
    isZoomed: () -> Boolean,
    onZoomPan: (zoomDelta: Float, panDelta: Offset) -> Unit,
    onGestureEnd: () -> Unit
): Modifier = this.pointerInput(Unit) {
    awaitEachGesture {
        var gestureActive = false
        val down = awaitFirstDown(requireUnconsumed = false)
        var lastCentroid = Offset.Zero
        var lastSpan = 0f
        var isPinchGesture = false
        // Separate position tracker for single-finger pan, initialised when we
        // first enter single-finger mode to avoid a jump from the initial DOWN.
        var lastSinglePosition: Offset? = null

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pointers = event.changes.filter { it.pressed }
            val count = pointers.size

            when {
                count >= 2 -> {
                    // Two-finger pinch — consume and handle zoom
                    lastSinglePosition = null  // reset single-finger tracker on re-pinch
                    if (!isPinchGesture) {
                        isPinchGesture = true
                        gestureActive = true
                        val p1 = pointers[0].position
                        val p2 = pointers[1].position
                        lastCentroid = (p1 + p2) / 2f
                        lastSpan = (p1 - p2).getDistance()
                    }
                    val p1 = pointers[0].position
                    val p2 = pointers[1].position
                    val centroid = (p1 + p2) / 2f
                    val span = (p1 - p2).getDistance()
                    val zoomDelta = if (lastSpan > 0) span / lastSpan else 1f
                    val panDelta = centroid - lastCentroid
                    onZoomPan(zoomDelta, panDelta)
                    lastCentroid = centroid
                    lastSpan = span
                    pointers.forEach { it.consume() }
                }
                count == 1 && (isPinchGesture || isZoomed()) -> {
                    // Single finger after pinch, or single finger on an already-zoomed page: pan
                    val pointer = pointers[0]
                    val prev = lastSinglePosition ?: pointer.position  // first frame: zero delta
                    lastSinglePosition = pointer.position
                    val delta = pointer.position - prev
                    if (delta != Offset.Zero) {
                        gestureActive = true
                        onZoomPan(1f, delta)
                    }
                    pointer.consume()
                }
                count == 1 && !isPinchGesture -> {
                    // Single finger, not zoomed, no pinch started — let HorizontalPager handle
                    break
                }
                else -> break
            }

            if (event.changes.none { it.pressed }) break
        }

        if (gestureActive) onGestureEnd()
    }
}

@Composable
private fun PdfPageSurface(
    pageIndex: Int,
    pageBitmap: Bitmap?,
    tiles: List<PdfTileBitmap>,
    bitmapVersion: Int,
    pageLayout: PdfPageLayout?,
    selectionState: PdfSelectionState,
    uiScale: Float,
    uiPanX: Float,
    uiPanY: Float,
    modifier: Modifier,
    contentWidthPx: Float?,
    contentHeightPx: Float?,
    colorFilter: ColorFilter?,
    onTap: (tapX: Float, width: Float) -> Unit,
    onLongPress: (tapX: Float, tapY: Float) -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .pointerInput(onTap, onLongPress) {
                detectTapGestures(
                    onTap = { offset ->
                        onTap(offset.x, size.width.toFloat())
                    },
                    onLongPress = { offset ->
                        onLongPress(offset.x, offset.y)
                    }
                )
            }
    ) {
        val bitmap = pageBitmap
        if (bitmap != null && pageLayout != null) {
            val imageBitmap = remember(bitmapVersion) { bitmap.asImageBitmap() }
            val contentOffsetX = uiPanX
            val contentOffsetY = uiPanY
            Box(
                modifier = Modifier
                    .offset { IntOffset(contentOffsetX.toInt(), contentOffsetY.toInt()) }
                    .width(with(density) { contentWidthPx!!.toDp() })
                    .height(with(density) { contentHeightPx!!.toDp() })
                    .graphicsLayer(
                        scaleX = uiScale,
                        scaleY = uiScale,
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center,
                        clip = true
                    )
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = colorFilter,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val widthScale = if (pageLayout.displayWidthPx == 0f) 1f else size.width / pageLayout.displayWidthPx
                    val heightScale = if (pageLayout.displayHeightPx == 0f) 1f else size.height / pageLayout.displayHeightPx
                    tiles.forEach { tile ->
                        val image = tile.bitmap.asImageBitmap()
                        drawImage(
                            image = image,
                            dstOffset = IntOffset(
                                x = (tile.request.tileRect.leftPx * widthScale).toInt(),
                                y = (tile.request.tileRect.topPx * heightScale).toInt()
                            ),
                            dstSize = IntSize(
                                width = (tile.request.tileRect.widthPx * widthScale).toInt().coerceAtLeast(1),
                                height = (tile.request.tileRect.heightPx * heightScale).toInt().coerceAtLeast(1)
                            ),
                            colorFilter = colorFilter
                        )
                    }

                    if (selectionState.bounds?.pageIndex == pageIndex) {
                        val bounds = selectionState.bounds ?: return@Canvas
                        drawRect(
                            color = Color(0x6633B5E5),
                            topLeft = Offset(bounds.left * widthScale, bounds.top * heightScale),
                            size = Size(
                                width = ((bounds.right - bounds.left) * widthScale).coerceAtLeast(1f),
                                height = ((bounds.bottom - bounds.top) * heightScale).coerceAtLeast(1f)
                            )
                        )
                    }
                }
            }
        } else {
            Text(
                text = "Loading PDF page...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}
