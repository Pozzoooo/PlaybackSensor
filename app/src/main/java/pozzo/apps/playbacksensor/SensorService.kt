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

/**
 * @author galien
 * @since 08/10/17.
 */
class SensorService : Service(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mProximity: Sensor
    private lateinit var eventHandler: EventHandler
    private var countIgnoreRequest = 1

    override fun onBind(intent: Intent?): IBinder = null!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val shouldStop = intent?.getBooleanExtra("stop", false) ?: false

        if(!shouldStop) {
            setupEventHandler()
            registerSensor()
            startForegroundService()
        } else {
            stopForegroundService()
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
        println("accuracy change: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent) {
        eventHandler.lastValue = event.values[1]
        val size = event.values.size
        val accuracy = event.accuracy
        println("size: $size distance: ${event.values[0]} ${event.values[1]} ${event.values[2]} " +
                "accuracy: $accuracy storedValue ${eventHandler.storedValue} countIgnoreRequest $countIgnoreRequest")

        countIgnoreRequest = --countIgnoreRequest
        if (countIgnoreRequest >= 0) {
            return
        }
        countIgnoreRequest = 1

        eventHandler.sendMessageDelayed(eventHandler.obtainMessage(), 500)
        eventHandler.storedValue = event.values[1]
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
    }
}
