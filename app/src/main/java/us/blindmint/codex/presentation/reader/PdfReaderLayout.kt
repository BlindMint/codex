/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
    var pagedSwipeAccumulatedX by remember { mutableFloatStateOf(0f) }
    var selectionState by remember { mutableStateOf(PdfSelectionState()) }
    var lastReadingDirection by remember { mutableStateOf(readingDirection) }
    val pagedTransitionOffset = remember { Animatable(0f) }
    val pagedOutgoingOffset = remember { Animatable(0f) }
    var transitionFromIndex by remember { mutableStateOf<Int?>(null) }

    val pageGeometries = remember { mutableStateMapOf<Int, PdfPageGeometry>() }
    val pageCropBounds = remember { mutableStateMapOf<Int, PdfCropBounds>() }
    val pageLayouts = remember { mutableStateMapOf<Int, PdfPageLayout>() }
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    val pageTiles = remember { mutableStateMapOf<Int, List<PdfTileBitmap>>() }
    val activeBitmapVersions = remember { mutableStateMapOf<Int, Int>() }

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

    SideEffect {
        Log.d(
            TAG,
            "state: isLoading=$isLoading totalPages=$totalPages viewport=$viewport pagedIndex=$pagedIndex currentPage=$currentPage hasSession=${session != null} hasController=${renderController != null} layouts=${pageLayouts.size} bitmaps=${pageBitmaps.size}"
        )
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
        val activeController = renderController ?: return
        val currentViewport = viewport ?: return
        val layout = pageLayouts[pageIndex] ?: updatePageLayout(pageIndex, zoom)
        val result = activeController.render(
            PdfRenderRequest(
                pageIndex = pageIndex,
                viewport = currentViewport,
                zoomScale = zoom,
                invertColors = invertColors
            )
        )
        if (layout != null) {
            pageTiles[pageIndex] = activeController.getVisibleTiles(
                layout.visibleTileRequests(
                    viewport = currentViewport,
                    zoomScale = zoom,
                    panX = panX,
                    panY = panY,
                    invertColors = invertColors
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
    }

    fun clampPan(pageIndex: Int, zoom: Float, panX: Float, panY: Float): Pair<Float, Float> {
        val layout = pageLayouts[pageIndex] ?: return 0f to 0f
        val currentViewport = viewport ?: return 0f to 0f
        if (zoom <= 1.02f) return 0f to 0f
        val maxPanX = max(0f, (layout.displayWidthPx - currentViewport.widthPx) / 2f)
        val maxPanY = max(0f, (layout.displayHeightPx - currentViewport.heightPx) / 2f)
        return panX.coerceIn(-maxPanX, maxPanX) to panY.coerceIn(-maxPanY, maxPanY)
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
        transitionFromIndex = null

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
                val preloadMargin = currentViewport.heightPx.toFloat()
                val pages = buildVerticalPages().filter {
                    it.topPx + it.heightPx >= verticalScrollPx - preloadMargin &&
                        it.topPx <= verticalScrollPx + currentViewport.heightPx + preloadMargin
                }
                pages.forEach { renderPage(it.pageIndex) }
                if (pages.isNotEmpty() && !transientZoom.active) {
                    val viewportCenter = verticalScrollPx + currentViewport.heightPx / 2f
                    val currentVisible = pages.minByOrNull { abs((it.topPx + it.heightPx / 2f) - viewportCenter) }?.pageIndex
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
            .pointerInput(isVertical, pagedIndex, totalPages) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (transientZoom.scale * zoom).coerceIn(0.75f / committedZoom, 5f / committedZoom)
                    transientZoom = PdfTransientZoom(
                        scale = nextScale,
                        panX = transientZoom.panX + if (!isVertical && nextScale <= 1.02f / committedZoom) 0f else pan.x,
                        panY = transientZoom.panY + pan.y,
                        active = true
                    )
                }
            }
            .pointerInput(isVertical, effectiveZoom) {
                detectDragGestures(
                    onDragEnd = {
                        val committedScale = (committedZoom * transientZoom.scale).coerceIn(0.75f, 5f)
                        val targetPage = if (isVertical) currentPage else pagedIndex
                        val (panX, panY) = clampPan(
                            pageIndex = targetPage,
                            zoom = committedScale,
                            panX = committedPanX + transientZoom.panX,
                            panY = committedPanY + transientZoom.panY
                        )
                        committedZoom = committedScale
                        committedPanX = panX
                        committedPanY = panY
                        if (!isVertical && committedScale <= 1.02f) {
                            val nextIndex = when {
                                pagedSwipeAccumulatedX > 140f && pagedIndex > 0 -> pagedIndex - 1
                                pagedSwipeAccumulatedX < -140f && pagedIndex < totalPages - 1 -> pagedIndex + 1
                                else -> pagedIndex
                            }
                            if (nextIndex != pagedIndex) {
                                // Positive direction → new page enters from the right (next page, LTR forward)
                                val direction = if (nextIndex > pagedIndex) 1f else -1f
                                val fromIndex = pagedIndex
                                pagedIndex = nextIndex
                                onPageChanged(nextIndex)
                                onPageSelected(nextIndex)
                                transitionFromIndex = fromIndex
                                scope.launch {
                                    val vpWidth = viewport?.widthPx?.toFloat() ?: 400f
                                    pagedOutgoingOffset.snapTo(0f)
                                    pagedTransitionOffset.snapTo(direction * vpWidth)
                                    launch {
                                        pagedOutgoingOffset.animateTo(
                                            -direction * vpWidth,
                                            tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    pagedTransitionOffset.animateTo(
                                        0f,
                                        tween(durationMillis = 200, easing = FastOutSlowInEasing)
                                    )
                                    transitionFromIndex = null
                                }
                            }
                        }
                        pagedSwipeAccumulatedX = 0f
                        transientZoom = PdfTransientZoom()
                    },
                    onDragCancel = {
                        pagedSwipeAccumulatedX = 0f
                        transientZoom = PdfTransientZoom()
                    }
                ) { change, dragAmount ->
                    change.consume()
                    if (effectiveZoom > 1.02f) {
                        transientZoom = transientZoom.copy(
                            panX = transientZoom.panX + if (isVertical) 0f else dragAmount.x,
                            panY = transientZoom.panY + dragAmount.y,
                            active = true
                        )
                    } else if (isVertical) {
                        val maxScroll = max(
                            0f,
                            (buildVerticalPages().lastOrNull()?.let { it.topPx + it.heightPx } ?: 0f) - (viewport?.heightPx ?: 0)
                        )
                        verticalScrollPx = (verticalScrollPx - dragAmount.y).coerceIn(0f, maxScroll)
                    } else {
                        pagedSwipeAccumulatedX += dragAmount.x
                    }
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
                    pages.filter {
                        it.topPx + it.heightPx >= verticalScrollPx && it.topPx <= verticalScrollPx + viewport!!.heightPx
                    }.forEach { page ->
                        PdfPageSurface(
                            pageIndex = page.pageIndex,
                            pageBitmap = pageBitmaps[page.pageIndex],
                            tiles = pageTiles[page.pageIndex].orEmpty(),
                            bitmapVersion = activeBitmapVersions[page.pageIndex] ?: 0,
                            pageLayout = pageLayouts[page.pageIndex],
                            selectionState = selectionState,
                            uiScale = effectiveZoom,
                            uiPanX = effectivePanX,
                            uiPanY = effectivePanY,
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
                            onTap = { _, _ -> onMenuToggle() }
                        )
                    }
                } else {
                    val pageLayout = pageLayouts[pagedIndex]
                    val fromIdx = transitionFromIndex
                    val fromLayout = if (fromIdx != null) pageLayouts[fromIdx] else null
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                    ) {
                        // Outgoing page slides out behind (rendered first = lower z-order)
                        if (fromIdx != null && fromLayout != null) {
                            PdfPageSurface(
                                pageIndex = fromIdx,
                                pageBitmap = pageBitmaps[fromIdx],
                                tiles = pageTiles[fromIdx].orEmpty(),
                                bitmapVersion = activeBitmapVersions[fromIdx] ?: 0,
                                pageLayout = fromLayout,
                                selectionState = selectionState,
                                uiScale = effectiveZoom,
                                uiPanX = effectivePanX + pagedOutgoingOffset.value,
                                uiPanY = effectivePanY,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            fromLayout.contentLeftPx.toInt(),
                                            fromLayout.contentTopPx.toInt()
                                        )
                                    }
                                    .size(
                                        width = with(density) { fromLayout.displayWidthPx.toDp() },
                                        height = with(density) { fromLayout.displayHeightPx.toDp() }
                                    ),
                                contentWidthPx = fromLayout.displayWidthPx,
                                contentHeightPx = fromLayout.displayHeightPx,
                                colorFilter = imageColorFilter,
                                onTap = { _, _ -> } // ignore taps on outgoing page
                            )
                        }
                        // Incoming (current) page slides in on top
                        if (pageLayout != null) {
                            PdfPageSurface(
                                pageIndex = pagedIndex,
                                pageBitmap = pageBitmaps[pagedIndex],
                                tiles = pageTiles[pagedIndex].orEmpty(),
                                bitmapVersion = activeBitmapVersions[pagedIndex] ?: 0,
                                pageLayout = pageLayout,
                                selectionState = selectionState,
                                uiScale = effectiveZoom,
                                uiPanX = effectivePanX + pagedTransitionOffset.value,
                                uiPanY = effectivePanY,
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            pageLayout.contentLeftPx.toInt(),
                                            pageLayout.contentTopPx.toInt()
                                        )
                                    }
                                    .size(
                                        width = with(density) { pageLayout.displayWidthPx.toDp() },
                                        height = with(density) { pageLayout.displayHeightPx.toDp() }
                                    ),
                                contentWidthPx = pageLayout.displayWidthPx,
                                contentHeightPx = pageLayout.displayHeightPx,
                                colorFilter = imageColorFilter,
                                onTap = { tapX, width ->
                                    when {
                                        effectiveZoom <= 1.02f && tapX < width * 0.2f && pagedIndex > 0 -> {
                                            val next = pagedIndex - 1
                                            val from = pagedIndex
                                            pagedIndex = next
                                            onPageChanged(next)
                                            onPageSelected(next)
                                            transitionFromIndex = from
                                            scope.launch {
                                                val vpWidth = viewport?.widthPx?.toFloat() ?: 400f
                                                pagedOutgoingOffset.snapTo(0f)
                                                pagedTransitionOffset.snapTo(-vpWidth)
                                                launch {
                                                    pagedOutgoingOffset.animateTo(
                                                        vpWidth,
                                                        tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                                pagedTransitionOffset.animateTo(
                                                    0f,
                                                    tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                                )
                                                transitionFromIndex = null
                                            }
                                        }

                                        effectiveZoom <= 1.02f && tapX > width * 0.8f && pagedIndex < totalPages - 1 -> {
                                            val next = pagedIndex + 1
                                            val from = pagedIndex
                                            pagedIndex = next
                                            onPageChanged(next)
                                            onPageSelected(next)
                                            transitionFromIndex = from
                                            scope.launch {
                                                val vpWidth = viewport?.widthPx?.toFloat() ?: 400f
                                                pagedOutgoingOffset.snapTo(0f)
                                                pagedTransitionOffset.snapTo(vpWidth)
                                                launch {
                                                    pagedOutgoingOffset.animateTo(
                                                        -vpWidth,
                                                        tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                                    )
                                                }
                                                pagedTransitionOffset.animateTo(
                                                    0f,
                                                    tween(durationMillis = 180, easing = FastOutSlowInEasing)
                                                )
                                                transitionFromIndex = null
                                            }
                                        }

                                        else -> onMenuToggle()
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = "No page layout for index $pagedIndex",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                if (showPageIndicator) {
                    ComicPageIndicator(
                        currentPage = if (isVertical) currentPage else pagedIndex,
                        totalPages = totalPages,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
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
    onTap: (tapX: Float, width: Float) -> Unit
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .pointerInput(onTap) {
                detectTapGestures { offset ->
                    onTap(offset.x, size.width.toFloat())
                }
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
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
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
                            )
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
