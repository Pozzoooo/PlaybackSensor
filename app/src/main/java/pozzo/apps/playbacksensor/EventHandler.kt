package pozzo.apps.playbacksensor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.view.KeyEvent

/**
 * @author galien
 * @since 15/10/17.
 */
class EventHandler(private val context: Context): Handler(context.mainLooper) {
    var storedValue = -1F
    var lastValue = -1F

    override fun handleMessage(msg: Message?) {
        println("2- storedValue $storedValue value: $lastValue")

        if (storedValue == -1F) {
            return
        }

        if (storedValue != lastValue) {
            sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        }

        if (storedValue == lastValue) {
            sendMediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
        storedValue = -1F
    }

    private fun sendMediaButton(context: Context, keyCode: Int) {
        var keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        var intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        context.sendOrderedBroadcast(intent, null)

        keyEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        intent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        context.sendOrderedBroadcast(intent, null)
    }
}
