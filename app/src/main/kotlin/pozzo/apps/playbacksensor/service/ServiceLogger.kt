package pozzo.apps.playbacksensor.service

import android.hardware.SensorEvent
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import pozzo.apps.playbacksensor.EventHandler
import pozzo.apps.playbacksensor.IgnoreRequestHandler
import pozzo.apps.tools.Log

class ServiceLogger(private val firebaseAnalytics: FirebaseAnalytics) {

    fun logSensorEvent(event: SensorEvent, eventHandler: EventHandler, ignoreRequestHandler: IgnoreRequestHandler) {
        val bundle = Bundle()
        bundle.putString("size", event.values.size.toString())
        var values = ""
        for ((i, value) in event.values.withIndex()) {
            bundle.putString("value $i", value.toString())
            values += "$i,"
        }

        bundle.putString("accuracy", event.accuracy.toString())
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "event")
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)

        Log.d("size: ${event.values.size} " +
                "distance: $values " +
                "accuracy: ${event.accuracy} " +
                "storedValue ${eventHandler.storedValue} " +
                "countIgnoreRequest ${ignoreRequestHandler.countIgnoreRequest}")
    }
}
