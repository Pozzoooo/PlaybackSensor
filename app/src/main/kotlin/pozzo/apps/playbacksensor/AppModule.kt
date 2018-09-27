package pozzo.apps.playbacksensor

import com.google.firebase.analytics.FirebaseAnalytics
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module
import pozzo.apps.playbacksensor.service.ServiceBusiness
import pozzo.apps.playbacksensor.service.ServiceLogger

val appModule = module {
    single { ServiceBusiness(androidApplication()) }
    single { ServiceLogger(get()) }
    single { FirebaseAnalytics.getInstance(androidApplication()) }
    factory { EventHandler(androidApplication()) }
    factory { IgnoreRequestHandler(androidApplication()) }
}
