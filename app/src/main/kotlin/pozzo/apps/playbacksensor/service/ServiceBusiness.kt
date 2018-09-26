package pozzo.apps.playbacksensor.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import pozzo.apps.playbacksensor.R
import pozzo.apps.playbacksensor.settings.Settings

class ServiceBusiness(private val context: Context) {
    companion object {
        private const val PARAM_STOP = "stop"
    }

    fun serviceStateChanged() {
        if (isEnabled()) {
            startService()
        } else {
            stopService()
        }
    }

    private fun isEnabled(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getBoolean(Settings.ENABLED, false)
    }

    private fun startService() {
        context.startService(Intent(context, SensorService::class.java))
    }

    private fun stopService() {
        context.startService(getStopIntent())
    }

    private fun getStopIntent() : Intent {
        val intent = Intent(context, SensorService::class.java)
        intent.putExtra(PARAM_STOP, true)
        return intent
    }

    fun getStopAction(): Notification.Action {
        val intentStopSensor = getStopIntent()
        val pendingIntentStopSensor = PendingIntent.getService(context, 0, intentStopSensor, 0)
        return Notification.Action
                .Builder(R.drawable.ic_icon, context.getString(R.string.stop), pendingIntentStopSensor)
                .build()
    }

    fun isStopSignal(intent: Intent?): Boolean = intent?.getBooleanExtra(PARAM_STOP, false) ?: false
}
