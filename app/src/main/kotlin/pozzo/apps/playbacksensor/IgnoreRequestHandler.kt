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
    var countIgnoreRequest = 2
        private set

    private val RESET_DELAY = 15000L

    fun shouldProcessEvent(): Boolean {
        val message = obtainMessage()
        message.what = 199
        removeMessages(message.what)
        sendMessageDelayed(message, RESET_DELAY)
        countIgnoreRequest = --countIgnoreRequest
        if (countIgnoreRequest >= 0) {
            return false
        }
        countIgnoreRequest = 1
        return true
    }

    override fun handleMessage(msg: Message) {
        countIgnoreRequest = 0
        Log.d("Count has been reset")
    }
}
