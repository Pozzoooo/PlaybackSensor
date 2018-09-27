package pozzo.apps.playbacksensor.notification

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import pozzo.apps.playbacksensor.R
import pozzo.apps.playbacksensor.service.ServiceBusiness
import pozzo.apps.playbacksensor.settings.SettingsActivity

class ForegroundNotifier(private val service: Service) : KoinComponent {
    private val serviceBusiness: ServiceBusiness by inject()

    fun startForegroundServiceNotification() {
        val builder = Notification.Builder(service)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addNotificationChannel(builder)
        }

        val notification = builder
                .setContentTitle(service.getString(R.string.app_name))
                .setContentText(service.getString(R.string.notification_foreground_text))
                .setTicker(service.getString(R.string.notification_foreground_text))
                .setSmallIcon(R.drawable.ic_icon)
                .setContentIntent(getPendingIntentToOpenMainActivity())
                .setPriority(Notification.PRIORITY_LOW)
                .addAction(serviceBusiness.getStopAction())
                .build()

        service.startForeground(0x22, notification)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun addNotificationChannel(builder: Notification.Builder) {
        val channel =  NotificationChannel("0", "keepAlive", NotificationManager.IMPORTANCE_HIGH)

        val notificationManager = service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        builder.setChannelId("0")
    }

    private fun getPendingIntentToOpenMainActivity(): PendingIntent {
        val intentSettingsActivity = Intent(service, SettingsActivity::class.java)
        return PendingIntent.getActivity(service, 0, intentSettingsActivity, 0)
    }
}
