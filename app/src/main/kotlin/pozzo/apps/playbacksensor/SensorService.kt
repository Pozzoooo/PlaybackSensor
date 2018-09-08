package pozzo.apps.playbacksensor

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import pozzo.apps.tools.Log


/**
 * @author Luiz Gustavo Pozzo
 * @since 08/10/17.
 *
 * todo there is still something funky going on, sometimes events are not really synced, can I use
 *  events value to validate it?
 */
class SensorService : Service(), SensorEventListener {
    companion object {
        private const val PARAM_STOP = "stop"
        private const val LONG_EVENT_DELAY = 500L
        private const val EVENT_INDEX = 0

        fun getStopIntent(context: Context): Intent {
            val intent = Intent(context, SensorService::class.java)
            intent.putExtra(PARAM_STOP, true)
            return intent
        }
    }

    private var mSensorManager: SensorManager? = null
    private lateinit var eventHandler: EventHandler
    private lateinit var ignoreRequestHanlder: IgnoreRequestHandler
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onBind(intent: Intent?): IBinder = null!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isStopSignal(intent)) {
            stopService()
        } else if(!isServiceRunning()) {
            startService()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun isStopSignal(intent: Intent?): Boolean = intent?.getBooleanExtra(PARAM_STOP, false) ?: false

    private fun startService() {
        setupFirebase()
        setupHandlers()
        registerSensor()
        startForegroundServiceNotification()
    }

    private fun setupFirebase() {
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
    }

    private fun stopService() {
        ensureEnabledSettingIsFalse()
        stopForegroundService()
    }

    private fun isServiceRunning(): Boolean = mSensorManager != null

    private fun registerSensor() {
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
            mSensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        } ?: warnError()
    }

    private fun warnError() {
        Toast.makeText(this, R.string.error_cant_start_sensor, Toast.LENGTH_LONG).show()
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

    private fun startForegroundServiceNotification() {
        val builder = Notification.Builder(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addNotificationChannel(builder)
        }

        val notification = builder
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_foreground_text))
                .setTicker(getString(R.string.notification_foreground_text))
                .setSmallIcon(R.drawable.ic_icon)
                .setContentIntent(getPendingIntentToOpenMainActivity())
                .setPriority(Notification.PRIORITY_LOW)
                .addAction(getStopAction())
                .build()

        startForeground(0x22, notification)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun addNotificationChannel(builder: Notification.Builder) {
        val channel =  NotificationChannel("0", "keepAlive", NotificationManager.IMPORTANCE_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        builder.setChannelId("0")
    }

    private fun getPendingIntentToOpenMainActivity(): PendingIntent {
        val intentSettingsActivity = Intent(this, SettingsActivity::class.java)
        return PendingIntent.getActivity(this, 0, intentSettingsActivity, 0)
    }

    private fun getStopAction(): Notification.Action {
        val intentStopSensor = getStopIntent(this)
        val pendingIntentStopSensor = PendingIntent.getService(this, 0, intentStopSensor, 0)
        return Notification.Action
                .Builder(R.drawable.ic_icon, getString(R.string.stop), pendingIntentStopSensor)
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
            eventHandler.lastValue = event.values[EVENT_INDEX]
            logSensorEvent(event)

            if (!ignoreRequestHanlder.shouldProcessEvent()) {
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

    private fun logSensorEvent(event: SensorEvent) {
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
                "countIgnoreRequest ${ignoreRequestHanlder.countIgnoreRequest}")
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager?.unregisterListener(this)
    }
}
