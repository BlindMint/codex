package us.blindmint.codex.domain.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

class PagePreloader(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val preloadSize: Int = 5,
    private val maxQueueSize: Int = 20
) {
    private val queue = java.util.concurrent.PriorityBlockingQueue<PriorityPage>()
    private val _queueSize = MutableStateFlow(0)
    val queueSizeFlow: StateFlow<Int> = _queueSize.asStateFlow()

    init {
        scope.launch {
            while (true) {
                val priorityPage = withContext(Dispatchers.IO) { queue.take() }
                processPage(priorityPage)
                _queueSize.value = queue.size
            }
        }
    }

    private fun processPage(priorityPage: PriorityPage) {
        val page = priorityPage.page
        when (page.state) {
            is ReaderPageState.Error -> {
                page.state = ReaderPageState.Queued
                queueIfNotPresent(page, priorityPage.priority)
            }
            is ReaderPageState.Queued -> {
                queueIfNotPresent(page, priorityPage.priority)
            }
            else -> {}
        }
    }

    private fun queueIfNotPresent(page: ReaderPage, priority: Int) {
        val entry = PriorityPage(page, priority)
        if (!queue.contains(entry)) {
            queue.offer(entry)
            _queueSize.value = queue.size
        }
    }

    fun enqueuePage(page: ReaderPage, priority: Int = 1) {
        when (page.state) {
            is ReaderPageState.Queued -> queueIfNotPresent(page, priority)
            is ReaderPageState.Error -> {
                page.state = ReaderPageState.Queued
                queueIfNotPresent(page, priority)
            }
            else -> {}
        }
    }

    fun enqueuePreload(currentPage: Int, totalPages: Int, pageLoader: suspend (Int) -> Unit) {
        if (currentPage >= totalPages - 1) return

        val startIndex = currentPage + 1
        val endIndex = min(currentPage + 1 + preloadSize, totalPages)

        for (i in startIndex until endIndex) {
            val page = ReaderPage(i)
            enqueuePage(page, priority = 0)
            scope.launch {
                queue.take()
                pageLoader(i)
            }
        }
    }

    fun preloadAround(
        currentPage: Int,
        totalPages: Int,
        isRTL: Boolean = false,
        pageLoader: suspend (Int) -> Unit
    ) {
        if (totalPages <= 0) return

        val preloadRange = preloadSize
        val current = if (isRTL) totalPages - 1 - currentPage else currentPage

        val startIndex = maxOf(0, current - preloadRange)
        val endIndex = minOf(totalPages - 1, current + preloadRange)

        for (i in startIndex..endIndex) {
            if (i == current) continue
            val logicalPage = if (isRTL) totalPages - 1 - i else i
            scope.launch {
                pageLoader(logicalPage)
            }
        }
    }

    fun retryPage(page: ReaderPage) {
        if (page.state is ReaderPageState.Error) {
            page.state = ReaderPageState.Queued
            queue.offer(PriorityPage(page, priority = 2))
            _queueSize.value = queue.size
        }
    }

    fun clear() {
        queue.clear()
        _queueSize.value = 0
    }

    private class PriorityPage(
        val page: ReaderPage,
        val priority: Int
    ) : Comparable<PriorityPage> {
        companion object {
            private val idGenerator = AtomicInteger(0)
        }

        private val identifier = idGenerator.incrementAndGet()

        override fun compareTo(other: PriorityPage): Int {
            val p = other.priority.compareTo(priority)
            return if (p != 0) p else identifier.compareTo(other.identifier)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is PriorityPage) return false
            return page.index == other.page.index
        }

        override fun hashCode(): Int {
            return page.index.hashCode()
        }
    }
}

sealed class PreloadDirection {
    data object Forward : PreloadDirection()
    data object Backward : PreloadDirection()
    data object Both : PreloadDirection()
}

data class PreloadRequest(
    val pageIndex: Int,
    val priority: Int = 0,
    val direction: PreloadDirection = PreloadDirection.Both
)
