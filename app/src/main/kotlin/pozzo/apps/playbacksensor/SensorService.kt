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
import pozzo.apps.tools.Log

/**
 * todo in doubt about creating a real architecture for this app, its kind of too simple
 *
 * @author galien
 * @since 08/10/17.
 */
class SensorService : Service(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mProximity: Sensor
    private var eventHandler: EventHandler? = null
    //todo need to understand if 2 is going to happen for any device
    private var countIgnoreRequest = 2

    override fun onBind(intent: Intent?): IBinder = null!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val shouldStop = intent?.getBooleanExtra("stop", false) ?: false

        if (shouldStop) {
            stopForegroundService()
        } else if(eventHandler == null) {
            setupEventHandler()
            registerSensor()
            startForegroundService()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun registerSensor() {
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    //todo add a toggle directly on the notification
    private fun startForegroundService() {
        val notificationIntent = Intent(this, SettingsActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val notification = Notification.Builder(this)
                .setContentTitle("Title")
                .setContentText("Not not not notification")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setTicker("Ticker")
                .setPriority(Notification.PRIORITY_LOW)
                .build()

        startForeground(0x22, notification)
    }

    private fun setupEventHandler() {
        eventHandler = EventHandler(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Log.d("accuracy change: $accuracy")
    }

    //todo it happened again, it switches the moment when he is ignoring request
    //      should I fix it using a timer? It would help on the initialization doubt as well
    override fun onSensorChanged(event: SensorEvent) {
        eventHandler?.lastValue = event.values[0]
        Log.d("size: ${event.values.size} " +
                "distance: ${event.values[0]} ${event.values[1]} ${event.values[2]} " +
                "accuracy: ${event.accuracy} " +
                "storedValue ${eventHandler?.storedValue} " +
                "countIgnoreRequest $countIgnoreRequest")

        countIgnoreRequest = --countIgnoreRequest
        if (countIgnoreRequest >= 0) {
            return
        }
        countIgnoreRequest = 1

        eventHandler?.sendMessageDelayed(eventHandler?.obtainMessage(), 500)
        eventHandler?.storedValue = event.values[0]
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
    }
}
