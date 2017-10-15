package pozzo.apps.playbacksensor

import android.util.Log

/**
 * todo don't I have it on any of my libraries?
 *
 * @author galien
 * @since 15/10/17.
 */
object Log {
    const val isLogging = true

    fun d(string: String) {
        if (isLogging)
            Log.d("pozzo", string)
    }
}
