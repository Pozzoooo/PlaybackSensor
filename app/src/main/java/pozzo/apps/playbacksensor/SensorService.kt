package pozzo.apps.playbacksensor

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.IBinder
import android.view.KeyEvent
import android.app.PendingIntent

/**
 * @author galien
 * @since 08/10/17.
 */
class SensorService : Service(), SensorEventListener {

    private lateinit var mSensorManager: SensorManager
    private lateinit var mProximity: Sensor
    private var storedValue = -1F
    private var lastValue = -1F
    private var countIgnoreRequest = 1

    override fun onBind(intent: Intent?): IBinder = null!!

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val shouldStop = intent.getBooleanExtra("stop", false)

        if(!shouldStop) {
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

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        println("accuracy change: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent) {
        lastValue = event.values[1]
        val size = event.values.size
        val accuracy = event.accuracy
        println("size: $size distance: ${event.values[0]} ${event.values[1]} ${event.values[2]} " +
                "accuracy: $accuracy storedValue $storedValue countIgnoreRequest $countIgnoreRequest")

        //todo fix overflow
        countIgnoreRequest = --countIgnoreRequest
        if (countIgnoreRequest >= 0) {
            return
        }
        countIgnoreRequest = 1
        //todo what about using timestamp?

        //todo post message instead
        Handler().postDelayed({
            println("2- storedValue $storedValue value: $lastValue")
            //todo is ignore a good approach?

            if (storedValue == -1F) {
                return@postDelayed
            }

            if (storedValue != lastValue) {
                sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_NEXT)
            }

            if (storedValue == lastValue) {
                sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
            storedValue = -1F
        }, 500)
        storedValue = event.values[1]
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
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
