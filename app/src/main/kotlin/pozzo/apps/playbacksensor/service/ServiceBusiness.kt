package pozzo.apps.playbacksensor.service

import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import pozzo.apps.playbacksensor.Settings

class ServiceBusiness(private val context: Context) {

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
        context.startService(SensorService.getStopIntent(context))
    }
}
