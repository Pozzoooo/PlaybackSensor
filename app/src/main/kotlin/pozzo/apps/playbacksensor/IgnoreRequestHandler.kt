package pozzo.apps.playbacksensor

import android.content.Context
import android.os.Handler
import android.os.Message
import pozzo.apps.tools.Log

/**
 * @author galien
 * @since 16/10/17.
 */
class IgnoreRequestHandler(context: Context): Handler(context.mainLooper) {
    companion object {
        private const val MESSAGE_ID = 199
        private const val RESET_DELAY = 15000L
    }

    var countIgnoreRequest = 2
        private set


    fun shouldProcessEvent(): Boolean {
        countRequest()
        postDelayedReset()

        return if(isCountNegative()) {
            ignoreNext()
            true
        } else {
            false
        }
    }

    override fun handleMessage(msg: Message) {
        resetCount()
    }

    private fun postDelayedReset() {
        val message = obtainMessage()
        message.what = MESSAGE_ID
        removeMessages(message.what)
        sendMessageDelayed(message, RESET_DELAY)
    }

    private fun isCountNegative(): Boolean = countIgnoreRequest < 0

    private fun countRequest() {
        countIgnoreRequest = --countIgnoreRequest
    }

    private fun resetCount() {
        countIgnoreRequest = 0
        Log.d("resetCount")
    }

    private fun ignoreNext() {
        countIgnoreRequest = 1
        Log.d("ignoreNext")
    }
}
