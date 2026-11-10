package vn.kozu.player

import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow

object PlaybackEvents {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    fun report(message: String) {
        _messages.tryEmit(message)
    }
}
