package pozzo.apps.playbacksensor

import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module.module
import pozzo.apps.playbacksensor.service.ServiceBusiness

val appModule = module {
    single { ServiceBusiness(androidApplication()) }
}
