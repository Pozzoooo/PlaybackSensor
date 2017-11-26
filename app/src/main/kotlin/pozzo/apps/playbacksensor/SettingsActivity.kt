package pozzo.apps.playbacksensor

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import pozzo.apps.tools.Log

/**
 * todo pause button in the notification
 * todo option to enable it only when the screen is off
 * todo also control it by other sensors (like punching the phone)
 * todo why is it not working on Google Play music in Oreo?
 *
 * @author Luiz Gustavo Pozzo
 * @since 15/10/17.
 */
class SettingsActivity : PreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupLogs()
        initialiseBaseServiceStateBasedOnSettings()
        registerItlsefForPreferenceChanges()
        setupLayout()
    }

    override fun onStop() {
        super.onStop()
        finish()//Needed to make sure preferences stays up to date
    }

    private fun setupLogs() {
        Log.SHOW_LOGS = BuildConfig.DEBUG
    }

    private fun initialiseBaseServiceStateBasedOnSettings() {
        if (isEnabled()) {
            startService()
        }
    }

    private fun registerItlsefForPreferenceChanges() {
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this)
    }

    private fun setupLayout() {
        fragmentManager.beginTransaction().replace(android.R.id.content,
                GeneralPreferenceFragment()).commit()
    }

    override fun onIsMultiPane(): Boolean = isXLargeTablet(this)

    override fun isValidFragment(fragmentName: String): Boolean =
            PreferenceFragment::class.java.name == fragmentName || GeneralPreferenceFragment::class.java.name == fragmentName

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (Settings.ENABLED != key) {
            return
        }

        if (isEnabled()) {
            startService()
        } else {
            stopService()
        }
    }

    private fun startService() {
        startService(Intent(this, SensorService::class.java))
    }

    private fun stopService() {
        startService(SensorService.getStopIntent(this))
    }

    private fun isEnabled(): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getBoolean(Settings.ENABLED, false)
    }

    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
        }
    }

    companion object {
        private fun isXLargeTablet(context: Context): Boolean =
                context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
    }
}
