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
    private var lastValue = -1F
    private var countIgnoreRequest = 1

    override fun onBind(intent: Intent?): IBinder = null!!

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
        startForegroundService()
        return super.onStartCommand(intent, flags, startId)
    }

    fun startForegroundService() {
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
        val distance = event.values[0]
        val size = event.values.size
        val accuracy = event.accuracy
        println("size: $size distance: $distance ${event.values[1]} ${event.values[2]} " +
                "accuracy: $accuracy lastValue $lastValue countIgnoreRequest $countIgnoreRequest")

        //todo fix overflow
        countIgnoreRequest = --countIgnoreRequest
        if (countIgnoreRequest >= 0) {
            return
        }
        //todo what about using timestamp?

        //todo is there a smarted way with ignore involved
        if(lastValue == -1F) {
            //todo post message instead
            Handler().postDelayed({
                println("lastValue $lastValue value: $distance")

                if (lastValue == -1F) {
                    return@postDelayed
                }

                if (lastValue != distance) {
                    sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_NEXT)
                }

                if (lastValue == distance) {
                    sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    //todo is ignore a good approach?
                    countIgnoreRequest = 1
                }
                lastValue = -1F
            }, 500)
        }
        lastValue = event.values[0]
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
