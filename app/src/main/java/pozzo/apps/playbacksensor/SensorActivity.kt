package pozzo.apps.playbacksensor

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.FrameLayout
import android.content.Intent
import android.view.KeyEvent

/**
 * @author galien
 * @since 08/10/17.
 */
class SensorActivity : Activity(), SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private var mProximity: Sensor? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this))

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mProximity = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        println("accuracy change: $accuracy")
    }

    override fun onSensorChanged(event: SensorEvent) {
        val distance = event.values[0]
        val size = event.values.size
        val accuracy = event.accuracy
        println("size: $size distance: $distance accuracy: $accuracy")
        sendMediaButton(getApplicationContext(), KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager!!.unregisterListener(this)
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
