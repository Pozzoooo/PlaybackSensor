package pozzo.apps.playbacksensor

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.preference.PreferenceManager
import com.google.firebase.crash.FirebaseCrash
import pozzo.apps.tools.Log

/**
 * @author galien
 * @since 08/10/17.
 *
 * todo proper icon
 * todo there is still something funky going on, sometimes events are not really synced, can I use
 *  events value to validate it?
 */
class SensorService : Service(), SensorEventListener {
    companion object {
        private const val PARAM_STOP = "stop"

        fun getStopIntent(context: Context): Intent {
            val intent = Intent(context, SensorService::class.java)
            intent.putExtra(PARAM_STOP, true)
            return intent
        }
    }

    private var mSensorManager: SensorManager? = null
    private lateinit var mProximity: Sensor
    private lateinit var eventHandler: EventHandler
    private lateinit var ignoreRequestHanlder: IgnoreRequestHandler

    override fun onBind(intent: Intent?): IBinder = null!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val shouldStop = intent?.getBooleanExtra(PARAM_STOP, false) ?: false

        if (shouldStop) {
            stopService()
        } else if(!isServiceRunning()) {
            startService()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startService() {
        setupHandlers()
        registerSensor()
        startForegroundServiceNotification()
    }

    private fun stopService() {
        assertEnabledSettingIsFalse()
        stopForegroundService()
    }

    private fun isServiceRunning(): Boolean = mSensorManager == null

    private fun registerSensor() {
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        mSensorManager!!.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun assertEnabledSettingIsFalse() {
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

    private fun startForegroundServiceNotification() {
        val notification = Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_foreground_text))
                .setTicker(getString(R.string.notification_foreground_text))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(getPendingIntentToOpenMainActivity())
                .setPriority(Notification.PRIORITY_LOW)
                .addAction(getStopAction())
                .build()

        startForeground(0x22, notification)
    }

    private fun getPendingIntentToOpenMainActivity(): PendingIntent {
        val intentSettingsActivity = Intent(this, SettingsActivity::class.java)
        return PendingIntent.getActivity(this, 0, intentSettingsActivity, 0)
    }

    private fun getStopAction(): Notification.Action {
        val intentStopSensor = getStopIntent(this)
        val pendingIntentStopSensor = PendingIntent.getService(this, 0, intentStopSensor, 0)
        return Notification.Action
                .Builder(R.mipmap.ic_launcher_round, getString(R.string.stop), pendingIntentStopSensor)
                .build()
    }

    private fun setupHandlers() {
        eventHandler = EventHandler(this)
        ignoreRequestHanlder = IgnoreRequestHandler(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d("accuracy change: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent) {
        try {
            //todo are the values anyhow consistent between devices, so maybe I can use values instead of ignores
            eventHandler.lastValue = event.values[0]
            logSensorEvent(event)

            if (!ignoreRequestHanlder.shouldProcessEvent()) {
                return
            }

            //todo what about keep holding for some more time and lock/unlock screen?
            eventHandler.sendMessageDelayed(eventHandler.obtainMessage(), 500)
            eventHandler.storedValue = event.values[0]
        } catch (e: Throwable) {
            FirebaseCrash.report(e)
            stopService()
        }
    }

    private fun logSensorEvent(event: SensorEvent) {
        Log.d("size: ${event.values.size} " +
                "distance: ${event.values[0]} ${event.values[1]} ${event.values[2]} " +
                "accuracy: ${event.accuracy} " +
                "storedValue ${eventHandler.storedValue} " +
                "countIgnoreRequest ${ignoreRequestHanlder.countIgnoreRequest}")
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager?.unregisterListener(this)
    }
}
