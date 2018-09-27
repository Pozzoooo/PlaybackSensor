package pozzo.apps.playbacksensor.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.preference.PreferenceManager
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import org.koin.android.ext.android.inject
import pozzo.apps.playbacksensor.EventHandler
import pozzo.apps.playbacksensor.IgnoreRequestHandler
import pozzo.apps.playbacksensor.R
import pozzo.apps.playbacksensor.settings.Settings
import pozzo.apps.tools.Log

/**
 * todo there is still something funky going on, sometimes events are not really synced, can I use
 *  events value to validate it?
 */
class SensorService : Service(), SensorEventListener {
    companion object {
        private const val LONG_EVENT_DELAY = 500L
        private const val EVENT_INDEX = 0
    }

    private var mSensorManager: SensorManager? = null

    private val eventHandler: EventHandler by inject()
    private val ignoreRequestHandler: IgnoreRequestHandler by inject()
    private val serviceBusiness: ServiceBusiness by inject()
    private val serviceLogger: ServiceLogger by inject()

    override fun onBind(intent: Intent?): IBinder = null!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (serviceBusiness.isStopSignal(intent)) {
            stopService()
        } else if(!isServiceRunning()) {
            startService()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startService() {
        registerSensor()
        ForegroundNotifier(this).startForegroundServiceNotification()
    }

    private fun registerSensor() {
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
            mSensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: warnError()
    }

    private fun isServiceRunning(): Boolean = mSensorManager != null

    private fun warnError() {
        Toast.makeText(this, R.string.error_cant_start_sensor, Toast.LENGTH_LONG).show()
    }

    private fun stopService() {
        ensureEnabledSettingIsFalse()
        stopForegroundService()
    }

    private fun ensureEnabledSettingIsFalse() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isEnabled = sharedPreferences.getBoolean(Settings.ENABLED, false)

        if (isEnabled) {
            val edition = sharedPreferences.edit()
            edition.putBoolean(Settings.ENABLED, false)
            edition.apply()
        }
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d("accuracy change: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent) {
        try {
            //todo are the values anyhow consistent between devices, so maybe I can use values instead of ignores
            eventHandler.lastValue = event.values[EVENT_INDEX]
            serviceLogger.logSensorEvent(event, eventHandler, ignoreRequestHandler)

            if (!ignoreRequestHandler.shouldProcessEvent()) {
                return
            }

            //todo what about keep holding for some more time and lock/unlock screen?
            eventHandler.sendMessageDelayed(eventHandler.obtainMessage(), LONG_EVENT_DELAY)
            eventHandler.storedValue = event.values[EVENT_INDEX]
        } catch (e: Throwable) {
            Crashlytics.logException(e)
            e.printStackTrace()
            stopService()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager?.unregisterListener(this)
    }
}
