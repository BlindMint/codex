package us.blindmint.codex.domain.reader

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ReaderPageState {
    data object Queued : ReaderPageState
    data object Loading : ReaderPageState
    data class Downloading(val progress: Int) : ReaderPageState
    data object Ready : ReaderPageState
    data class Error(val message: String, val throwable: Throwable? = null) : ReaderPageState
}

class StateHolder<T : Any>(
    initialState: T,
    private val onStateChanged: ((T) -> Unit)? = null
) {
    private val _stateFlow = MutableStateFlow(initialState)
    val stateFlow: StateFlow<T> = _stateFlow.asStateFlow()

    var currentState: T
        get() = _stateFlow.value
        set(value) {
            _stateFlow.value = value
            onStateChanged?.invoke(value)
        }

    fun update(reducer: (T) -> T) {
        currentState = reducer(currentState)
    }
}

class ReaderPage(
    val index: Int,
    val url: String = "",
    var imageUrl: String? = null
) {
    val number: Int
        get() = index + 1

    private val _stateHolder = StateHolder<ReaderPageState>(ReaderPageState.Queued)
    val stateFlow: StateFlow<ReaderPageState> = _stateHolder.stateFlow

    var state: ReaderPageState
        get() = _stateHolder.currentState
        set(value) {
            _stateHolder.currentState = value
        }

    private val _progressFlow = MutableStateFlow(0)
    val progressFlow: StateFlow<Int> = _progressFlow.asStateFlow()

    var progress: Int
        get() = _progressFlow.value
        set(value) {
            _progressFlow.value = value
        }

    fun updateProgress(bytesRead: Long, contentLength: Long) {
        progress = if (contentLength > 0) {
            (100 * bytesRead / contentLength).toInt()
        } else {
            -1
        }
    }
}

enum class ChapterTransitionType {
    Prev,
    Next
}

sealed class ChapterTransition {
    abstract val to: Int?

    data class Prev(override val to: Int? = null) : ChapterTransition()
    data class Next(override val to: Int? = null) : ChapterTransition()
}
